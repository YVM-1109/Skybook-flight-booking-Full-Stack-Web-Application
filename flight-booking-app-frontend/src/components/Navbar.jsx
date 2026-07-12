import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <NavLink to="/" className="brand">
          <span className="brand-mark">✈</span>
          SkyBook
        </NavLink>

        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Search flights
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/bookings" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              My bookings
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/admin" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              Admin
            </NavLink>
          )}

          {isAuthenticated ? (
            <>
              <span className="nav-link" style={{ color: 'rgba(244,247,255,0.5)', cursor: 'default' }}>
                {user.fullName?.split(' ')[0]}
              </span>
              <button onClick={handleLogout} className="nav-link" style={{ background: 'none', border: 'none' }}>
                Log out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
                Log in
              </NavLink>
              <NavLink to="/register" className="nav-link nav-cta">
                Sign up
              </NavLink>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
