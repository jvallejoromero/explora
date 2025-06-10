import React, {useState} from 'react';
import { MapContainer } from 'react-leaflet';
import L from 'leaflet';

import { COLOR_PRIMARY, COLOR_SECONDARY, DEFAULT_FONT, LOGO_IMAGE } from '../constants'
import { FaBars, FaCheckCircle } from "react-icons/fa";
import "@fontsource/inter/600.css";

import VisibleTileLoader from "../components/VisibleTileLoader.tsx";
import WorldsDropdown from "../components/WorldsDropdown.tsx";

const ExploraMapViewer = () => {

    const [selectedWorld, setSelectedWorld] = useState<string>('world');

    const handleWorldSelect = (world: string) => {
        if (world.toLowerCase() === "overworld") {
            setSelectedWorld("world");
        } else if (world.toLowerCase().includes("nether")) {
            setSelectedWorld("world_nether");
        } else if (world.toLowerCase().includes("end")) {
            setSelectedWorld("world_the_end");
        } else {
            setSelectedWorld(world);
        }
        console.log("SELECTED WORLD", selectedWorld);
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
                    />
                    <div style={styles.worldStatus}>
                        <FaCheckCircle size={18} color="#43ad95" />
                        <div style={{color: "#43ad95"}}>Online</div>
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
                        <VisibleTileLoader key={selectedWorld} world={selectedWorld} />
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
        flex: 1,
        marginTop: 25,
        borderRadius: 10,
        overflow: "hidden",
        boxShadow: "0 0 8px rgba(0,0,0,0.2)",
    },
    map: {
        height: "500px",
        width: "65%",
        backgroundColor: "#0e0e0e",
        borderRadius: 10,
        overflow: "hidden",
    },
};