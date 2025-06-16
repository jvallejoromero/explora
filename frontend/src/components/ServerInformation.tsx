import {useServerStatus} from "../hooks/ServerStatus.ts";
import { DEFAULT_FONT } from "../constants.ts";
import React, {useEffect, useState} from "react";

const baseUrl = import.meta.env.VITE_BACKEND_BASE_URL;
const apiKey = import.meta.env.VITE_BACKEND_API_KEY;

type ServerInformationProps = {
    world: string,
}

const ServerInformation = ({world}: ServerInformationProps) => {
    const { onlinePlayers } = useServerStatus();
    const [chunkCount, setChunkCount] = useState<number | undefined>(undefined);

    const fetchChunkCount = async() => {
        try {
            const url = `${baseUrl}/debug/chunk-count/${world}?apiKey=${apiKey}`;
            const res = await fetch(url);
            const data = await res.json();
            setChunkCount(data.totalChunks);
        } catch (err) {
            console.log("Could not fetch chunk count", err);
        }
    }

    useEffect(() => {
        (async () => {
            await fetchChunkCount();
        })();
    }, [world]);

    return (
        <div style={styles.container}>
            <div>Online Players: {onlinePlayers.length}</div>
            <div>Chunks: {chunkCount}</div>
        </div>
    )
}

const styles = {
    container: {
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-evenly",
        color: 'white',
        fontSize: 14,
        fontFamily: DEFAULT_FONT,
    } as React.CSSProperties,
};

export default ServerInformation