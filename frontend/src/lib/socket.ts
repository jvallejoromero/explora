import { io } from 'socket.io-client';

const websocketURL = import.meta.env.VITE_BACKEND_WEBSOCKET_URL;

export const socket = io(websocketURL, {
    autoConnect: false,
});