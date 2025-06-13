import { useMap } from "react-leaflet";
import { ImageOverlay } from "react-leaflet";
import {useEffect, useState} from "react";
import React from "react";
import { socket } from '../lib/socket';

const TILE_SIZE = import.meta.env.VITE_TILE_SIZE;
const apiKey = import.meta.env.VITE_BACKEND_API_KEY;
const baseUrl = import.meta.env.VITE_BACKEND_BASE_URL;

type Props = {
    world: string;
};

const MemoTile = React.memo(({ world, x, z, versionParam }: { world: string, x: number; z: number, versionParam: string }) => {
    const overlayRef = React.useRef<L.ImageOverlay | null>(null);

    useEffect(() => {
        const overlay = overlayRef.current;
        if (!overlay) return;

        const leafletOverlay = overlay as unknown as { _image: HTMLImageElement };
        const img = leafletOverlay._image;

        if (!img) return;

        // Start invisible
        img.style.opacity = "0";
        img.style.transition = "opacity 0.3s ease-in";

        // When image loads, fade it in
        img.onload = () => {
            requestAnimationFrame(() => {
                img.style.opacity = "1";
            });
        };
    }, [world, x, z]);

    return (
        <ImageOverlay
            ref={(ref) => {
                overlayRef.current = ref as L.ImageOverlay;
            }}
            key={`${world}_${x}_${z}_${versionParam}`}
            url={baseUrl + `/tiles/${world}/0/${x}/${z}.png?apiKey=${apiKey}${versionParam}`}
            bounds={[
                [(z + 1) * TILE_SIZE * -1, x * TILE_SIZE],
                [z * TILE_SIZE * -1, (x + 1) * TILE_SIZE],
            ]}
            crossOrigin="anonymous"
        />
    );
});

const VisibleTileLoader: React.FC<Props> = ({ world }) => {
    const map = useMap();
    const [tiles, setTiles] = useState<{x: number, z: number}[]>([]);
    const tileExistenceCache = React.useRef(new Map<string, boolean>()).current;
    const updatedTiles = React.useRef(new Set<string>()).current;

    useEffect(() => {
        if (!socket.connected) {
            return;
        }
        const handleTileUpdate = (data: { world: string, tiles: {x: number, z: number}[] }) => {
            data.tiles.forEach(({x, z}) => {
                updatedTiles.add(`${data.world}_${x}_${z}`);
                console.log("Received tile update for:", world, x, z);
            });
        };
        socket.on("tileUpdate", handleTileUpdate);
        return () => {
            socket.off("tileUpdate", handleTileUpdate);
        }
    }, []);

    useEffect(() => {
        const updateVisibleTiles = async () => {
            const bounds = map.getBounds();
            if (!bounds) {
                console.warn("Map bounds not available yet.");
                return;
            }

            const zMin = Math.floor(bounds.getNorth() / -TILE_SIZE); // lowest visible Z
            const zMax = Math.floor(bounds.getSouth() / -TILE_SIZE); // highest visible Z

            const xMin = Math.floor(bounds.getWest() / TILE_SIZE);
            const xMax = Math.floor(bounds.getEast() / TILE_SIZE);

            const visibleTiles: { x: number; z: number }[] = [];

            for (let z = zMin; z <= zMax; z++) {
                for (let x = xMin; x <= xMax; x++) {
                    const key = `${world}_${x}_${z}`;
                    if (tileExistenceCache.has(key)) {
                        if (tileExistenceCache.get(key)) {
                            visibleTiles.push({x, z});
                        }
                    } else {
                        const res = await fetch(baseUrl + `/tiles/exists/${world}/0/${x}/${z}.png?apiKey=${apiKey}`);
                        const exists = res.ok;
                        tileExistenceCache.set(key, exists);
                        if (exists) {
                            visibleTiles.push({ x, z });
                        }
                    }
                }
            }

            setTiles((prev) => {
                const newKeys = new Set(visibleTiles.map(({ x, z }) => `${x}_${z}`));
                const prevKeys = new Set(prev.map(({ x, z }) => `${x}_${z}`));

                const same = newKeys.size === prevKeys.size && [...newKeys].every((key) => prevKeys.has(key));
                return same ? prev : visibleTiles;
            });
        };

        map.whenReady(() => {
            updateVisibleTiles().catch((err) => {
                console.error("Error during initial tile update: ", err);
            });

            map.on("moveend", updateVisibleTiles);
            map.on("zoomend", updateVisibleTiles);
        });

        return () => {
            map.off("moveend", updateVisibleTiles);
            map.off("zoomend", updateVisibleTiles);
        };
    }, [map, world]);

    useEffect(() => {
        setTiles([]);
        map.setView([0, 0], 0);
    }, [map, world]);

    return (
        <>
            {tiles.map(({ x, z }) => {
                const key = `${world}_${x}_${z}`;
                const isUpdated = updatedTiles.has(key);
                const versionParam = isUpdated ? `&v=${Date.now()}_${Math.random().toString(36).substring(2, 6)}` : "";

                if (isUpdated) {
                    updatedTiles.delete(key);
                    console.log("Setting v param", versionParam);
                }
                return (
                    <MemoTile key={`${world}_${x}_${z}_${versionParam}`} world={world} x={x} z={z} versionParam={versionParam} />
                );
            })}
        </>
    );
}
export default VisibleTileLoader;