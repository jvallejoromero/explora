import React, {useRef, useState} from 'react';
import { MapContainer } from 'react-leaflet';
import L from 'leaflet';

import { COLOR_PRIMARY, COLOR_SECONDARY, COLOR_SUBTEXT, DEFAULT_FONT, LOGO_IMAGE } from '../constants'
import { FaBars, FaCheckCircle, FaTimesCircle } from "react-icons/fa";
import "@fontsource/inter/600.css";

import {useServerStatus} from "../hooks/ServerStatus.ts";
import VisibleTileLoader from "../components/VisibleTileLoader.tsx";
import WorldsDropdown from "../components/WorldsDropdown.tsx";
import MapOverlay from "../components/MapOverlay.tsx";
import NetherPortalTransition from "../components/NetherPortalTransition.tsx";
import PlayersOverlay from "../components/PlayersOverlay.tsx";
import "../css/utility.css"
import SavedMapView from "../components/SavedMapView.tsx";
import ServerInformation from "../components/ServerInformation.tsx";
import PlayerSearchBar from "../components/PlayerSearchBar.tsx";
import MinecraftCoordinatesSearchBar from "../components/MinecraftCoordinatesSearchBar.tsx";

const ExploraMapViewer = () => {
    const savedView = localStorage.getItem(`savedMapView`);
    let selectedWorldName = "world";

    if (savedView) {
        selectedWorldName = JSON.parse(savedView).world;
    }

    const { serverStatus, loadingServerStatus, connectedToBackend } = useServerStatus();
    const [selectedWorld, setSelectedWorld] = useState<string>(selectedWorldName);
    const [nextWorld, setNextWorld] = useState<string | null>(null);
    const [transitioning, setTransitioning] = useState(false);

    const mapRef = useRef<L.Map | null>(null);

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
            setSelectedWorld(nextWorld);
            setNextWorld(null);
        } else {
            console.log(" NO SELECTED");
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
                    <sup style={{color: "white", fontSize: "0.5em", top: "-0.75em", position: "relative", paddingLeft: 2}}>beta</sup>
                </div>
                <FaBars size={22} style={{ cursor: "pointer", color: "white" }} />
            </div>

            {/* Body Content */}
            <div style={styles.body}>

                <div style={styles.controlSection}>
                    <div style={styles.worldContainer}>
                        <WorldsDropdown
                            onSelectWorld={handleWorldSelect}
                            disabled={!connectedToBackend}
                        />
                        <div style={styles.worldStatus}>
                            {!connectedToBackend || !serverStatus ? (
                                <div className="spinner" style={{width: 18, height: 18}} />
                            ) : serverStatus?.isOnline ? (
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
                </div>

                <div style={styles.mapAndToolsSection}>
                    {/* Leaflet Map */}
                    <div style={styles.mapContainer}>
                        <MapContainer
                            crs={L.CRS.Simple}
                            zoom={0}
                            center={[0,0]}
                            minZoom={-6}
                            maxZoom={2}
                            style={styles.map}
                            ref={(mapInstance) => {
                                if (mapInstance) {mapRef.current = mapInstance;}
                            }}
                        >
                            {(loadingServerStatus && !connectedToBackend) ? (
                                <>
                                    <div style={styles.spinnerOverlay}>
                                        <div className="spinner" style={{width: 32, height: 32}} />
                                    </div>
                                </>
                            ) : connectedToBackend ? (
                                <>
                                    {transitioning && <NetherPortalTransition onComplete={handleTransitionComplete} />}
                                    <MapOverlay />
                                    <VisibleTileLoader key={selectedWorld} world={selectedWorld} />
                                    <PlayersOverlay world={selectedWorld}/>
                                    <SavedMapView world={selectedWorld} />
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

                    {/* Tools & settings UI */}
                    <div style={styles.toolsContainer}>
                        <div style={styles.toolsHeader}>Tools & Settings</div>
                        <div style={styles.toolsContent}>
                            <ServerInformation world={selectedWorld}/>
                            <PlayerSearchBar map={mapRef.current ?? undefined} world={selectedWorld}/>
                            <MinecraftCoordinatesSearchBar world={selectedWorld} map={mapRef.current ?? undefined} />
                        </div>
                    </div>
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
        flexWrap: "wrap",
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
        flex: "3 1 500px",
        minWidth: "300px",
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
    spinnerOverlay: {
        position: "absolute",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "rgba(0,0,0,0.3)",
        zIndex: 1000,
    } as React.CSSProperties,
    controlSection: {
        display: "flex",
        flexDirection: "column",
        gap: 10,
        width: "100%",
        marginBottom: 20,
    } as React.CSSProperties,
    mapAndToolsSection: {
        display: "flex",
        flexDirection: "row",
        flexWrap: "wrap",
        flex: 1,
        width: "100%",
        gap: 10,
    } as React.CSSProperties,
    toolsContainer: {
        display: "flex",
        flexDirection: "column",
        flex: "1 1 300px",
        height: "auto",
        padding: 16,
        alignItems: "center",
    } as React.CSSProperties,
    toolsHeader: {
        color: COLOR_SUBTEXT,
        fontFamily: DEFAULT_FONT,
        fontSize: "clamp(20px, 4vw, 25px)",
        textAlign: "center",
        width: "100%",
    } as React.CSSProperties,
    toolsContent: {
        margin: 10,
        display: "flex",
        flex: 1,
        flexDirection: "column",
        justifyContent: "flex-start",
        gap: 12,
    } as React.CSSProperties,
};