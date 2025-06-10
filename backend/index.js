const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const chunkRoutes = require('./routes/chunks');
const playerRoutes = require('./routes/players');
const adminRoutes = require('./routes/admin');
const debugRoutes = require('./routes/debug');
const uploadRoutes = require('./routes/upload');
const tilesRoutes = require('./routes/tiles');

const apiKeyMiddleware = require('./middleware/api-key');
const config = require('./config');
const app = express();

app.use(cors());
app.use(bodyParser.json());

app.use('/api', apiKeyMiddleware);
app.use('/api/chunks', chunkRoutes);
app.use('/api/players', playerRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/debug', debugRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/tiles', tilesRoutes);

const PORT = config.port;

app.listen(PORT, () => {
    console.log(`✅ Explora backend running on port ${PORT}`);
})