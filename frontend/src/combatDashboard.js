import { useState, useEffect } from "react";

export default function CombatWindow({ userId }) {
    const [combatLog, setCombatLog] = useState([]);
    const [player, setPlayer] = useState({});
    const [enemy, setEnemy] = useState({});
    const [loading, setLoading] = useState(false);
    const [combatOver, setCombatOver] = useState(false);

    // Example combat action handler
    const handleAction = async (action) => {
        setLoading(true);
        try {
            const response = await fetch("/api/game/combat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ userId, action }),
            });
            const data = await response.json();

            setCombatLog((prev) => [...prev, data.currentNarrative]);
            setPlayer(data.playerCharacter);
            if (!data.playerCharacter) setCombatOver(true);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="p-4 bg-gray-900 text-white rounded-2xl shadow-xl max-w-lg mx-auto">
            <h2 className="text-2xl font-bold mb-2">⚔️ Combat</h2>

            <div className="flex justify-between mb-4">
                <div>
                    <h3 className="text-xl">{enemy.name}</h3>
                    <p>HP: {enemy.currentHealth}/{enemy.maxHealth}</p>
                </div>
                <div>
                    <h3 className="text-xl">{player.name}</h3>
                    <p>HP: {player.currentHealth}/{player.maxHealth}</p>
                </div>
            </div>

            <div className="border border-gray-700 p-3 rounded mb-3 h-40 overflow-y-auto bg-gray-800">
                {combatLog.map((line, i) => (
                    <p key={i}>{line}</p>
                ))}
            </div>

            {!combatOver && (
                <div className="flex gap-2">
                    <button onClick={() => handleAction("ATTACK")} disabled={loading} className="bg-red-600 px-4 py-2 rounded">
                        Attack
                    </button>
                    <button onClick={() => handleAction("DEFEND")} disabled={loading} className="bg-blue-600 px-4 py-2 rounded">
                        Defend
                    </button>
                    <button onClick={() => handleAction("USE_ITEM")} disabled={loading} className="bg-green-600 px-4 py-2 rounded">
                        Use Item
                    </button>
                    <button onClick={() => handleAction("FLEE")} disabled={loading} className="bg-gray-600 px-4 py-2 rounded">
                        Flee
                    </button>
                </div>
            )}

            {combatOver && (
                <p className="mt-3 text-yellow-400 font-semibold">Combat Over!</p>
            )}
        </div>
    );
}
