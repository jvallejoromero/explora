import {useMap} from "react-leaflet";
import {useEffect} from "react";

type SavedMapViewProps = {
    world: string
}

const SavedMapView = (world: SavedMapViewProps) => {
    const map = useMap();

    useEffect(() => {
        map.whenReady(() => {
            const saved = localStorage.getItem(`savedMapView`);
            if (saved) {
                try {
                    const { world, lat, lng, zoom } = JSON.parse(saved);
                    console.log(`Restoring view for ${world}:`, lat, lng, zoom);
                    let newZoom = zoom;
                    if (zoom > -1) {
                        newZoom = -1;
                    }
                    map.setView([lat, lng], newZoom);
                } catch (err) {
                    console.warn("Invalid saved map view format:", err);
                }
            } else {
                console.log("NOT SAVED");
            }
        });
    }, []);

    useEffect(() => {
        const savedMapView = () => {
            const center = map.getCenter();
            const zoom = map.getZoom();
            localStorage.setItem(`savedMapView`, JSON.stringify({
                world: world.world,
                lat: center.lat,
                lng: center.lng,
                zoom,
            }));
        };

        window.addEventListener("beforeunload", savedMapView);
        return () => window.removeEventListener("beforeunload", savedMapView);
    }, [world]);

    return null;
}

export default SavedMapView