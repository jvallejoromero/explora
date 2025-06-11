import {useMapEvents} from "react-leaflet";
import {useState} from 'react';

export function useMouseCoords() {
    const [coords, setCoords] = useState< {x: number, z: number} | null>(null);

    useMapEvents({
        mousemove(e) {
            const latLng = e.latlng;
            const pixelX = latLng.lng;
            const pixelZ = latLng.lat;

            setCoords({x: pixelX, z: pixelZ});
        },
        mouseout() {
            setCoords(null);
        },
    });

    return {mouseCoords: coords}
}