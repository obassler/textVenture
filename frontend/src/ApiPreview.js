import React, { useState } from 'react';
import axios from 'axios';
import { initializeApp } from 'firebase/app';
import { getAnalytics } from 'firebase/analytics';
import { getFirestore, collection, getDocs } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: "AIzaSyB4dGUaQlbG7fZ0hDUUyC-iwcqW4Krt2wc",
  authDomain: "textventure-1bb77.firebaseapp.com",
  projectId: "textventure-1bb77",
  storageBucket: "textventure-1bb77.firebasestorage.app",
  messagingSenderId: "893714821443",
  appId: "1:893714821443:web:54cadfc69d51e1eb927ff6",
  measurementId: "G-0X5W3GB2EW"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
const db = getFirestore(app);

function ApiPreview() {
  const [backendData, setBackendData] = useState(null);
  const [firebaseData, setFirebaseData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchBackend = async () => {
    setLoading(true);
    setError(null);
    try {
      // Změňte URL podle vašeho backendu
      const res = await axios.get('/api/game');
      setBackendData(res.data);
    } catch (err) {
      setError('Chyba při načítání z backendu');
    }
    setLoading(false);
  };

  const fetchFirebase = async () => {
    setLoading(true);
    setError(null);
    try {
      // Změňte kolekci podle vaší struktury
      const querySnapshot = await getDocs(collection(db, 'games'));
      const data = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setFirebaseData(data);
    } catch (err) {
      setError('Chyba při načítání z Firebase');
    }
    setLoading(false);
  };

  return (
    <div style={{ padding: 20 }}>
      <h2>API Preview</h2>
      <button onClick={fetchBackend} disabled={loading}>Načíst data z backendu</button>
      <button onClick={fetchFirebase} disabled={loading} style={{ marginLeft: 10 }}>Načíst data z Firebase</button>
      {loading && <p>Načítám...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {backendData && (
        <div>
          <h3>Backend data:</h3>
          <pre>{JSON.stringify(backendData, null, 2)}</pre>
        </div>
      )}
      {firebaseData.length > 0 && (
        <div>
          <h3>Firebase data:</h3>
          <pre>{JSON.stringify(firebaseData, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}

export default ApiPreview;
