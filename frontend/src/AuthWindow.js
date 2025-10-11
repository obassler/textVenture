import React, { useState } from 'react';
import axios from 'axios';

function AuthWindow() {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      if (isLogin) {
        // Přihlášení
        const res = await axios.post('/api/login', { username, password });
        setMessage('Přihlášení úspěšné!');
      } else {
        // Registrace
        const res = await axios.post('/api/register', { username, password });
        setMessage('Registrace úspěšná!');
      }
    } catch (err) {
      setMessage('Chyba: ' + (err.response?.data?.message || err.message));
    }
    setLoading(false);
  };

  return (
    <div style={{ padding: 20, maxWidth: 400, margin: 'auto' }}>
      <h2>{isLogin ? 'Přihlášení' : 'Registrace'}</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Uživatelské jméno:</label><br />
          <input type="text" value={username} onChange={e => setUsername(e.target.value)} required />
        </div>
        <div style={{ marginTop: 10 }}>
          <label>Heslo:</label><br />
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
        </div>
        <button type="submit" disabled={loading} style={{ marginTop: 20 }}>
          {isLogin ? 'Přihlásit se' : 'Registrovat'}
        </button>
      </form>
      <button onClick={() => setIsLogin(!isLogin)} style={{ marginTop: 10 }}>
        {isLogin ? 'Nemáte účet? Registrovat' : 'Máte účet? Přihlásit se'}
      </button>
      {message && <p style={{ color: message.startsWith('Chyba') ? 'red' : 'green' }}>{message}</p>}
    </div>
  );
}

export default AuthWindow;

