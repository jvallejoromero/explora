const express = require("express");
const router = express.Router();
const store = require("../stores/chunk-store-sqlite");

router.get('/chunk-count/:world', async (req, res) => {
    const world = req.params.world;
    try {
        const count = await store.getChunkCountForWorld(world);
        res.json({ world, totalChunks: count });
    } catch (err) {
        console.error("Error fetching chunk count for world:", err);
        res.sendStatus(500);
    }
});

module.exports = router;