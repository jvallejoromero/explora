const express = require("express");
const router = express.Router();
const websocket = require("../websocket");

// key = playerName, value = { world, x, z, lastSeen }
const onlinePlayers = new Map();

router.post('/update', (req, res) => {
    const {"online-players": players} = req.body;

    if (!Array.isArray(players)) {
        res.status(400).json({ message: "Missing or invalid online-players array" });
    }

    const now = Date.now();

    for (const player of players) {
        const { name, world, x, y, z, yaw } = player;
        if (!name || !world || x === undefined || y === undefined || z === undefined || yaw === undefined) {
            continue;
        }
        onlinePlayers.set(name, {
            name,
            world,
            x,
            y,
            z,
            yaw,
            lastSeen: now,
        });
    }

    websocket.getIO().emit("onlinePlayersUpdate", {"online-players": players});
    res.sendStatus(200);
});

router.get('/', (req, res) => {
    const now = Date.now();
    const ACTIVE_TIMEOUT_MS = 10_000; // remove inactive players after 10 seconds

    const worldFilter = req.query.world;

    const activePlayers = Array.from(onlinePlayers.values()).filter(
        (p) => {
            const isActive = (now - p.lastSeen) <= ACTIVE_TIMEOUT_MS;
            if (worldFilter != null) {
                return isActive && p.world === worldFilter;
            }
            return isActive;
        }
    );

    const playersForClient = activePlayers.map(p => ({
        name: p.name,
        world: p.world,
        x: p.x,
        y: p.y,
        z: p.z,
        yaw: p.yaw,
    }));

    res.json({"online-players": playersForClient});
});

module.exports = router;