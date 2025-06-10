const express = require("express");
const router = express.Router();

const multer = require("multer");
const upload = multer({ storage: multer.memoryStorage() });
const unzipper = require("unzipper");

const path = require("path");
const fs = require("fs");

const TILES_DIR = path.join(__dirname, "../tiles"); // <-- your actual tiles/ folder

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
            }
        }

        return res.status(200).json({ status: "success", message: "Tiles extracted" });
    } catch (err) {
        console.error("Unexpected error during zip processing:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
});

module.exports = router;