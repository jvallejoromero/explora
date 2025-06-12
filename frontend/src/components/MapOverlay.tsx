import {useMouseCoords} from "../hooks/MapMouseTracker.tsx";
import {useServerStatus} from "../hooks/ServerStatus.ts";
import {pixelsToMinecraftCoords} from "../utils/MinecraftUtils.ts";
import React from 'react';
import LiveMinecraftClock from "./LiveMinecraftClock.tsx";

const MapOverlay = () => {
    const {mouseCoords} = useMouseCoords();
    const {serverStatus} = useServerStatus();
    const minecraftCoords = mouseCoords ? pixelsToMinecraftCoords(mouseCoords?.x, mouseCoords?.z) : null;

    return (
        <div style={{...styles.container, display: minecraftCoords || serverStatus?.isOnline ? 'block' : 'none'}}>
            {minecraftCoords && (
                <div>X: {minecraftCoords.x}, Z: {minecraftCoords.z}</div>
            )}
            {serverStatus?.isOnline && (
                <LiveMinecraftClock worldTime={serverStatus.worldTime}/>
            )}
        </div>
    )
}

const styles = {
    container: {
        position: 'absolute',
        top: 10,
        right: 10,
        color: 'white',
        backgroundColor: 'rgba(0,0,0,0.5)',
        padding: 6,
        borderRadius: 4,
        zIndex: 1000,
        pointerEvents: 'none',
    } as React.CSSProperties,
}

export default MapOverlay