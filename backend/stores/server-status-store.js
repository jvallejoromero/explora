let lastUpdateTime = Date.now();

let serverStatus = {
    isOnline: false,
    playerCount: 0,
    worldTime: null,
    motd: '',
    lastUpdateSent: null,
}

module.exports = {
    getStatus: () => serverStatus,
    updateStatus: (newData) => {
        lastUpdateTime = Date.now();
        serverStatus = newData;
    },
    checkStaleness: () => {
        const age = Date.now() - lastUpdateTime;
        if (age > 5000 && serverStatus.isOnline) {
            serverStatus.isOnline = false;
            return true;
        }
        return false;
    },
};