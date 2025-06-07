const express = require("express");
const router = express.Router();
const store = require("../stores/chunk-store-sqlite");

/**
 * @typedef {Object} ClearChunksRequestBody
 * @property {string} secret
 */
router.delete("/clear-chunks", async (req, res) => {
    const secret = req.query.secret;

    // this is just for unwanted deletions, will change to more secure option in the future
    if (!secret || secret !== "thisisaneasysecret") {
        return res.sendStatus(403);
    }

    try {
        const deleted = await store.clearChunks();
        res.status(200).json({ deleted });
    } catch (err) {
        console.error("Error clearing chunk database:", err);
        res.sendStatus(500);
    }
});

module.exports = router;