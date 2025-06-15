import React, {useEffect, useState} from "react";
import {DEFAULT_FONT} from "../constants.ts";
import "../css/player-search-bar.css"

type PopupMessageProps = {
    message: string,
    timeout?: number,
    offsetTop?: number,
}

const PopupMessage = ({ timeout=1500, offsetTop=-5, message }: PopupMessageProps) => {
    const [active, setActive] = useState(true);

    useEffect(() => {
        const timer = setTimeout(() => {setActive(false)}, timeout);
        return () => clearTimeout(timer);
    }, [message]);

    return (
        <div>
            {active && (
                <div style={{...styles.popupBox, top: offsetTop}}>{message}</div>
            )}
        </div>
    )
}

export default PopupMessage

const styles = {
    popupBox: {
        position: "absolute",
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
}