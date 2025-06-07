const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const chunkRoutes = require('./routes/chunks');
const playerRoutes = require('./routes/players');
const adminRoutes = require('./routes/admin');
const debugRoutes = require('./routes/debug');

const app = express();

app.use(cors());
app.use(bodyParser.json());
app.use('/chunks', chunkRoutes);
app.use('/players', playerRoutes);
app.use('/admin', adminRoutes);
app.use('/debug', debugRoutes);

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`✅ Explora backend running on port ${PORT}`);
})