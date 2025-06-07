const express = require("express");
const router = express.Router();
const store = require("../stores/chunk-store-sqlite");

router.post('/update/batch', async (req, res) => {
    const {world, chunks} = req.body;

    if (!world || !Array.isArray(chunks)) {
        return res.status(400).json("Missing world or chunks array.");
    }

    try {
        await store.addChunksBatch(world, chunks);
        res.sendStatus(200);
    } catch (err) {
        console.error("Error in batch update: ", err);
        res.sendStatus(500);
    }
});

router.post('/update', async (req, res) => {
    const {world, x, z} = req.body;
    if (!world || x === undefined || z === undefined) return res.sendStatus(400);

    try {
        await store.addChunk(world, x, z);
        res.sendStatus(200);
    } catch (err) {
        console.error("Error adding chunk: ", err);
        res.sendStatus(500);
    }
});

router.get('/has-chunk', async(req, res) => {
    const world = req.query.world;
    const x = req.query.x;
    const z = req.query.z;

    if (!world || x === undefined || z === undefined) return res.sendStatus(400);

    try {
        const exists = await store.hasChunk(world, Number(x), Number(z));
        if (exists) {
            res.json({exists, world, x, z});
        } else {
            res.json({exists});
        }
    } catch (err) {
        console.error("Error verifying chunk existence: ", err);
        res.sendStatus(500);
    }
});

/**
 * @typedef {Object} ChunkRequestBody
 * @property {string} world
 * @property {number} x
 * @property {number} z
 */
router.get('/:world', async (req, res) => {
    try {
        const chunks = await store.getChunks(req.params.world);
        res.json(chunks);
    } catch (err) {
        console.error("Error fetching chunks: ", err);
        res.sendStatus(500);
    }
});

module.exports = router;