import L from "leaflet";
import React, {type ChangeEvent, useEffect, useRef, useState} from "react";
import {DEFAULT_FONT} from "../constants.ts";
import PopupMessage from "./PopupMessage.tsx";
import {minecraftCoordsToPixels, minecraftCoordsToRegionCoords} from "../utils/MinecraftUtils.ts";

const baseUrl = import.meta.env.VITE_BACKEND_BASE_URL;
const apiKey = import.meta.env.VITE_BACKEND_API_KEY;

type MinecraftCoordinatesSearchBarProps = {
    world: string,
    map?: L.Map,
};

const MinecraftCoordinatesSearchBar = ({world, map }: MinecraftCoordinatesSearchBarProps) => {
    const [coordX, setCoordX] = useState<string>("");
    const [coordZ, setCoordZ] = useState<string>("");
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [popupId, setPopupId] = useState(0);

    const markerRef = useRef<L.Marker | null>(null);

    const redIcon = new L.Icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [20, 35],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [37, 37]
    });

    useEffect(() => {
        if (!map) {
            return;
        }
        if (markerRef.current) {
            markerRef.current.removeFrom(map);
            markerRef.current = null;
        }
        markerRef.current = L.marker([0,0], {icon: redIcon});
    }, [map, world]);

    const setError = (error: string) => {
        setErrorMessage(error);
        setPopupId(prev => prev + 1);
    }

    const handleXCoordChange = (e: ChangeEvent<HTMLInputElement>) => {
        const input = e.target?.value;
        setCoordX(input);
    }

    const handleZCoordChange = (e: ChangeEvent<HTMLInputElement>) => {
        const input = e.target?.value;
        setCoordZ(input);
    }

    const handleButtonClick = async() => {
        if (!map) {
            return;
        }

        const missingCoordinates = coordX.trim().length === 0 || coordZ.trim().length == 0;
        if (missingCoordinates) {
            setError("Please enter both coordinates!");
            return;
        }

        const parsedX = Number(coordX);
        const parsedZ = Number(coordZ);
        const invalidNumber = isNaN(parsedX) || isNaN(parsedZ);
        if (invalidNumber) {
            setError("Coordinates must be numbers!");
            return;
        }

        const {x, z} = minecraftCoordsToRegionCoords(parsedX, parsedZ);
        const url=`${baseUrl}/tiles/exists/${world}/0/${x}/${z}.png?apiKey=${apiKey}`;
        try {
            const res = await fetch(url);
            const data = await res.json();
            if (data.exists === false) {
                setError("Coordinates not within valid region!");
                return;
            }

            // coordinates are valid
            const {x: pixelX, z: pixelZ} = minecraftCoordsToPixels(parsedX, parsedZ);
            map.setView([pixelZ, pixelX], 0);
            if (markerRef.current) {
                const marker = markerRef.current;
                marker.addTo(map);
                marker.setLatLng([pixelZ, pixelX]);
                marker.bindPopup(`X:${coordX}, Z:${coordZ}`);
                marker.openPopup();
            }
        } catch (err) {
            console.error("Could not verify coordinates", err);
        }
    }

    return (
        <div style={styles.container}>
            <div style={{color: "white", textAlign: "center", fontSize: 13}}>Or go to coordinates</div>
            <div style={styles.inputContainer}>
                <input
                    type={"text"}
                    placeholder={"X"}
                    value={coordX}
                    onChange={handleXCoordChange}
                    onKeyDown={(e) => e.key === "Enter" && handleButtonClick()}
                    style={styles.input}
                />
                <input
                    type={"text"}
                    value={coordZ}
                    placeholder={"Z"}
                    onChange={handleZCoordChange}
                    onKeyDown={(e) => e.key === "Enter" && handleButtonClick()}
                    style={styles.input}
                />
                <button style={styles.button} onClick={handleButtonClick}>Go</button>
                {errorMessage  && (
                    <PopupMessage key={popupId} message={errorMessage} />
                )}
            </div>
        </div>
    );
};

export default MinecraftCoordinatesSearchBar;

const styles = {
    container: {
        position: "relative",
        display: 'flex',
        flexDirection: 'column',
        paddingTop: '10px',
        gap: 10,
    } as React.CSSProperties,
    inputContainer: {
        display: "flex",
        flexDirection: "row",
        justifyContent: "center",
        fontFamily: DEFAULT_FONT,
        width: "100%",
        gap: 20,
    } as React.CSSProperties,
    input: {
        border: "none",
        outline: "none",
        maxWidth: "85px",
        height: "30px",
        textAlign: "center",
        backgroundColor: '#1e1e1e',
        boxShadow: "0 0 10px rgba(0,0,0,0.5)",
        borderRadius: 10,
        color: 'white',
    } as React.CSSProperties,
    button: {
        backgroundColor: "#292929",
        color: "white",
        border: "none",
        borderRadius: 10,
        cursor: "pointer",
        fontSize: 12,
        boxShadow: "0 0 10px rgba(0,0,0,0.5)",
        fontFamily: DEFAULT_FONT,
        flexShrink: 0,
        minWidth: 40,
    },
};