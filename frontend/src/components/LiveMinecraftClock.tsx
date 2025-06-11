import {getDayNightIcon, formatMinecraftTime} from "../utils/MinecraftUtils.ts";
import {FaCloudSun, FaCloudSunRain, FaMoon, FaSun} from "react-icons/fa";

type LiveMinecraftClockProps = {
    worldTime: number;
}

const LiveMinecraftClock = ( { worldTime }: LiveMinecraftClockProps ) => {
    const timeString = formatMinecraftTime(worldTime);

    const iconMap = {
        day: <FaSun size={10} color="gold" />,
        night: <FaMoon color="#ccc" />,
        sunrise: <FaCloudSun color="#FFA500" />,
        sunset: <FaCloudSunRain color="#FF8C00" />,
    };
    const icon = iconMap[getDayNightIcon(worldTime)];

    return (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "right", gap: 4}}>
            {icon}
            <span>{timeString}</span>
        </div>
    )
}

export default LiveMinecraftClock