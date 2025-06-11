import {useEffect, useState} from "react";
import "../css/nether-portal-transition.css"

const NetherPortalTransition = ({ onComplete } : { onComplete: () => void}) => {
    const [visible, setVisible] = useState(true);

    useEffect(() => {
        const timeout = setTimeout(() => {
            setVisible(false);
            onComplete();
        }, 400);

        return () => clearTimeout(timeout);
    }, [onComplete])

    if (!visible) return null;
    return (
        <div className="nether-portal-overlay">
            <video
                className="nether-portal-video"
                src="../nether-portal.mp4"
                autoPlay
                muted
                loop
                playsInline
            />
        </div>
    )
}

export default NetherPortalTransition