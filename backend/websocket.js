let io = null;

module.exports = {
    init: (server) => {
        const socketIo = require("socket.io");
        io = socketIo(server, { cors: { origin: "*" } });
        return io;
    },

    getIO: () => {
        if (!io) {
            throw new Error("Socket.io not initialized!");
        }
        return io;
    }
}
