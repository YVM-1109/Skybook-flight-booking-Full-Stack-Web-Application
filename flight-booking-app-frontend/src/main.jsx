import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { BookingDraftProvider } from './context/BookingDraftContext.jsx';
import './styles/global.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <BookingDraftProvider>
          <App />
        </BookingDraftProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
