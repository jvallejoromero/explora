import L from "leaflet";
import React, {type ChangeEvent, useState} from "react";
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
    const [coordX, setCoordX] = useState<number>(0);
    const [coordZ, setCoordZ] = useState<number>(0);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [popupId, setPopupId] = useState(0);

    const setError = (error: string) => {
        setErrorMessage(error);
        setPopupId(prev => prev + 1);
    }

    const handleXCoordChange = (e: ChangeEvent<HTMLInputElement>) => {
        const input = e.target?.value;
        const parsedInput = Number(input);
        if (isNaN(parsedInput)) {
            return;
        }
        setCoordX(parsedInput);
    }

    const handleZCoordChange = (e: ChangeEvent<HTMLInputElement>) => {
        const input = e.target?.value;
        const parsedInput = Number(input);
        if (isNaN(parsedInput)) {
            return;
        }
        setCoordZ(parsedInput);
    }

    const handleButtonClick = async() => {
        if (!map) {
            return;
        }

        if (!coordX || !coordZ) {
            setError("Please enter both coordinates!");
            console.log("coords not set");
            return;
        }
        const {x, z} = minecraftCoordsToRegionCoords(coordX, coordZ);
        const url=`${baseUrl}/tiles/exists/${world}/0/${x}/${z}.png?apiKey=${apiKey}`;
        console.log("Checking tile:", url);
        try {
            const res = await fetch(url);
            const data = await res.json();
            if (data.exists === false) {
                setError("Coordinates not within valid region!");
                return;
            }

            const {x: pixelX, z: pixelZ} = minecraftCoordsToPixels(coordX, coordZ);
            map.setView([pixelZ, pixelX], 0);
        } catch (err) {
            console.error("Could not verify coordinates", err);
        }
    }

    return (
        <div style={styles.container}>
            <div style={{color: 'white', textAlign: 'center', fontSize: 13}}>Or go to coordinates</div>
            <div style={styles.inputContainer}>
                <input
                    type={"text"}
                    placeholder={"X"}
                    value={coordX}
                    onChange={handleXCoordChange}
                    style={styles.input}
                />
                <input
                    type={"text"}
                    value={coordZ}
                    placeholder={"Z"}
                    onChange={handleZCoordChange}
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