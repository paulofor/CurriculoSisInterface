'use strict';

var fs = require('fs');
var path = require('path');

var DEFAULT_LOG_FILE = '/var/log/curriculosis/backend.log';

function maskSensitiveValues(value) {
  return String(value)
    .replace(/(authorization:\s*bearer\s+)[^\s,;]+/gi, '$1[REDACTED]')
    .replace(/(password=)[^\s&]+/gi, '$1[REDACTED]')
    .replace(/(token=)[^\s&]+/gi, '$1[REDACTED]')
    .replace(/(api[_-]?key=)[^\s&]+/gi, '$1[REDACTED]');
}

function appendLog(logFile, line) {
  fs.mkdirSync(path.dirname(logFile), {recursive: true});
  fs.appendFile(logFile, line + '\n', function(err) {
    if (err) {
      console.error('Failed to write backend access log: %s', err.message);
    }
  });
}

module.exports = function(server) {
  var logFile = process.env.BACKEND_LOG_FILE || DEFAULT_LOG_FILE;

  server.middleware('initial:before', function backendRequestLogger(req, res, next) {
    var startedAt = Date.now();

    res.on('finish', function() {
      var durationMs = Date.now() - startedAt;
      var forwardedFor = req.headers['x-forwarded-for'];
      var remoteAddress = forwardedFor || req.connection.remoteAddress || '-';
      var userAgent = req.headers['user-agent'] || '-';
      var requestUrl = maskSensitiveValues(req.originalUrl || req.url || '-');
      var line = [
        new Date().toISOString(),
        remoteAddress,
        req.method,
        requestUrl,
        res.statusCode,
        durationMs + 'ms',
        'ua="' + maskSensitiveValues(userAgent) + '"'
      ].join(' ');

      appendLog(logFile, line);
    });

    next();
  });
};
