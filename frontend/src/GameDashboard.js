import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './GameDashboard.css';

function GameDashboard({ userId, username, token, onLogout }) {
    const [gameState, setGameState] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [characterName, setCharacterName] = useState('');
    const [showStartGame, setShowStartGame] = useState(false);
    const [inCombat, setInCombat] = useState(false);
    const [combatLog, setCombatLog] = useState([]);
    const [recentCombatLog, setRecentCombatLog] = useState([]);

    const getAuthHeaders = () => ({
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    useEffect(() => {
        fetchGameState();
    }, [userId]);

    const fetchGameState = async () => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.get('http://localhost:8080/api/game/state', getAuthHeaders());
            setGameState(res.data);
            setShowStartGame(false);
            detectCombat(res.data);
        } catch (err) {
            if (err.response?.status === 401) {
                setError('Session expired. Please login again.');
                setTimeout(() => onLogout(), 2000);
            } else if (err.response?.data?.error?.includes('not found')) {
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
            if (state?.combatState?.combatLog && state.combatState.combatLog.length > 0) {
                setRecentCombatLog(state.combatState.combatLog);
            }
        }
    };


    const startNewGame = async (e) => {
        e.preventDefault();
        if (!characterName.trim()) return;

        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/start', {
                characterName: characterName.trim()
            }, getAuthHeaders());
            setGameState(res.data);
            setShowStartGame(false);
            setCharacterName('');
            detectCombat(res.data);
        } catch (err) {
            if (err.response?.status === 401) {
                setError('Session expired. Please login again.');
                setTimeout(() => onLogout(), 2000);
            } else {
                setError(err.response?.data?.error || 'Failed to start game');
            }
        }
        setLoading(false);
    };


    const resetGame = async () => {
        if (!window.confirm('Are you sure you want to start a new game? This will delete your current progress.')) return;

        setLoading(true);
        setError('');
        try {
            await axios.delete('http://localhost:8080/api/game/reset', getAuthHeaders());
            setShowStartGame(true);
            setGameState(null);
        } catch (err) {
            if (err.response?.status === 401) {
                setError('Session expired. Please login again.');
                setTimeout(() => onLogout(), 2000);
            } else {
                setError(err.response?.data?.error || 'Failed to reset game');
            }
        }
        setLoading(false);
    };

    const makeChoice = async (choiceId) => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/choice', {
                choiceId
            }, getAuthHeaders());
            setGameState(res.data);
            detectCombat(res.data);
        } catch (err) {
            if (err.response?.status === 401) {
                setError('Session expired. Please login again.');
                setTimeout(() => onLogout(), 2000);
            } else {
                setError(err.response?.data?.error || 'Failed to process choice');
            }
        }
        setLoading(false);
    };

    const performCombatAction = async (action) => {
        setLoading(true);
        setError('');
        try {
            const res = await axios.post('http://localhost:8080/api/game/combat', {
                action
            }, getAuthHeaders());

            console.log('Combat response:', res.data);

            if (res.data?.combatState?.combatLog) {
                const currentLog = res.data.combatState.combatLog;
                console.log('Combat log found:', currentLog);
                setCombatLog(currentLog);

                if (!res.data.combatState.combatActive) {
                    console.log('Combat ended, saving to recentCombatLog');
                    setRecentCombatLog(currentLog);
                }
            } else if (res.data?.combatState === null && combatLog.length > 0) {
                console.log('No combatState but we have combatLog, saving to recent');
                setRecentCombatLog(combatLog);
            }

            setGameState(res.data);

            if (res.data?.combatState) {
                setInCombat(res.data.combatState.combatActive);
            } else {
                setInCombat(false);
            }
        } catch (err) {
            if (err.response?.status === 401) {
                setError('Session expired. Please login again.');
                setTimeout(() => onLogout(), 2000);
            } else {
                setError(err.response?.data?.error || 'Failed to process combat action');
            }
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
    const { playerCharacter, currentNarrative, availableChoices, gameCompleted } = gameState;

    if (gameCompleted) {
        const enemiesDefeated = Object.keys(playerCharacter.flags || {})
            .filter(flag => flag.startsWith('defeated_'))
            .length;

        return (
            <div className="dashboard-container">
                <div className="victory-card">
                    <div className="victory-header">
                        <h1 className="victory-title">VICTORY!</h1>
                        <p className="victory-subtitle">You have defeated the Ancient Dragon!</p>
                    </div>

                    <div className="victory-content">
                        <div className="victory-message">
                            <p>The Ancient Dragon lets out a final roar before collapsing. Its scales shimmer one last time in the fading light. You stand victorious at the peak, having conquered the greatest challenge in the land.</p>
                            <p>The villages below celebrate your heroic deeds. Songs will be sung of your bravery for generations to come!</p>
                        </div>

                        <div className="victory-stats">
                            <h2>Final Statistics</h2>
                            <div className="stats-grid">
                                <div className="stat-card">
                                    <span className="stat-label">Final Level</span>
                                    <span className="stat-value">{playerCharacter.level}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label">Total Experience</span>
                                    <span className="stat-value">{playerCharacter.experience}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label">Enemies Defeated</span>
                                    <span className="stat-value">{enemiesDefeated}</span>
                                </div>
                                <div className="stat-card">
                                    <span className="stat-label">Items Collected</span>
                                    <span className="stat-value">{playerCharacter.inventory?.length || 0}</span>
                                </div>
                            </div>
                        </div>

                        <div className="victory-hero">
                            <h2>The Hero: {playerCharacter.name}</h2>
                            <div className="hero-final-stats">
                                <div className="hero-stat">
                                    <span className="hero-stat-label">Attack Power</span>
                                    <span className="hero-stat-value">{playerCharacter.baseDamage}</span>
                                </div>
                                <div className="hero-stat">
                                    <span className="hero-stat-label">Max Health</span>
                                    <span className="hero-stat-value">{playerCharacter.baseHealth}</span>
                                </div>
                            </div>
                        </div>

                        {playerCharacter.inventory && playerCharacter.inventory.length > 0 && (
                            <div className="victory-inventory">
                                <h2>Final Inventory</h2>
                                <div className="inventory-grid">
                                    {playerCharacter.inventory.map((item, index) => (
                                        <div key={index} className="inventory-item">
                                            <span className="item-name">{item.name}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="victory-actions">
                            <button onClick={resetGame} className="btn-new-game" disabled={loading}>
                                Start New Adventure
                            </button>
                            <button onClick={onLogout} className="btn-logout-victory">
                                Return to Menu
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    const getXpForLevel = (level) => {
        let totalXp = 0;
        for (let i = 2; i <= level; i++) {
            totalXp += (i - 1) * 10;
        }
        return totalXp;
    };

    const currentLevelXp = getXpForLevel(playerCharacter.level);
    const nextLevelXp = getXpForLevel(playerCharacter.level + 1);
    const xpIntoLevel = playerCharacter.experience - currentLevelXp;
    const xpNeededForLevel = nextLevelXp - currentLevelXp;
    const xpProgress = (xpIntoLevel / xpNeededForLevel) * 100;

    if (inCombat && gameState?.combatState) {
        const cs = gameState.combatState;
        const playerHpPercent = (cs.playerCurrentHealth / cs.playerMaxHealth) * 100;
        const enemyHpPercent = (cs.enemyCurrentHealth / cs.enemyMaxHealth) * 100;

        return (
            <div className="dashboard-container">
                <div className="dashboard-card">
                    <div className="dashboard-header">
                        <h1>⚔️ Combat vs {cs.enemyName}</h1>
                        <button onClick={onLogout} className="btn-logout">Logout</button>
                    </div>

                    <div className="combat-status">
                        <div className="combat-hp-card">
                            <div className="hp-header">
                                <span className="hp-label">{playerCharacter.name}</span>
                                <span className="hp-value">{cs.playerCurrentHealth}/{cs.playerMaxHealth}</span>
                            </div>
                            <div className="hp-bar-container">
                                <div className="hp-bar player-hp" style={{ width: `${playerHpPercent}%` }}></div>
                            </div>
                        </div>
                        <div className="combat-hp-card">
                            <div className="hp-header">
                                <span className="hp-label">{cs.enemyName}</span>
                                <span className="hp-value">{cs.enemyCurrentHealth}/{cs.enemyMaxHealth}</span>
                            </div>
                            <div className="hp-bar-container">
                                <div className="hp-bar enemy-hp" style={{ width: `${enemyHpPercent}%` }}></div>
                            </div>
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
                    <div className="stat-card">
                        <span className="stat-label">Level</span>
                        <span className="stat-value">{playerCharacter.level}</span>
                    </div>
                    <div className="stat-card stat-card-progress">
                        <span className="stat-label">Experience</span>
                        <span className="stat-value-small">{xpIntoLevel}/{xpNeededForLevel} XP</span>
                        <div className="progress-bar-container">
                            <div className="progress-bar xp-bar" style={{ width: `${xpProgress}%` }}></div>
                        </div>
                    </div>
                    <div className="stat-card stat-card-progress">
                        <span className="stat-label">Health</span>
                        <span className="stat-value-small">{playerCharacter.currentHealth}/{playerCharacter.baseHealth}</span>
                        <div className="progress-bar-container">
                            <div className="progress-bar health-bar" style={{ width: `${(playerCharacter.currentHealth / playerCharacter.baseHealth) * 100}%` }}></div>
                        </div>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Damage</span>
                        <span className="stat-value">{playerCharacter.baseDamage}</span>
                    </div>
                </div>

                <div className="game-layout">
                    <div className="main-content">
                        <div className="story-section">
                            <h2>Current Story</h2>
                            <div className="narrative-box"><p>{currentNarrative}</p></div>
                        </div>

                {playerCharacter.inventory && playerCharacter.inventory.length > 0 && (
                    <div className="inventory-section">
                        <h2>Inventory ({playerCharacter.inventory.length} items)</h2>
                        <div className="inventory-grid">
                            {playerCharacter.inventory.map((item, index) => (
                                <div key={index} className="inventory-item">
                                    <div className="item-header">
                                        <span className="item-name">{item.name}</span>
                                        {item.goldValue && <span className="item-gold">💰 {item.goldValue}g</span>}
                                    </div>
                                    {item.type && <span className="item-type">Type: {item.type}</span>}
                                    {item.power && <span className="item-power">Power: +{item.power}</span>}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

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

                        {playerCharacter.gameHistory && playerCharacter.gameHistory.length > 0 && (
                            <div className="history-section">
                                <h2>Recent Events</h2>
                                <div className="history-box">
                                    {playerCharacter.gameHistory.slice(-5).map((event, index) => (
                                        <p key={index} className="history-item">{event}</p>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="reset-section">
                            <button onClick={resetGame} className="btn-reset" disabled={loading}>
                                Reset Game
                            </button>
                        </div>

                        {error && <div className="message message-error">{error}</div>}
                    </div>

                    {recentCombatLog.length > 0 && (
                        <div className="combat-sidebar">
                            <h3>Recent Combat</h3>
                            <div className="recent-combat-log">
                                {recentCombatLog.map((line, i) => (
                                    <p key={i} className="combat-log-entry">{line}</p>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default GameDashboard;
