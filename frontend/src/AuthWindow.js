import React, { useState } from 'react';
import axios from 'axios';
import './AuthWindow.css';

function AuthWindow({ onLoginSuccess }) {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState('player');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');

        try {
            const endpoint = isLogin ? 'http://localhost:8080/api/login' : 'http://localhost:8080/api/register';
            const payload = isLogin ? { username, password } : { username, password, role };

            const res = await axios.post(endpoint, payload);

            if (isLogin) {
                setMessage(`Login successful! Welcome, ${res.data.username}!`);
                setTimeout(() => {
                    onLoginSuccess({
                        userId: res.data.id,
                        username: res.data.username,
                        role: res.data.role
                    });
                }, 500);
            } else {
                setMessage(`Registration successful! User ID: ${res.data.id}`);
                setTimeout(() => {
                    setIsLogin(true);
                    setMessage('');
                    setPassword('');
                }, 2000);
            }
        } catch (err) {
            const errorMsg = err.response?.data?.error || err.response?.data?.message || err.message;
            setMessage(`Error: ${errorMsg}`);
        }

        setLoading(false);
    };

    const toggleMode = () => {
        setIsLogin(!isLogin);
        setMessage('');
        setUsername('');
        setPassword('');
        setRole('player');
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <h1 className="auth-title">TextVentures</h1>
                    <p className="auth-subtitle">{isLogin ? 'Welcome back!' : 'Create your account'}</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label htmlFor="username" className="form-label">Username</label>
                        <input
                            id="username"
                            type="text"
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Enter your username"
                            required
                            disabled={loading}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password" className="form-label">Password</label>
                        <input
                            id="password"
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter your password"
                            required
                            disabled={loading}
                        />
                    </div>

                    {!isLogin && (
                        <div className="form-group">
                            <label htmlFor="role" className="form-label">Role</label>
                            <select
                                id="role"
                                className="form-select"
                                value={role}
                                onChange={(e) => setRole(e.target.value)}
                                disabled={loading}
                            >
                                <option value="player">Player</option>
                                <option value="admin">Admin</option>
                            </select>
                        </div>
                    )}

                    <button
                        type="submit"
                        className={`btn-primary ${loading ? 'btn-loading' : ''}`}
                        disabled={loading}
                    >
                        {loading ? (
                            <span className="btn-spinner">
                <span className="spinner"></span>
                                {isLogin ? 'Logging in...' : 'Registering...'}
              </span>
                        ) : (
                            isLogin ? 'Login' : 'Register'
                        )}
                    </button>
                </form>

                {message && (
                    <div className={`message ${message.startsWith('Error') ? 'message-error' : 'message-success'}`}>
                        {message}
                    </div>
                )}

                <div className="auth-footer">
                    <p className="toggle-text">
                        {isLogin ? "Don't have an account?" : "Already have an account?"}
                    </p>
                    <button
                        type="button"
                        className="btn-secondary"
                        onClick={toggleMode}
                        disabled={loading}
                    >
                        {isLogin ? 'Create Account' : 'Login'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default AuthWindow;

