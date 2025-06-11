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
    const scale = 0.5 + normalizedZoom * 1.0;
    const size = 24 * scale;

    return new L.DivIcon({
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2],
        html: `
        <div style="position: relative;">
            <img 
                src="https://mc-heads.net/avatar/${name}/${size}/nohelm.png" 
                style="
                    border-radius: 4px;
                    box-shadow: 0 0 3px rgba(0,0,0,0.5);
                "
            />
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24"
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
                ">
                <polygon points="12,-4 4,20 20,20" />
            </svg>
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
                // if (zoom < -3) return null;
                const {x, z} = minecraftCoordsToPixels(player.x, player.z);

                return (
                    <Marker
                        key={player.name}
                        position={[z,x]}
                        icon={createPlayerIcon(player.name, player.yaw, zoom)}
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