const express = require("express");
const router = express.Router();

const multer = require("multer");
const upload = multer({ storage: multer.memoryStorage() });
const unzipper = require("unzipper");

const path = require("path");
const fs = require("fs");
const websocket = require("../websocket");

const TILES_DIR = path.join(__dirname, "../tiles");

function deleteFolderRecursive(folderPath) {
    if (fs.existsSync(folderPath)) {
        fs.rmSync(folderPath, { recursive: true, force: true });
    }
}

/**
 * @typedef {Object} UploadZipRequestBody
 * @property {file} file - The uploaded zip file
 * @property {boolean} deleteExisting - Whether to delete existing tiles before uploading
 */
router.post("/tile-zip", upload.single("file"), async (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded" });
    }

    const shouldDeleteAll = req.query.deleteExisting === "true";
    const zipBuffer = req.file.buffer;

    try {
        if (shouldDeleteAll) {
            deleteFolderRecursive(TILES_DIR);
        }

        fs.mkdirSync(TILES_DIR, { recursive: true });

        const directory = await unzipper.Open.buffer(zipBuffer);
        const updatedTiles = new Map();

        for (const file of directory.files) {
            const relativePath = file.path;
            const fullPath = path.join(TILES_DIR, relativePath);

            // Prevent zip slip
            if (!fullPath.startsWith(TILES_DIR)) {
                console.warn(`Blocked unsafe path: ${fullPath}`);
                continue;
            }

            const dir = path.dirname(fullPath);
            fs.mkdirSync(dir, { recursive: true });

            if (file.type === "File") {
                const contentStream = file.stream();
                const writeStream = fs.createWriteStream(fullPath);
                await new Promise((resolve, reject) => {
                    contentStream.pipe(writeStream)
                        .on("finish", resolve)
                        .on("error", reject);
                });

                if (!shouldDeleteAll) {
                    const parts = relativePath.split('/');
                    if (parts.length >= 2) {
                        const world = parts[0];
                        const filename = parts[1];
                        const match = filename.match(/^r\.(-?\d+)\.(-?\d+)\.png$/);

                        if (match) {
                            const x = parseInt(match[1]);
                            const z = parseInt(match[2]);
                            const key = `${x},${z}`;
                            if (!updatedTiles.has(world)) {
                                updatedTiles.set(world, new Set());
                            }
                            updatedTiles.get(world).add(key);
                        }
                    }
                }
            }
        }

        for (const [world, coordsSet] of updatedTiles.entries()) {
            const coords = [...coordsSet].map(key => {
                const [x, z] = key.split(",").map(Number);
                return { x, z };
            });

            websocket.getIO().emit("tileUpdate", {
                world,
                tiles: coords,
            });
        }

        return res.status(200).json({
            status: "success",
            updated: Object.fromEntries([...updatedTiles].map(([world, set]) => [world, [...set]])),
            message: "Tiles extracted and updates sent to frontend",
        });
    } catch (err) {
        console.error("Unexpected error during zip processing:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
});

module.exports = router;