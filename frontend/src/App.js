import React, { useState, useEffect } from 'react';
import './App.css';
import AuthWindow from './AuthWindow';
import GameDashboard from './GameDashboard';

function App() {
    const [user, setUser] = useState(null);

    useEffect(() => {
        const token = localStorage.getItem('token');
        const userId = localStorage.getItem('userId');
        const username = localStorage.getItem('username');

        if (token && userId && username) {
            setUser({ userId, username, token });
        }
    }, []);

    const handleLoginSuccess = (userData) => {
        setUser(userData);
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        setUser(null);
    };

    return (
        <div className="App">
            {user ? (
                <GameDashboard
                    userId={user.userId}
                    username={user.username}
                    token={user.token}
                    onLogout={handleLogout}
                />
            ) : (
                <div className="App-header">
                    <AuthWindow onLoginSuccess={handleLoginSuccess} />
                </div>
            )}
        </div>
    );
}

export default App;

