import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(form.email, form.password);
      const dest = location.state?.from?.pathname || '/';
      navigate(dest, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <form className="card auth-card" onSubmit={submit}>
        <h1>Welcome back</h1>
        <p className="sub">Log in to book flights and manage your trips.</p>

        {error && <div className="alert alert-danger" style={{ marginBottom: 16 }}>{error}</div>}

        <div className="stack" style={{ gap: 14 }}>
          <div className="field">
            <label>Email</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required autoFocus />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          </div>
        </div>

        <button type="submit" className="btn btn-primary btn-block" style={{ marginTop: 20 }} disabled={submitting}>
          {submitting ? 'Logging in…' : 'Log in'}
        </button>

        <p className="dim" style={{ fontSize: 13, marginTop: 18, textAlign: 'center' }}>
          Don&apos;t have an account? <Link to="/register" style={{ color: 'var(--blue-600)', fontWeight: 700 }}>Sign up</Link>
        </p>
        <p className="dim" style={{ fontSize: 11.5, marginTop: 10, textAlign: 'center' }}>
          Admin demo login: admin@flightbooking.com / Admin@123
        </p>
      </form>
    </div>
  );
}
