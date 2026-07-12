import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { cancelBooking, getBookingByRef, initiatePayment } from '../api/bookings';
import { PageLoader, Stepper } from '../components/Ui';
import { useAuth } from '../context/AuthContext';
import { useBookingDraft } from '../context/BookingDraftContext';
import { formatMoney } from '../utils/format';

const POLL_INTERVAL_MS = 2500;
const POLL_TIMEOUT_MS = 60000;

export default function Payment() {
  const { bookingRef } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { resetDraft } = useBookingDraft();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [payState, setPayState] = useState('idle'); // idle | opening | verifying | failed
  const pollTimer = useRef(null);
  const pollDeadline = useRef(null);

  const loadBooking = useCallback(() => {
    return getBookingByRef(bookingRef).then(setBooking);
  }, [bookingRef]);

  useEffect(() => {
    setLoading(true);
    loadBooking().catch((e) => setError(e.message)).finally(() => setLoading(false));
    return () => clearTimeout(pollTimer.current);
  }, [loadBooking]);

  const pollForConfirmation = () => {
    setPayState('verifying');
    pollDeadline.current = Date.now() + POLL_TIMEOUT_MS;

    const tick = async () => {
      try {
        const fresh = await getBookingByRef(bookingRef);
        setBooking(fresh);
        if (fresh.status === 'CONFIRMED') {
          resetDraft();
          navigate(`/bookings/${bookingRef}`, { replace: true });
          return;
        }
      } catch {
        // keep polling — a transient error here shouldn't stop the flow
      }
      if (Date.now() < pollDeadline.current) {
        pollTimer.current = setTimeout(tick, POLL_INTERVAL_MS);
      } else {
        setPayState('idle');
        setError(
          'Payment received but confirmation is taking longer than usual. ' +
          'Check "My bookings" in a moment — it updates as soon as the webhook lands.'
        );
      }
    };
    tick();
  };

  const handlePay = async () => {
    setError('');
    setPayState('opening');
    try {
      const order = await initiatePayment(bookingRef);

      if (typeof window.Razorpay !== 'function') {
        throw new Error('Payment SDK failed to load. Check your connection and try again.');
      }

      const rzp = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: 'SkyBook',
        description: order.description,
        order_id: order.razorpayOrderId,
        prefill: { name: user?.fullName, email: user?.email },
        theme: { color: '#2451e0' },
        handler: () => {
          // Checkout succeeded client-side. We do NOT trust this alone —
          // the booking only becomes CONFIRMED once our backend verifies
          // the payment.captured webhook. So we poll until that happens.
          pollForConfirmation();
        },
        modal: {
          ondismiss: () => setPayState('idle'),
        },
      });

      rzp.on('payment.failed', () => {
        setPayState('failed');
        setError('Payment failed or was declined. You can try again.');
      });

      rzp.open();
    } catch (e) {
      setPayState('failed');
      setError(e.message);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm('Cancel this booking and release the seats?')) return;
    try {
      await cancelBooking(bookingRef);
      resetDraft();
      navigate('/bookings');
    } catch (e) {
      setError(e.message);
    }
  };

  if (loading) return <div className="container page-section"><PageLoader label="Loading booking…" /></div>;
  if (!booking) return <div className="container page-section"><div className="alert alert-danger">{error || 'Booking not found.'}</div></div>;

  if (booking.status !== 'PENDING') {
    return (
      <div className="container page-section">
        <div className="alert alert-info">
          This booking is already <strong>{booking.status.toLowerCase()}</strong>.
        </div>
        <button className="btn btn-secondary" style={{ marginTop: 16 }} onClick={() => navigate(`/bookings/${bookingRef}`)}>
          View booking
        </button>
      </div>
    );
  }

  return (
    <div className="container page-section">
      <Stepper steps={['Select seats', 'Passenger details', 'Payment']} currentIndex={2} />

      <div style={{ maxWidth: 480, margin: '0 auto' }}>
        <div className="card card-pad">
          <div className="row-between" style={{ marginBottom: 6 }}>
            <h2 style={{ fontSize: 20 }}>Complete your payment</h2>
            <span className="badge badge-warning">Pending</span>
          </div>
          <p className="muted" style={{ fontSize: 14, marginBottom: 20 }}>
            Booking ref <strong>{booking.bookingRef}</strong> · {booking.flightNumber} · {booking.originCity} → {booking.destinationCity}
          </p>

          <div className="row-between" style={{ fontSize: 15, marginBottom: 6 }}>
            <span className="muted">Passengers</span>
            <span>{booking.passengers.length}</span>
          </div>
          <div className="row-between" style={{ fontSize: 20, fontWeight: 700, marginTop: 10 }}>
            <span>Amount due</span>
            <span>{formatMoney(booking.totalAmount)}</span>
          </div>

          {error && <div className="alert alert-danger" style={{ marginTop: 16 }}>{error}</div>}

          <button
            className="btn btn-primary btn-block"
            style={{ marginTop: 22 }}
            disabled={payState === 'opening' || payState === 'verifying'}
            onClick={handlePay}
          >
            {payState === 'verifying' ? 'Confirming with the bank…' : payState === 'opening' ? 'Opening secure checkout…' : `Pay ${formatMoney(booking.totalAmount)} with Razorpay`}
          </button>
          <button className="btn btn-ghost btn-block" style={{ marginTop: 10 }} onClick={handleCancel} disabled={payState === 'verifying'}>
            Cancel booking
          </button>

          <p className="dim" style={{ fontSize: 11.5, marginTop: 16, textAlign: 'center' }}>
            Your seats are held for a limited time. Payments are processed securely by Razorpay —
            SkyBook never sees your card details.
          </p>
        </div>
      </div>
    </div>
  );
}
