import {FaSearch} from "react-icons/fa";
import {DEFAULT_FONT} from "../constants.ts";
import React, {useEffect, useState} from "react";
import L from "leaflet";
import {type PlayerStatus, useServerStatus} from "../hooks/ServerStatus.ts";
import {minecraftCoordsToPixels} from "../utils/MinecraftUtils.ts";
import "../css/player-search-bar.css";

type PlayerSearchBarProps = {
    placeholder?: string;
    map?: L.Map;
};

const PlayerSearchBar = ( { placeholder="Search for a player..", map }: PlayerSearchBarProps) => {
    const { onlinePlayers } = useServerStatus();
    const [query, setQuery] = useState("");
    const [lastSearch, setLastSearch] = useState<PlayerStatus | string>("");
    const [notFound, setNotFound] = useState(false);

    const onSubmit = () => {
        if (!map) return;

        console.log("Searching for player", query);
        const foundPlayer = onlinePlayers.find((player) => player.name === query);

        if (foundPlayer) {
            const { x, z } = minecraftCoordsToPixels(foundPlayer!.x, foundPlayer!.z);
            map.setView([z, x], 0);
            setLastSearch(foundPlayer);
        } else {
            const maxNameLength = 16;
            const ellipsis = "..";
            const shouldTruncate = query.length > maxNameLength;
            const trimmedName = shouldTruncate
                ? query.slice(0, maxNameLength - ellipsis.length) + ellipsis
                : query;

            setNotFound(true);
            setLastSearch(trimmedName);
        }
    }

    useEffect(() => {
        if (notFound) {
            const timer = setTimeout(() => setNotFound(false), 1500);
            return () => clearTimeout(timer);
        }
    }, [notFound]);

    return (
        <div style={styles.wrapper}>
            {notFound && (
                <div style={styles.popupBox}>
                    <div>
                        {typeof lastSearch === "string"
                            ?  lastSearch.trim().length === 0
                                ? `Please enter a username!`
                                : `Player ${lastSearch} is not online!`
                            :  `Player not online!`}
                    </div>
                </div>
            )}
            <div style={styles.container}>
                <FaSearch style={styles.icon}/>
                <input
                    type={"text"}
                    value={query}
                    placeholder={placeholder}
                    onChange={(e) => setQuery(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && onSubmit?.()}
                    style={styles.input}
                />
                <button style={styles.button} onClick={onSubmit}>Find</button>
            </div>
        </div>
    )
};

export default PlayerSearchBar

const styles = {
    wrapper: {
        position: "relative",
        width: "100%",
        minWidth: "200px",
    } as React.CSSProperties,
    popupBox: {
        position: "absolute",
        top: "-40px",
        left: 0,
        backgroundColor: "#2a2a2a",
        color: "white",
        padding: "8px 12px",
        borderRadius: 8,
        boxShadow: "0 0 6px rgba(0,0,0,0.4)",
        fontFamily: DEFAULT_FONT,
        fontSize: 13,
        whiteSpace: "nowrap",
        zIndex: 10,
        animation: "fadeInPopup 0.2s ease-in-out",
        transition: "opacity 0.3s ease-in-out",
    } as React.CSSProperties,
    container: {
        display: "flex",
        alignItems: "center",
        backgroundColor: '#1e1e1e',
        borderRadius: 12,
        padding: "8px 12px",
        boxShadow: "0 0 5px rgba(0,0,0,0.5)",
        color: "white",
        width: "100%",
        maxWidth: "250px",
        maxHeight: "30px",
        fontFamily: DEFAULT_FONT,
    },
    icon: {
        marginRight: 8,
        color: "white",
        flexShrink: 0,
        height: 12,
    },
    input: {
        flex: 1,
        border: "none",
        outline: "none",
        backgroundColor: "transparent",
        color: "white",
        fontsize: 14,
    },
    button: {
        backgroundColor: "#292929",
        color: "white",
        border: "none",
        borderRadius: 5,
        padding: "4px 5px",
        cursor: "pointer",
        fontFamily: DEFAULT_FONT,
        fontSize: 12,
        boxShadow: "0 0 3px rgba(0,0,0,0.3)",
        flexShrink: 0,
    },
};