import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { loginUser, logoutUser, registerUser } from '../api/auth';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../api/client';

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const raw = localStorage.getItem('user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

// Check if JWT token is expired (without verifying signature)
function isTokenExpired(token) {
  if (!token) return true;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  // On mount, validate stored tokens and user
  useEffect(() => {
    const storedUser = readStoredUser();
    const accessToken = getAccessToken();
    const refreshToken = getRefreshToken();

    if (storedUser && accessToken && refreshToken) {
      // If access token is expired, we could trigger a refresh here
      // For now, just trust the stored user if tokens exist
      // The API interceptor will handle 401 and attempt refresh
      setUser(storedUser);
    }
    setReady(true);
  }, []);

  const persistSession = (auth) => {
    setTokens({ accessToken: auth.accessToken, refreshToken: auth.refreshToken });
    const nextUser = {
      id: auth.userId,
      email: auth.email,
      fullName: auth.fullName,
      role: auth.role,
    };
    localStorage.setItem('user', JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  };

  const login = useCallback(async (email, password) => {
    const auth = await loginUser({ email, password });
    return persistSession(auth);
  }, []);

  const register = useCallback(async (payload) => {
    const auth = await registerUser(payload);
    return persistSession(auth);
  }, []);

  const logout = useCallback(async () => {
    try {
      if (getAccessToken()) await logoutUser();
    } catch {
      // best-effort — clear local session regardless
    }
    clearTokens();
    setUser(null);
  }, []);

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin: user?.role === 'ADMIN',
    ready,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
