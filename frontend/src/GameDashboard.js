import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './GameDashboard.css';

function GameDashboard({ userId, username, onLogout }) {
    const [gameState, setGameState] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [characterName, setCharacterName] = useState('');
    const [showStartGame, setShowStartGame] = useState(false);
    const [inCombat, setInCombat] = useState(false);
    const [combatLog, setCombatLog] = useState([]);

    useEffect(() => {
        fetchGameState();
    }, [userId]);

    const fetchGameState = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.get(`http://localhost:8080/api/game/state?userId=${userId}`);
            setGameState(res.data);
            setShowStartGame(false);
            detectCombat(res.data);
        } catch (err) {
            if (err.response?.data?.error?.includes('not found')) {
                setShowStartGame(true);
            } else {
                setError(err.response?.data?.error || 'Failed to load game state');
            }
        }
        setLoading(false);
    };

    const detectCombat = (state) => {
        if (state?.combatState?.combatActive) {
            setInCombat(true);
            setCombatLog(state.combatState.combatLog || []);
        } else {
            setInCombat(false);
        }
    };


    const startNewGame = async (e) => {
        e.preventDefault();
        if (!characterName.trim()) return;

        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/start', {
                userId,
                characterName: characterName.trim()
            });
            setGameState(res.data);
            setShowStartGame(false);
            setCharacterName('');
            detectCombat(res.data);
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to start game');
        }
        setLoading(false);
    };


    const resetGame = async () => {
        if (!window.confirm('Are you sure you want to start a new game? This will delete your current progress.')) return;

        setLoading(true);
        setError('');
        try {
            await axios.delete(`http://localhost:8080/api/game/reset?userId=${userId}`);
            setShowStartGame(true);
            setGameState(null);
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to reset game');
        }
        setLoading(false);
    };

    const makeChoice = async (choiceId) => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/choice', {
                userId,
                choiceId
            });
            setGameState(res.data);
            detectCombat(res.data);
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to process choice');
        }
        setLoading(false);
    };

    const performCombatAction = async (action) => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/combat', {
                userId,
                action
            });
            setGameState(res.data);

            // Update combat log with the latest entries
            if (res.data?.combatState?.combatLog) {
                setCombatLog(res.data.combatState.combatLog);
            }

            // Update combat status
            if (res.data?.combatState) {
                setInCombat(res.data.combatState.combatActive);
            }
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to process combat action');
        }
        setLoading(false);
    };


    if (showStartGame) {
        return (
            <div className="dashboard-container">
                <div className="dashboard-card">
                    <div className="dashboard-header">
                        <h1>Welcome, {username}!</h1>
                        <button onClick={onLogout} className="btn-logout">Logout</button>
                    </div>
                    <div className="start-game-section">
                        <h2>Start Your Adventure</h2>
                        <form onSubmit={startNewGame} className="start-game-form">
                            <input
                                type="text"
                                value={characterName}
                                onChange={(e) => setCharacterName(e.target.value)}
                                placeholder="Enter your character's name"
                                required
                            />
                            <button type="submit" disabled={loading}>
                                {loading ? 'Starting...' : 'Start Adventure'}
                            </button>
                        </form>
                        {error && <div className="message message-error">{error}</div>}
                    </div>
                </div>
            </div>
        );
    }

    if (!gameState) return null;
    const { playerCharacter, currentNarrative, availableChoices } = gameState;

    // --- COMBAT MODE ---
    if (inCombat && gameState?.combatState) {
        const cs = gameState.combatState;

        return (
            <div className="dashboard-container">
                <div className="dashboard-card">
                    <div className="dashboard-header">
                        <h1>⚔️ Combat vs {cs.enemyName}</h1>
                        <button onClick={onLogout} className="btn-logout">Logout</button>
                    </div>

                    <div className="combat-status">
                        <div className="stat-card">
                            <span className="stat-label">Player HP</span>
                            <span className="stat-value">{cs.playerCurrentHealth}/{cs.playerMaxHealth}</span>
                        </div>
                        <div className="stat-card">
                            <span className="stat-label">{cs.enemyName}</span>
                            <span className="stat-value">{cs.enemyCurrentHealth}/{cs.enemyMaxHealth}</span>
                        </div>
                    </div>

                    <div className="combat-log">
                        {combatLog.length > 0 ? (
                            combatLog.map((line, i) => (
                                <p key={i}>{line}</p>
                            ))
                        ) : (
                            cs.combatLog && cs.combatLog.length > 0 ? (
                                cs.combatLog.map((line, i) => (
                                    <p key={i}>{line}</p>
                                ))
                            ) : (
                                <p>Combat has begun...</p>
                            )
                        )}
                    </div>

                    <div className="combat-actions">
                        <button onClick={() => performCombatAction('ATTACK')} disabled={loading}>⚔️ Attack</button>
                        <button onClick={() => performCombatAction('DEFEND')} disabled={loading}>🛡️ Defend</button>
                        <button onClick={() => performCombatAction('USE_ITEM')} disabled={loading}>🧪 Use Item</button>
                        <button onClick={() => performCombatAction('FLEE')} disabled={loading}>🏃‍♂️ Flee</button>
                    </div>

                    {error && <div className="message message-error">{error}</div>}
                </div>
            </div>
        );
    }


    // --- NORMAL STORY MODE ---
    return (
        <div className="dashboard-container">
            <div className="dashboard-card">
                <div className="dashboard-header">
                    <div className="header-info">
                        <h1>{playerCharacter.name}</h1>
                        <p className="username-tag">@{username}</p>
                    </div>
                    <button onClick={onLogout} className="btn-logout">Logout</button>
                </div>

                <div className="stats-grid">
                    <div className="stat-card"><span className="stat-label">Level</span><span className="stat-value">{playerCharacter.level}</span></div>
                    <div className="stat-card"><span className="stat-label">Health</span><span className="stat-value">{playerCharacter.baseHealth}</span></div>
                    <div className="stat-card"><span className="stat-label">Damage</span><span className="stat-value">{playerCharacter.baseDamage}</span></div>
                </div>

                <div className="story-section">
                    <h2>Current Story</h2>
                    <div className="narrative-box"><p>{currentNarrative}</p></div>
                </div>

                <div className="choices-section">
                    <h2>What will you do?</h2>
                    <div className="choices-grid">
                        {availableChoices?.length ? (
                            availableChoices.map((choice) => (
                                <button key={choice.id} onClick={() => makeChoice(choice.id)} disabled={loading}>
                                    {choice.text}
                                </button>
                            ))
                        ) : (
                            <p>No actions available at the moment.</p>
                        )}
                    </div>
                </div>

                {error && <div className="message message-error">{error}</div>}
            </div>
        </div>
    );
}

export default GameDashboard;
