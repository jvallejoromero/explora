const express = require("express");
const router = express.Router();
const store = require("../stores/chunk-store-sqlite");

/**
 * @typedef {Object} ClearChunksRequestBody
 * @property {string} secret
 */
router.delete("/clear-chunks", async (req, res) => {
    try {
        const deleted = await store.clearChunks();
        res.status(200).json({ deleted });
    } catch (err) {
        console.error("Error clearing chunk database:", err);
        res.sendStatus(500);
    }
});

module.exports = router;