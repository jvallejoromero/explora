const TILE_SIZE = import.meta.env.VITE_TILE_SIZE;

export function pixelsToMinecraftCoords(pixelX: number, pixelZ: number): {x: number, z: number} {
    // Each tile is 1024px = 512 blocks (16 blocks per chunk × 32 chunks)
    // So: 1 block = 2 pixels
    const minecraftX = Math.floor(pixelX / 2);
    const minecraftZ = -Math.floor(pixelZ / 2);

    return {x: minecraftX, z: minecraftZ}
}

export function minecraftCoordsToPixels(minecraftX: number, minecraftZ: number) : {x: number, z: number} {
    const pixelsPerBlock = TILE_SIZE / 512; // = 2 pixels per block
    const pixelX = Math.floor(minecraftX * pixelsPerBlock);
    const pixelZ = -Math.floor(minecraftZ * pixelsPerBlock);
    return { x: pixelX, z: pixelZ };
}

export function formatMinecraftTime(ticks: number): string {
    // Offset by 6000 ticks because 0 ticks = 6 AM
    const totalMinutes = Math.floor((ticks + 6000) % 24000 * 1440 / 24000);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
}

export function formatMinecraftTime12h(ticks: number): string {
    const totalMinutes = Math.floor((ticks + 6000) % 24000 * 1440 / 24000);
    let hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    const suffix = hours >= 12 ? "PM" : "AM";

    hours = hours % 12;
    if (hours === 0) hours = 12;

    return `${hours}:${String(minutes).padStart(2, "0")} ${suffix}`;
}

export type TimeIcon = "day" | "night" | "sunrise" | "sunset";
export function getDayNightIcon(ticks: number): TimeIcon {
    if (ticks >= 23000 || ticks < 1000) return "sunrise";
    if (ticks > 1000 && ticks < 12000) return "day";
    if (ticks >= 12000 && ticks < 14000) return "sunset";
    return "night";
}