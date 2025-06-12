import {Marker, Popup, useMap} from "react-leaflet";
import {useEffect, useState} from "react";
import {type PlayerStatus, useServerStatus} from "../hooks/ServerStatus.ts";
import L from "leaflet";
import {minecraftCoordsToPixels} from "../utils/MinecraftUtils.ts";

type PlayersOverlayProps = {
    world: string,
}


const createPlayerIcon = (name: string, yaw: number, zoom: number) => {
    const maxZoom = 2;
    const minZoom = -6;
    const normalizedZoom = (zoom - minZoom) / (maxZoom - minZoom);
    const scale = 0.25 + normalizedZoom;
    const size = 24 * scale;

    const svgHtml = (zoom > -3) ? `
        <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24"
            style="
                position: absolute;
                top: -14px;
                left: 0;
                right: 0;
                margin: auto;
                transform: rotate(${yaw - 180}deg);
                fill: #ff3c3c;
                opacity: 0.9;
                z-index: 2;
                transition: transform 0.25s ease-out;
                pointer-events: none;
            ">
            <polygon points="12,-4 4,20 20,20" />
        </svg>
        ` : '';

    return new L.DivIcon({
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        className: "",
        html: `
        <div style="position: relative; pointer-events: auto">
            <img 
                src="https://mc-heads.net/avatar/${name}/${size}.png" 
                style="
                    border-radius: 4px;
                    box-shadow: 0 0 3px rgba(0,0,0,0.5);
                    pointer-events: auto;
                "
            />
            ${svgHtml}
        </div>
    `,
    });
}

const PlayersOverlay = ({ world }: PlayersOverlayProps) => {
    const map = useMap();
    const [players, setPlayers] = useState<PlayerStatus[]>([]);
    const [zoom, setZoom] = useState(map.getZoom());
    const { onlinePlayers } = useServerStatus();

    // update map zoom
    useEffect(() => {
        const updateZoom = () => {
            console.log("updated zoom", map.getZoom());
            setZoom(map.getZoom());
        }
        const cleanup: () => void = () => {
            map.off("zoomend", updateZoom);
        };
        map.on("zoomend", updateZoom);
        return cleanup;
    }, [map]);

    // update player markers
    useEffect(() => {
        const visiblePlayers = onlinePlayers.filter((player) => {
            return player.world === world;
        });
        setPlayers(visiblePlayers);
    }, [world, onlinePlayers]);

    return (
        <>
            {players.map((player) => {
                if (zoom <= -6) return null;
                const {x, z} = minecraftCoordsToPixels(player.x, player.z);

                return (
                    <Marker
                        key={player.name}
                        position={[z,x]}
                        icon={createPlayerIcon(player.name, player.yaw, zoom)}
                        interactive={true}
                    >
                        <Popup>
                            <div>
                                <strong>{player.name}</strong> <br />
                                x: {Math.floor(player.x)}<br />
                                y: {Math.floor(player.y)}<br />
                                z: {Math.floor(player.z)}
                            </div>
                        </Popup>
                    </Marker>
                )
            })}
        </>
    )
}

export default PlayersOverlay