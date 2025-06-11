const express = require("express");
const websocket = require("../websocket");
const serverStatusStore = require("../stores/server-status-store");
const router = express.Router();

const OFFLINE_TIMEOUT_MS = 3000;

router.post('/server/update', (req, res) => {
    const { isOnline, playerCount, worldTime, motd } = req.body;
    if (typeof isOnline !== "boolean" || typeof playerCount !== "number"
        || typeof worldTime !== "number" || typeof motd !== "string") {
        return res.status(400).json({ message: "Invalid parameters" });
    }

    serverStatusStore.updateStatus({isOnline, playerCount, worldTime, motd, lastUpdateSent: new Date()});
    websocket.getIO().emit('serverStatusUpdate', serverStatusStore.getStatus());

    res.json({message: "Server status updated"});
});

router.get('/server', (req, res) => {
    res.json(serverStatusStore.getStatus());
});

setInterval(() => {
    if (serverStatusStore.checkStaleness()) {
        console.log("⚠️ Spigot Server appears offline — emitting fallback status");
        websocket.getIO().emit('serverStatusUpdate', serverStatusStore.getStatus());
    }
}, 1000);

module.exports = router;
