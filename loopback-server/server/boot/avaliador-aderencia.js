'use strict';

var http = require('http');
var https = require('https');
var loopback = require('loopback');
var url = require('url');

var DEFAULT_AVALIADOR_URL = 'http://avaliador-aderencia:8082';

module.exports = function(app) {
  var baseUrl = process.env.AVALIADOR_ADERENCIA_URL || DEFAULT_AVALIADOR_URL;

  garantirColunasAderencia(app);
  descartarAvaliadasPresenciaisHibridas(app);

  app.get('/api/avaliador-aderencia/health', function(req, res) {
    proxyJson(baseUrl, '/api/avaliador-aderencia/health', 'GET', null, res);
  });

  app.get('/api/avaliador-aderencia/status', function(req, res) {
    proxyJson(baseUrl, '/api/avaliador-aderencia/status', 'GET', null, res);
  });

  app.post('/api/avaliador-aderencia/executar', function(req, res) {
    proxyJson(
      baseUrl,
      '/api/avaliador-aderencia/executar',
      'POST',
      req.body || {},
      res
    );
  });

  app.post(
    '/api/avaliador-aderencia/resultados',
    loopback.json({limit: '100kb'}),
    function(req, res) {
      registrarRecebimentoResultado(req);

      salvarResultadoAderencia(app, req.body, function(err) {
        if (err) {
          console.error(
            '[avaliador-aderencia] Resultado rejeitado pelo backend.',
            {
              erro: err.message,
              body: resumirPayload(req.body),
            }
          );
          res.status(500).json({recebido: false, erro: err.message});
          return;
        }
        app.emit('avaliador-aderencia:resultado', req.body);
        res.status(202).json({recebido: true});
      });
    }
  );
};

function salvarResultadoAderencia(app, body, callback) {
  var resultado = body && body.resultado ? body.resultado : body;
  var oportunidadeId = resultado && resultado.oportunidadeId;
  var status = normalizarStatus(resultado && resultado.status);

  if (!oportunidadeId) {
    callback(new Error('Resultado de aderencia sem oportunidadeId.'));
    return;
  }

  app.models.OportunidadeLinkedin.findById(oportunidadeId, function(findErr, oportunidade) {
    if (findErr) {
      callback(findErr);
      return;
    }

    var descarteModelo = isModeloPresencialOuHibrido(oportunidade, resultado);
    var notaAderencia = descarteModelo ? 0 : resultado.notaAderencia;
    var analiseIa = descarteModelo
      ? montarAnaliseDescarteModelo(resultado.analiseIa)
      : resultado.analiseIa;
    var statusFinal = descarteModelo ? 'descartada' : status;

    console.log('[avaliador-aderencia] Salvando resultado recebido.', {
      oportunidadeId: oportunidadeId,
      notaAderencia: notaAderencia,
      status: statusFinal,
      descarteModelo: descarteModelo,
      modelo: oportunidade && oportunidade.modelo,
      analiseIaTamanho: tamanhoTexto(analiseIa),
    });

    app.models.OportunidadeLinkedin.updateAll(
      {id: oportunidadeId},
      {
        notaAderencia: notaAderencia,
        analiseAderenciaIa: analiseIa,
        statusAderencia: statusFinal,
        dataAvaliacaoAderencia: new Date(),
      },
      function(err, info) {
        if (err) {
          callback(err);
          return;
        }

        console.log('[avaliador-aderencia] Resultado salvo no backend.', {
          oportunidadeId: oportunidadeId,
          linhasAfetadas: obterLinhasAfetadas(info),
        });
        callback(null, info);
      }
    );
  });
}

function isModeloPresencialOuHibrido(oportunidade, resultado) {
  var texto = [
    oportunidade && oportunidade.modelo,
    oportunidade && oportunidade.descricao,
    resultado && resultado.analiseIa,
  ].filter(Boolean).join(' ').toLowerCase();

  return texto.indexOf('presencial') >= 0 ||
    texto.indexOf('híbrido') >= 0 ||
    texto.indexOf('hibrido') >= 0 ||
    texto.indexOf('hybrid') >= 0 ||
    texto.indexOf('on-site') >= 0 ||
    texto.indexOf('onsite') >= 0;
}

function montarAnaliseDescarteModelo(analiseOriginal) {
  var motivo = 'Descartada automaticamente: oportunidade presencial ou híbrida, fora da preferência 100% remota do candidato.';

  if (!analiseOriginal) {
    return motivo;
  }

  return motivo + '\n\nAnálise original: ' + analiseOriginal;
}

function registrarRecebimentoResultado(req) {
  console.log('[avaliador-aderencia] Resultado recebido pelo backend.', {
    contentType: req.headers['content-type'],
    contentLength: req.headers['content-length'],
    userAgent: req.headers['user-agent'],
    bodyPresente: !!req.body,
    body: resumirPayload(req.body),
  });
}

function resumirPayload(body) {
  if (!body) {
    return body;
  }

  var resultado = body.resultado || body;
  return {
    chaves: Object.keys(body),
    resultadoChaves: obterChaves(resultado),
    oportunidadeId: resultado && resultado.oportunidadeId,
    notaAderencia: resultado && resultado.notaAderencia,
    status: resultado && resultado.status,
    analiseIaTamanho: resultado ? tamanhoTexto(resultado.analiseIa) : 0,
    preview: limitarTexto(body),
  };
}

function limitarTexto(valor) {
  var texto = JSON.stringify(valor);

  if (!texto) {
    return texto;
  }

  if (texto.length > 500) {
    return texto.substring(0, 500) + '...[truncado]';
  }

  return texto;
}

function obterChaves(valor) {
  return valor && typeof valor === 'object' ? Object.keys(valor) : [];
}

function tamanhoTexto(valor) {
  return valor ? String(valor).length : 0;
}

function normalizarStatus(status) {
  return status ? String(status).toLowerCase() : 'avaliada';
}

function obterLinhasAfetadas(info) {
  if (!info) {
    return null;
  }

  return info.count || info.affectedRows || null;
}

function garantirColunasAderencia(app) {
  var dataSource = app.models.OportunidadeLinkedin.dataSource;
  var connector = dataSource && dataSource.connector;

  if (!connector || typeof connector.query !== 'function') {
    return;
  }

  var colunas = [
    {nome: 'notaAderencia', definicao: 'INT NULL'},
    {nome: 'analiseAderenciaIa', definicao: 'TEXT NULL'},
    {nome: 'statusAderencia', definicao: 'VARCHAR(40) NULL'},
    {nome: 'dataAvaliacaoAderencia', definicao: 'DATETIME NULL'},
  ];

  connector.query(
    'SHOW COLUMNS FROM OportunidadeLinkedin',
    function(err, existentes) {
      if (err || !existentes) {
        console.error('Não foi possível verificar colunas de aderência.', err);
        return;
      }

      var nomesExistentes = existentes.map(function(coluna) {
        return coluna.Field;
      });

      colunas
        .filter(function(coluna) {
          return nomesExistentes.indexOf(coluna.nome) < 0;
        })
        .forEach(function(coluna) {
          var sql = 'ALTER TABLE OportunidadeLinkedin ADD COLUMN ' +
            coluna.nome + ' ' + coluna.definicao;
          connector.query(sql, function(alterErr) {
            if (alterErr) {
              console.error(
                'Não foi possível criar coluna ' + coluna.nome +
                ' em OportunidadeLinkedin.',
                alterErr
              );
            }
          });
        });
    }
  );
}

function descartarAvaliadasPresenciaisHibridas(app) {
  var dataSource = app.models.OportunidadeLinkedin.dataSource;
  var connector = dataSource && dataSource.connector;

  if (!connector || typeof connector.query !== 'function') {
    return;
  }

  var motivo = 'Descartada automaticamente: oportunidade presencial ou híbrida, fora da preferência 100% remota do candidato.';
  var sql = [
    'UPDATE OportunidadeLinkedin',
    'SET notaAderencia = 0,',
    "statusAderencia = 'descartada',",
    'analiseAderenciaIa = CASE',
    '  WHEN analiseAderenciaIa IS NULL OR analiseAderenciaIa = \'\' THEN ?',
    '  WHEN analiseAderenciaIa LIKE ? THEN analiseAderenciaIa',
    "  ELSE CONCAT(?, '\\n\\nAnálise original: ', analiseAderenciaIa)",
    'END,',
    'dataAvaliacaoAderencia = NOW()',
    'WHERE statusAderencia = \'avaliada\'',
    'AND COALESCE(notaAderencia, 0) >= 70',
    'AND (',
    "  LOWER(COALESCE(modelo, '')) LIKE '%presencial%' OR",
    "  LOWER(COALESCE(modelo, '')) LIKE '%híbrido%' OR",
    "  LOWER(COALESCE(modelo, '')) LIKE '%hibrido%' OR",
    "  LOWER(COALESCE(modelo, '')) LIKE '%hybrid%' OR",
    "  LOWER(COALESCE(modelo, '')) LIKE '%on-site%' OR",
    "  LOWER(COALESCE(modelo, '')) LIKE '%onsite%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%presencial%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%híbrido%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%hibrido%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%hybrid%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%on-site%' OR",
    "  LOWER(COALESCE(descricao, '')) LIKE '%onsite%'",
    ')',
  ].join(' ');

  connector.query(sql, [motivo, motivo + '%', motivo], function(err, info) {
    if (err) {
      console.error('Não foi possível descartar oportunidades presenciais/híbridas já avaliadas.', err);
      return;
    }

    console.log('[avaliador-aderencia] Oportunidades presenciais/híbridas já avaliadas descartadas.', {
      linhasAfetadas: obterLinhasAfetadas(info),
    });
  });
}

function proxyJson(baseUrl, path, method, body, res) {
  var parsed = url.parse(baseUrl + path);
  var payload = body ? JSON.stringify(body) : null;
  var client = parsed.protocol === 'https:' ? https : http;
  var options = {
    hostname: parsed.hostname,
    port: parsed.port,
    path: parsed.path,
    method: method,
    headers: {
      'Accept': 'application/json',
    },
  };

  if (payload) {
    options.headers['Content-Type'] = 'application/json';
    options.headers['Content-Length'] = Buffer.byteLength(payload);
  }

  var request = client.request(options, function(response) {
    var chunks = [];
    response.on('data', function(chunk) {
      chunks.push(chunk);
    });
    response.on('end', function() {
      var responseBody = Buffer.concat(chunks).toString('utf8');
      res.status(response.statusCode || 502);
      copyContentType(response, res);
      res.send(responseBody);
    });
  });

  request.on('error', function(err) {
    res.status(502).json({
      erro: 'avaliador-aderencia indisponivel',
      detalhe: err.message,
    });
  });

  if (payload) {
    request.write(payload);
  }
  request.end();
}

function copyContentType(response, res) {
  if (response.headers && response.headers['content-type']) {
    res.set('Content-Type', response.headers['content-type']);
  }
}
