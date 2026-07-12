import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyBookings } from '../api/bookings';
import { EmptyState, PageLoader } from '../components/Ui';
import { formatDate, formatMoney, statusBadgeClass } from '../utils/format';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyBookings().then(setBookings).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, []);

  return (
    <div className="container page-section">
      <div className="section-head">
        <h2>My bookings</h2>
        <p>Everything you&apos;ve booked, in one place.</p>
      </div>

      {loading && <PageLoader />}
      {!loading && error && <div className="alert alert-danger">{error}</div>}
      {!loading && !error && bookings.length === 0 && (
        <EmptyState
          title="No bookings yet"
          message="Search for a flight to make your first booking."
          action={<Link to="/" className="btn btn-primary">Search flights</Link>}
        />
      )}

      {!loading && bookings.length > 0 && (
        <div className="stack">
          {bookings.map((b) => (
            <Link to={`/bookings/${b.bookingRef}`} key={b.id} className="card card-pad" style={{ display: 'block' }}>
              <div className="row-between">
                <div>
                  <div style={{ fontWeight: 700, fontSize: 15.5 }}>{b.originCity} → {b.destinationCity}</div>
                  <div className="muted" style={{ fontSize: 13, marginTop: 3 }}>
                    {b.flightNumber} · {formatDate(b.departureTime)} · Ref {b.bookingRef}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span className={`badge ${statusBadgeClass(b.status)}`}>{b.status}</span>
                  <div style={{ fontWeight: 700, marginTop: 8 }}>{formatMoney(b.totalAmount)}</div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
