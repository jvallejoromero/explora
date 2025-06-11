import React, {useState} from 'react';
import { MapContainer } from 'react-leaflet';
import L from 'leaflet';

import { COLOR_PRIMARY, COLOR_SECONDARY, DEFAULT_FONT, LOGO_IMAGE } from '../constants'
import { FaBars, FaCheckCircle, FaTimesCircle } from "react-icons/fa";
import "@fontsource/inter/600.css";

import {useServerStatus} from "../hooks/ServerStatus.ts";
import VisibleTileLoader from "../components/VisibleTileLoader.tsx";
import WorldsDropdown from "../components/WorldsDropdown.tsx";
import MapOverlay from "../components/MapOverlay.tsx";
import NetherPortalTransition from "../components/NetherPortalTransition.tsx";
import PlayersOverlay from "../components/PlayersOverlay.tsx";

const ExploraMapViewer = () => {
    const { serverStatus, connectedToBackend } = useServerStatus();
    const [selectedWorld, setSelectedWorld] = useState<string>('world');
    const [nextWorld, setNextWorld] = useState<string | null>(null);
    const [transitioning, setTransitioning] = useState(false);

    const handleWorldSelect = (world: string) => {
        if (world.toLowerCase() === "overworld") {
            setNextWorld("world");
        } else if (world.toLowerCase().includes("nether")) {
            setNextWorld("world_nether");
        } else if (world.toLowerCase().includes("end")) {
            setNextWorld("world_the_end");
        } else {
            setNextWorld(world);
        }
        setTransitioning(true);
    }

    const handleTransitionComplete = () => {
        if (nextWorld) {
            console.log("NEXT WORLD=", nextWorld);
            setSelectedWorld(nextWorld);
            setNextWorld(null);
        }
        setTransitioning(false);
    }

    return (
        <div style={styles.root}>
            {/* Header */}
            <div style={styles.header}>
                <div style={styles.headerLeft}>
                    <img src={LOGO_IMAGE} alt="Explora Logo"/>
                    <h1 style={ {color: "white", fontSize: 28, paddingLeft: 10} }>Explora </h1>
                </div>
                <FaBars size={22} style={{ cursor: "pointer", color: "white" }} />
            </div>

            {/* Body Content */}
            <div style={styles.body}>
                <div style={styles.worldContainer}>
                    <WorldsDropdown
                        onSelectWorld={handleWorldSelect}
                        disabled={!connectedToBackend}
                    />
                    <div style={styles.worldStatus}>
                        {serverStatus?.isOnline ? (
                            <>
                                <FaCheckCircle size={18} color="#43ad95" />
                                <div style={{color: "#43ad95"}}>Online</div>
                            </>
                        ) : (
                            <>
                                <FaTimesCircle size={18} color="#e25353" />
                                <div style={{color: "#e25353"}}>Offline</div>
                            </>
                        )}
                    </div>
                </div>

                {/* Leaflet Map */}
                <div style={styles.mapContainer}>
                    <MapContainer
                        crs={L.CRS.Simple}
                        zoom={0}
                        center={[0,0]}
                        minZoom={-6}
                        maxZoom={2}
                        style={styles.map}
                    >
                        {connectedToBackend ? (
                            <>
                                {transitioning && <NetherPortalTransition onComplete={handleTransitionComplete} />}
                                <MapOverlay />
                                <VisibleTileLoader key={selectedWorld} world={selectedWorld} />
                                <PlayersOverlay world={selectedWorld}/>
                            </>
                        ) : (
                            <>
                                <div style={styles.mapMessageContainer}>
                                    <div style={styles.errorMsg}>Connection to backend server failed. Is it online?</div>
                                </div>
                            </>
                        )}
                    </MapContainer>
                </div>

            </div>
        </div>
    )
}

export default ExploraMapViewer;

const styles = {
    root: {
        display: "flex",
        height: "100%",
        flex: 1,
        backgroundColor: COLOR_PRIMARY,
        fontFamily: DEFAULT_FONT,
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "stretch",
    } as React.CSSProperties,
    header: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        gap: "12px",
        padding: "15px 12px",
        width: "100%",
        borderBottom: "1px solid rgba(80, 150, 200, 0.2)",
    },
    headerLeft: {
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
    } as React.CSSProperties,
    body: {
        display: "flex",
        flex: 1,
        flexDirection: "column",
        justifyContent: "flex-start",
        backgroundColor: COLOR_SECONDARY,
        padding: 25,
    } as React.CSSProperties,
    worldContainer: {
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
        gap: "10px",
    } as React.CSSProperties,
    worldTitle: {
        color: 'white',
        fontFamily: DEFAULT_FONT,
        fontSize: 30,
        lineHeight: "30px",
        margin: 0,
    },
    worldStatus: {
        display: "flex",
        flexDirection: "row",
        alignItems: "center",
        gap: 5,
    } as React.CSSProperties,
    mapContainer: {
        position: "relative",
        flex: 1,
        marginTop: 25,
        borderRadius: 10,
        overflow: "hidden",
        boxShadow: "0 0 8px rgba(0,0,0,0.2)",
    } as React.CSSProperties,
    map: {
        height: "500px",
        width: "100%",
        backgroundColor: "#0e0e0e",
        borderRadius: 10,
        overflow: "hidden",
    },
    mapMessageContainer: {
        display: "flex",
        height: "100%",
        alignItems: "center",
        justifyContent: "center",
        padding: 25,
    },
    errorMsg: {
        color: "#e25353",
        fontSize: "clamp(12px, 2vw, 24px)",
    },
};