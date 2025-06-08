const config = require('../config.json');

module.exports = function (req, res, next) {
    const keyFromHeader = req.headers['x-api-key'];
    const keyFromQuery = req.query.apiKey;
    const key = keyFromHeader || keyFromQuery;

    if (!key || key !== config.apiKey) {
        return res.status(401).json({error: 'Unauthorized: Invalid API key'});
    }
    next();
};