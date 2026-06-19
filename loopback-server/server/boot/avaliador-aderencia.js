'use strict';

var http = require('http');
var https = require('https');
var loopback = require('loopback');
var url = require('url');

var DEFAULT_AVALIADOR_URL = 'http://avaliador-aderencia:8082';

module.exports = function(app) {
  var baseUrl = process.env.AVALIADOR_ADERENCIA_URL || DEFAULT_AVALIADOR_URL;

  garantirColunasAderencia(app);

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

  console.log('[avaliador-aderencia] Salvando resultado recebido.', {
    oportunidadeId: oportunidadeId,
    notaAderencia: resultado.notaAderencia,
    status: status,
    analiseIaTamanho: tamanhoTexto(resultado.analiseIa),
  });

  app.models.OportunidadeLinkedin.updateAll(
    {id: oportunidadeId},
    {
      notaAderencia: resultado.notaAderencia,
      analiseAderenciaIa: resultado.analiseIa,
      statusAderencia: status,
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
