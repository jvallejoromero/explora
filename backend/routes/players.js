const express = require("express");
const router = express.Router();

// key = playerName, value = { world, x, z, lastSeen }
const onlinePlayers = new Map();

router.post('/update', (req, res) => {
    const {name, world, x, y, z} = req.body;

    if (!name || !world || x === undefined || y === undefined || z === undefined) {
        return res.status(400).send({error: 'Missing required fields'});
    }

    onlinePlayers.set(name, {
        name,
        world,
        x,
        y,
        z,
        lastSeen: Date.now(),
    });

    res.sendStatus(200);
});

router.get('/', (req, res) => {
    const now = Date.now();
    const ACTIVE_TIMEOUT_MS = 10_000; // remove inactive players after 10 seconds

    const activePlayers = Array.from(onlinePlayers.values().filter(
        p => (now - p.lastSeen) <= ACTIVE_TIMEOUT_MS
    ));

    const playersForClient = activePlayers.map(p => ({
        name: p.name,
        world: p.world,
        x: p.x,
        y: p.y,
        z: p.z,
    }));

    res.json({"online-players": playersForClient});
});

module.exports = router;