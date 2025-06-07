const sqlite3 = require('sqlite3').verbose();
const db = new sqlite3.Database('./explored-chunks.db');

db.run(`
    CREATE TABLE IF NOT EXISTS chunks (
        world TEXT,
        x INTEGER, 
        z INTEGER,
        PRIMARY KEY (world, x, z)
    )
`);

module.exports = {
    addChunk(world, x, z) {
        return new Promise((resolve, reject) => {
            db.run(`INSERT OR IGNORE INTO chunks (world, x, z) VALUES (?, ?, ?)`, [world, x, z], function(err) {
                if (err) return reject(err);
                resolve();
            });
        });
    },

    addChunksBatch(world, chunks) {
        return new Promise((resolve, reject) => {
            db.serialize(() => {
                db.run('BEGIN TRANSACTION');

                const stmt = db.prepare(
                    `INSERT OR IGNORE INTO chunks (world, x, z) VALUES (?, ?, ?)`
                );

                for (const chunk of chunks) {
                    const {x, z} = chunk;
                    stmt.run(world, x, z);
                }

                stmt.finalize(err => {
                    if (err) return reject(err);

                    db.run('COMMIT', commitErr => {
                        if (commitErr) return reject(commitErr);
                        resolve();
                    });
                });
            });
        });
    },

    getChunks(world) {
        return new Promise((resolve, reject) => {
            db.all(`SELECT x, z FROM chunks WHERE world = ?`, [world], (err, rows) => {
                if (err) return reject(err);
                resolve(rows.map(r => `${r.x},${r.z}`));
            });
        });
    },

    hasChunk(world, x, z) {
        return new Promise((resolve, reject) => {
            db.get(`SELECT 1 FROM chunks WHERE world = ? and x = ? and z =  ?`, [world, x, z], (err, row) => {
                if (err) return reject(err);
                resolve(!!row);
            });
        });
    },

    clearChunks() {
        return new Promise((resolve, reject) => {
            const query = `DELETE FROM chunks`;

            db.run(query, function(err) {
                if (err) return reject(err);
                resolve(this.changes);
            });
        });
    },

    getChunkCountForWorld(world) {
        return new Promise((resolve, reject) => {
            db.get(`SELECT COUNT(*) as count FROM chunks WHERE world = ?`, [world], (err, row) => {
                if (err) return reject(err);
                resolve(row.count);
            });
        });
    }

};