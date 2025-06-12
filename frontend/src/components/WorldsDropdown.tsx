import React, { useEffect, useState, useRef } from 'react';
import "../css/worlds-dropdown.css";
import {useServerStatus} from "../hooks/ServerStatus.ts";

const apiKey = import.meta.env.VITE_BACKEND_API_KEY;
const baseUrl = import.meta.env.VITE_BACKEND_BASE_URL;

type WorldDropdownProps = {
  onSelectWorld: (world: string) => void;
  disabled: boolean;
};

const worldNameMap: Record<string, string> = {
  world: "Overworld",
  world_nether: "The Nether",
  world_the_end: "The End",
};

const WorldDropdown: React.FC<WorldDropdownProps> = ({ onSelectWorld, disabled }) => {
  const { connectedToBackend } = useServerStatus();

  const savedView = localStorage.getItem(`savedMapView`);
  let selectedWorldName = worldNameMap["world"];

  if (savedView) {
    selectedWorldName = worldNameMap[JSON.parse(savedView).world];
  }

  const [worlds, setWorlds] = useState<string[]>([]);
  const [selectedWorld, setSelectedWorld] = useState<string>(selectedWorldName);
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const dropdownRef = useRef<HTMLDivElement>(null);
  const displayName = worldNameMap[selectedWorld] ?? selectedWorld;

  useEffect(() => {
    if (disabled) {
      setWorlds(["None available"]);
      return;
    }

    // Fetch worlds from backend
    fetch(baseUrl + `/tiles/available-worlds?apiKey=${apiKey}`)
      .then(res => res.json())
      .then(data => {
        const parsedWorlds = data.worlds.map((world: string) => {
          return worldNameMap[world] ?? world;
        });
        setWorlds(parsedWorlds || []);
      })
      .catch((err) => {
        console.error('Failed to fetch worlds:', err)
      });
  }, [connectedToBackend]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (world: string) => {
    if (disabled) return;
    setSelectedWorld(world);
    setIsOpen(false);
    onSelectWorld(world);
  };

  return (
      <div ref={dropdownRef} style={{ position: "relative" }}>
        <h2 style={styles.worldTitle} onClick={() => setIsOpen((prev) => !prev)}>
          {worldNameMap[displayName] ?? displayName}
        </h2>

        {isOpen && (
            <ul style={styles.dropdownList}>
              {worlds.map((world) => (
                  <li
                      key={world}
                      onClick={() => handleSelect(world)}
                      className="dropdown-item"
                  >
                    {worldNameMap[world] ?? world}
                  </li>
              ))}
            </ul>
        )}
      </div>
  );
};

export default WorldDropdown;

const styles = {
  worldTitle: {
    color: "white",
    fontFamily: "Inter, sans-serif",
    fontSize: 30,
    lineHeight: "30px",
    margin: 0,
    cursor: "pointer",
    userSelect: "none",
  } as React.CSSProperties,
  dropdownList: {
    position: "absolute",
    backgroundColor: "#1e1e1e",
    border: "1px solid #333",
    borderRadius: 6,
    marginTop: 8,
    padding: 0,
    listStyle: "none",
    width: 200,
    zIndex: 99999,
  } as React.CSSProperties,
  dropdownItem: {
    padding: "10px 14px",
    color: "white",
    cursor: "pointer",
    fontSize: 16,
    fontFamily: "Inter, sans-serif",
  } as React.CSSProperties,
};