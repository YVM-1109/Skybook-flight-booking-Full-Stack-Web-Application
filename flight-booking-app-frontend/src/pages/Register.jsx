import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setSubmitting(true);
    try {
      await register(form);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <form className="card auth-card" onSubmit={submit}>
        <h1>Create your account</h1>
        <p className="sub">Takes less than a minute.</p>

        {error && <div className="alert alert-danger" style={{ marginBottom: 16 }}>{error}</div>}

        <div className="stack" style={{ gap: 14 }}>
          <div className="field">
            <label>Full name</label>
            <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required autoFocus />
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </div>
          <div className="field">
            <label>Phone (optional)</label>
            <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} />
          </div>
        </div>

        <button type="submit" className="btn btn-primary btn-block" style={{ marginTop: 20 }} disabled={submitting}>
          {submitting ? 'Creating account…' : 'Sign up'}
        </button>

        <p className="dim" style={{ fontSize: 13, marginTop: 18, textAlign: 'center' }}>
          Already have an account? <Link to="/login" style={{ color: 'var(--blue-600)', fontWeight: 700 }}>Log in</Link>
        </p>
      </form>
    </div>
  );
}
