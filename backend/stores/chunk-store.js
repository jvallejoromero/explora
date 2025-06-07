/**
 * Interface for chunk storage modules
 * Implementations: chunk-store-sqlite.js or chunk-store-redis.js in future versions
 */
module.exports = {
    /**
     * Add a chunk to storage
     * @param {string} world
     * @param {number} x
     * @param {number} z
     * @returns {Promise<void>}
     */
    addChunk(world, x, z) {
        throw new Error("Method not implemented.");
    },

    /**
     * Check if a chunk is already stored
     * @param {string} world
     * @param {number} x
     * @param {number} z
     * @returns {Promise<boolean>}
     */
    hasChunk(world, x, z) {
        throw new Error("Method not implemented.");
    },

    /**
     * Get all chunks for a world
     * @param {string} world
     * @returns {Promise<string[]>} array of "x,z" strings
     */
    getChunks(world) {
        throw new Error("Method not implemented.");
    },

    /**
     * Deletes all chunks in the database
     */
    clearChunks() {
        throw new Error("Method not implemented.");
    }
};