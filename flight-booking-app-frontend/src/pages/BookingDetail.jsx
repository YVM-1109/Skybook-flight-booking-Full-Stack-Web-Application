import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { cancelBooking, getBookingByRef } from '../api/bookings';
import { PageLoader } from '../components/Ui';
import { formatDate, formatMoney, formatTime, statusBadgeClass } from '../utils/format';

export default function BookingDetail() {
  const { ref } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => getBookingByRef(ref).then(setBooking).catch((e) => setError(e.message));

  useEffect(() => {
    setLoading(true);
    load().finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ref]);

  const handleCancel = async () => {
    if (!window.confirm('Cancel this booking and release the seats? This can\u2019t be undone.')) return;
    try {
      await cancelBooking(ref);
      await load();
    } catch (e) {
      setError(e.message);
    }
  };

  if (loading) return <div className="container page-section"><PageLoader /></div>;
  if (error && !booking) return <div className="container page-section"><div className="alert alert-danger">{error}</div></div>;
  if (!booking) return null;

  const isConfirmed = booking.status === 'CONFIRMED';
  const isPending = booking.status === 'PENDING';

  return (
    <div className="container page-section" style={{ maxWidth: 760, margin: '0 auto' }}>
      {error && <div className="alert alert-danger" style={{ marginBottom: 16 }}>{error}</div>}

      {isPending && (
        <div className="alert alert-info" style={{ marginBottom: 20 }}>
          This booking isn&apos;t paid yet.{' '}
          <Link to={`/book/payment/${booking.bookingRef}`} style={{ fontWeight: 700, textDecoration: 'underline' }}>
            Complete payment →
          </Link>
        </div>
      )}

      <div className="ticket-card">
        <div className="ticket-main">
          <div className="row-between" style={{ marginBottom: 4 }}>
            <span className="dim" style={{ fontSize: 12.5, fontWeight: 700, letterSpacing: '0.04em' }}>
              {isConfirmed ? 'E-TICKET' : 'BOOKING'}
            </span>
            <span className={`badge ${statusBadgeClass(booking.status)}`}>{booking.status}</span>
          </div>

          <div className="flight-route">
            <div>
              <div className="flight-time">{formatTime(booking.departureTime)}</div>
              <div className="flight-code">{booking.originCity}</div>
            </div>
            <div className="flight-path">
              <div className="flight-duration">{booking.flightNumber}</div>
              <div className="line" />
              <div className="dim" style={{ fontSize: 11.5 }}>{formatDate(booking.departureTime)}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="flight-time" />
              <div className="flight-code">{booking.destinationCity}</div>
            </div>
          </div>

          <hr className="divider" />

          <h4 style={{ fontSize: 14, marginBottom: 10 }}>Passengers &amp; seats</h4>
          <div className="stack" style={{ gap: 8 }}>
            {booking.passengers.map((p, i) => (
              <div key={i} className="row-between" style={{ fontSize: 14 }}>
                <span>{p.fullName} <span className="dim">· age {p.age}</span></span>
                <span style={{ fontWeight: 700 }}>{p.seatNumber} · {p.seatClass === 'BUSINESS' ? 'Business' : 'Economy'}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="ticket-stub">
          <div className="dim" style={{ color: 'rgba(244,247,255,0.6)', fontSize: 12 }}>Booking reference</div>
          <div style={{ fontFamily: 'var(--font-display)', fontSize: 22, fontWeight: 800, letterSpacing: '0.04em' }}>
            {booking.bookingRef}
          </div>
          <hr style={{ border: 'none', borderTop: '1px dashed rgba(255,255,255,0.25)', margin: '6px 0' }} />
          <div className="dim" style={{ color: 'rgba(244,247,255,0.6)', fontSize: 12 }}>Total paid</div>
          <div style={{ fontSize: 19, fontWeight: 700 }}>{formatMoney(booking.totalAmount)}</div>
          <div className="dim" style={{ color: 'rgba(244,247,255,0.5)', fontSize: 11, marginTop: 6 }}>
            Booked {formatDate(booking.bookedAt)}
          </div>
        </div>
      </div>

      <div className="row" style={{ marginTop: 20, gap: 12 }}>
        <button className="btn btn-ghost" onClick={() => navigate('/bookings')}>Back to my bookings</button>
        {booking.status !== 'CANCELLED' && (
          <button className="btn btn-danger" onClick={handleCancel}>Cancel booking</button>
        )}
        {isConfirmed && (
          <button className="btn btn-secondary" onClick={() => window.print()}>Print e-ticket</button>
        )}
      </div>
    </div>
  );
}
