import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="container page-section" style={{ textAlign: 'center', padding: '100px 0' }}>
      <h1 style={{ fontSize: 64, marginBottom: 8 }}>404</h1>
      <p className="muted" style={{ marginBottom: 24 }}>This page took a wrong turn somewhere over the Atlantic.</p>
      <Link to="/" className="btn btn-primary">Back home</Link>
    </div>
  );
}
