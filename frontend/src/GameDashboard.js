import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './GameDashboard.css';

function GameDashboard({ userId, username, onLogout }) {
    const [gameState, setGameState] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [characterName, setCharacterName] = useState('');
    const [showStartGame, setShowStartGame] = useState(false);

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
        } catch (err) {
            if (err.response?.data?.error?.includes('not found')) {
                setShowStartGame(true);
            } else {
                setError(err.response?.data?.error || 'Failed to load game state');
            }
        }
        setLoading(false);
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
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to start game');
        }
        setLoading(false);
    };

    const resetGame = async () => {
        if (!window.confirm('Are you sure you want to start a new game? This will delete your current progress.')) {
            return;
        }

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
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to process choice');
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
                        <p>Create your character to begin your journey in TextVentures.</p>

                        <form onSubmit={startNewGame} className="start-game-form">
                            <div className="form-group">
                                <label htmlFor="characterName">Character Name</label>
                                <input
                                    id="characterName"
                                    type="text"
                                    className="form-input"
                                    value={characterName}
                                    onChange={(e) => setCharacterName(e.target.value)}
                                    placeholder="Enter your character's name"
                                    required
                                    disabled={loading}
                                />
                            </div>
                            <button type="submit" className="btn-primary" disabled={loading}>
                                {loading ? 'Starting...' : 'Start Adventure'}
                            </button>
                        </form>

                        {error && <div className="message message-error">{error}</div>}
                    </div>
                </div>
            </div>
        );
    }

    if (loading && !gameState) {
        return (
            <div className="dashboard-container">
                <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Loading...</p>
                </div>
            </div>
        );
    }

    if (!gameState) {
        return null;
    }

    const { playerCharacter, currentNarrative, availableChoices } = gameState;

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
                    <div className="stat-card">
                        <span className="stat-label">Experience</span>
                        <span className="stat-value">{playerCharacter.experience}</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Health</span>
                        <span className="stat-value">{playerCharacter.baseHealth}</span>
                    </div>
                    <div className="stat-card">
                        <span className="stat-label">Damage</span>
                        <span className="stat-value">{playerCharacter.baseDamage}</span>
                    </div>
                </div>

                <div className="story-section">
                    <h2>Current Story</h2>
                    <div className="narrative-box">
                        <p>{currentNarrative}</p>
                    </div>
                </div>

                <div className="choices-section">
                    <h2>What will you do?</h2>
                    <div className="choices-grid">
                        {availableChoices && availableChoices.length > 0 ? (
                            availableChoices.map((choice) => (
                                <button
                                    key={choice.id}
                                    onClick={() => makeChoice(choice.id)}
                                    className="choice-button"
                                    disabled={loading}
                                >
                                    <span className="choice-text">{choice.text}</span>
                                    <span className="choice-arrow">→</span>
                                </button>
                            ))
                        ) : (
                            <p className="no-choices">No actions available at the moment.</p>
                        )}
                    </div>
                </div>

                {playerCharacter.gameHistory && playerCharacter.gameHistory.length > 0 && (
                    <div className="history-section">
                        <h2>Story History</h2>
                        <div className="history-list">
                            {playerCharacter.gameHistory.map((entry, index) => (
                                <div key={index} className="history-item">
                                    <span className="history-number">{index + 1}</span>
                                    <span className="history-text">{entry}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {error && <div className="message message-error">{error}</div>}
            </div>
        </div>
    );
}

export default GameDashboard;

