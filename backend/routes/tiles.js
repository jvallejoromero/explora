const express = require("express");
const router = express.Router();
const path = require("path");
const fs = require("fs");

router.get("/available-worlds", (req, res) => {

    const tileRoot = path.join(__dirname, "..", "tiles");
    const worlds = fs.readdirSync(tileRoot).filter(f => fs.statSync(path.join(tileRoot, f)).isDirectory());

    res.json({ worlds });
});

router.get("/exists/:world/:zoom/:x/:z", async (req, res) => {
    const { world, zoom, x, z } = req.params;
    const zoomNum = parseInt(zoom);
    const xNum = parseInt(x);
    const zNum = parseInt(z);

    if (Number.isNaN(zoomNum) || Number.isNaN(xNum) || Number.isNaN(zNum)) {
        return res.status(400).send("Invalid zoom or coordinates");
    }

    const filename = `r.${x}.${z}`;
    const zoomFolder = zoomNum > 0 ? `zoom${zoomNum}` : "";
    const imagePath = path.join(__dirname, "..", "tiles", world, zoomFolder, filename);

    if (fs.existsSync(imagePath)) {
        return res.status(200).json({ exists: true });
    } else {
        return res.status(404).json({ exists: false });
    }
});

router.get("/:world/:zoom/:x/:z.:extension", async (req, res) => {
    const { world, zoom, x, z, extension } = req.params;

    console.log(`received request for tile: world=${world}, x=${x}, z=${z}, zoom=${zoom}, ext=${extension}`);

    if (extension !== "png" && extension !== "json") {
        return res.status(400).send("Unsupported file type");
    }

    const zoomFolder = parseInt(zoom) > 0 ? `zoom${zoom}` : "";
    const filename = `r.${x}.${z}.${extension}`;
    const imagePath = path.join(__dirname, "..", "tiles", world, zoomFolder, filename);

    let filePathToSend = imagePath;

    if (!fs.existsSync(imagePath)) {
        const fallbackPath = path.join(__dirname, "..", "fallback", `empty.${extension}`);
        if (!fs.existsSync(fallbackPath)) {
            return res.status(404).send("Requested file not found");
        }
        filePathToSend = fallbackPath;
        console.log(`tile not found, sending fallback for: x=${x}, z=${z}`);
    }

    try {
        const stats = fs.statSync(filePathToSend);
        const eTag = `"${stats.mtimeMs}"`;

        res.setHeader("ETag", eTag);
        res.setHeader("Cache-Control", "public, max-age=0, must-revalidate");

        if (req.headers["if-none-match"] === eTag) {
            console.log(`304 Not Modified: ${filePathToSend}`);
            return res.status(304).end();
        }

        res.sendFile(filePathToSend);
        console.log(`200 OK: sent ${filePathToSend}`);
    } catch (err) {
        console.error("Failed to read file:", err);
        return res.status(500).send("Internal server error");
    }
});


module.exports = router;
