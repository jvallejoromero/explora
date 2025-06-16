import {FaSearch} from "react-icons/fa";
import {DEFAULT_FONT} from "../constants.ts";
import React, {useState} from "react";
import L from "leaflet";
import {minecraftCoordsToPixels} from "../utils/MinecraftUtils.ts";
import "../css/player-search-bar.css";
import PopupMessage from "./PopupMessage.tsx";
import {useServerStatus} from "../hooks/ServerStatus.ts";

type PlayerSearchBarProps = {
    placeholder?: string;
    map?: L.Map;
    world: string,
};

const PlayerSearchBar = ( { placeholder="Search for a player..", map, world }: PlayerSearchBarProps) => {
    const { onlinePlayers } = useServerStatus();
    const [query, setQuery] = useState("");

    const [suggestions, setSuggestions] = useState<string[]>([]);
    const [suggestionsIndex, setSuggestionsIndex] = useState(0);

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [popupId, setPopupId] = useState(0);

    const trimString = (input: string): string => {
        const maxNameLength = 16;
        const ellipsis = "..";
        const shouldTruncate = input.length > maxNameLength;
        const trimmedName = shouldTruncate
            ? query.slice(0, maxNameLength - ellipsis.length) + ellipsis
            : query;

        return trimmedName;
    }

    const setError = (error: string) => {
        setErrorMessage(error);
        setPopupId(prev => prev + 1);
    }

    const onSubmit = () => {
        if (!map) return;

        if (query.trim().length === 0) {
            setError("Please enter a username!");
            return;
        }

        const foundPlayer = onlinePlayers.find((player) => player.name.toLowerCase() === query.toLowerCase());

        if (foundPlayer) {
            if (foundPlayer.world !== world) {
                setError(`Player ${foundPlayer.name} is in "${foundPlayer.world}", but you're viewing "${world}".`);
                return;
            }

            const { x, z } = minecraftCoordsToPixels(foundPlayer!.x, foundPlayer!.z);
            map.setView([z, x], 0);
        } else {
            const trimmedName = trimString(query);
            setError(`Player ${trimmedName} is not online!`);
        }
    }

    const handleInput = (e: React.ChangeEvent<HTMLInputElement>) => {
        const input = e.target.value;
        setQuery(input);

        const playerMatches = onlinePlayers.filter((player) => player.name.toLowerCase().startsWith(input.toLowerCase())).map((player) => player.name);
        setSuggestions(playerMatches);
    }

    const handleTabPress = () => {
        if (query.trim().length === 0) {
            setError("Please enter a username!");
            return;
        }
        if (suggestions.length === 0) {
            const trimmedInput = trimString(query);
            setError(`No suggestions found for: ${trimmedInput}`);
            return;
        }
        const next = (suggestionsIndex + 1) % suggestions.length;
        setSuggestionsIndex(next);
        setQuery(suggestions[next]);
     }

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            onSubmit();
        } else if (e.key === "Tab") {
            e.preventDefault();
            handleTabPress();
        }
    }

    return (
        <div style={styles.wrapper}>
            {errorMessage && (
                <PopupMessage key={popupId} message={errorMessage} offsetTop={-40} />
            )}
            <div style={styles.container}>
                <FaSearch style={styles.icon}/>
                <input
                    type={"text"}
                    value={query}
                    placeholder={placeholder}
                    onChange={handleInput}
                    onKeyDown={handleKeyDown}
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