'use strict';

var http = require('http');
var https = require('https');
var url = require('url');

var DEFAULT_AVALIADOR_URL = 'http://avaliador-aderencia:8082';

module.exports = function(app) {
  var baseUrl = process.env.AVALIADOR_ADERENCIA_URL || DEFAULT_AVALIADOR_URL;

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

  app.post('/api/avaliador-aderencia/resultados', function(req, res) {
    app.emit('avaliador-aderencia:resultado', req.body);
    res.status(202).json({recebido: true});
  });
};

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
