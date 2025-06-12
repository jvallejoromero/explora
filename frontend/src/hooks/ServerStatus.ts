import { useEffect, useState } from 'react';
import { socket } from '../lib/socket';

export type ServerStatus = {
    isOnline: boolean;
    playerCount: number;
    worldTime: number;
    motd: string;
    lastUpdateSent: string | Date;
}

export type PlayerStatus = {
    name: string,
    world: string,
    x: number,
    y: number,
    z: number,
    yaw: number,
}

export function useServerStatus() {
    const [serverStatus, setServerStatus] = useState<ServerStatus | null>(null);
    const [onlinePlayers, setOnlinePlayers] = useState<PlayerStatus[]>([]);
    const [connectedToBackend, setConnectedToBackend] = useState(false);
    const [loadingServerStatus, setLoadingServerStatus] = useState(true);

    useEffect(() => {
        if (!socket.connected) {
            socket.connect();
        }

        const handleConnect = () => {
            console.log("🟢 Connected");
            setConnectedToBackend(true);
        };

        const handleDisconnect = () => {
            console.log("🔴 Disconnected");
            setConnectedToBackend(false);
        };

        const handleServerUpdate = (data: ServerStatus) => {
            setServerStatus(data);
            setLoadingServerStatus(false);
        }

        const handlePlayersUpdate = (data: {"online-players": PlayerStatus[]}) => {
            setOnlinePlayers(data["online-players"]);
        }

        socket.on('connect', handleConnect);
        socket.on('disconnect', handleDisconnect);
        socket.on('connect_error', (err) => {
            console.error('❌ connect_error:', err);
            setLoadingServerStatus(false);
        });
        socket.on('serverStatusUpdate', handleServerUpdate);
        socket.on('onlinePlayersUpdate', handlePlayersUpdate);

        return () => {
            socket.off("connect", handleConnect);
            socket.off("disconnect", handleDisconnect);
            socket.off("serverStatusUpdate", handleServerUpdate);
            socket.off("onlinePlayersUpdate", handlePlayersUpdate);
        }
    }, []);

    return {serverStatus, onlinePlayers, connectedToBackend, loadingServerStatus};
}