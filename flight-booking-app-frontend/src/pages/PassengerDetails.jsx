import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { initiateBooking } from '../api/bookings';
import { Stepper } from '../components/Ui';
import { useBookingDraft } from '../context/BookingDraftContext';
import { formatMoney } from '../utils/format';

export default function PassengerDetails() {
  const { flightId } = useParams();
  const navigate = useNavigate();
  const { draft, updateDraft } = useBookingDraft();
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [shouldRedirect, setShouldRedirect] = useState(false);

  const seats = draft.selectedSeats;
  const flight = draft.flight;

  const [forms, setForms] = useState(
    seats.map(() => ({ fullName: '', age: '', passportNo: '' }))
  );

  // Redirect if draft is incomplete - use useEffect to avoid navigate during render
  useEffect(() => {
    if (!flight || seats.length === 0) {
      setShouldRedirect(true);
    }
  }, [flight, seats, flightId]);

  if (shouldRedirect) {
    return null; // Redirect will happen via useEffect below
  }

  useEffect(() => {
    if (shouldRedirect) {
      navigate(`/book/${flightId}/seats`, { replace: true });
    }
  }, [shouldRedirect, flightId, navigate]);

  const updateField = (i, field, value) => {
    setForms((prev) => prev.map((f, idx) => (idx === i ? { ...f, [field]: value } : f)));
  };

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    for (const f of forms) {
      if (!f.fullName.trim() || !f.age || Number(f.age) <= 0) {
        setError('Please fill in a full name and a valid age for every passenger.');
        return;
      }
    }

    setSubmitting(true);
    try {
      const payload = {
        flightId: Number(flightId),
        passengers: forms.map((f, i) => ({
          seatId: seats[i].id,
          fullName: f.fullName.trim(),
          age: Number(f.age),
          passportNo: f.passportNo.trim() || null,
        })),
      };
      const booking = await initiateBooking(payload);
      updateDraft({ passengers: forms });
      navigate(`/book/payment/${booking.bookingRef}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container page-section">
      <Stepper steps={['Select seats', 'Passenger details', 'Payment']} currentIndex={1} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 28, alignItems: 'start' }}>
        <form onSubmit={submit}>
          <div className="section-head">
            <h2>Who&apos;s flying?</h2>
            <p>Enter details exactly as they appear on a government ID.</p>
          </div>

          {error && <div className="alert alert-danger" style={{ marginBottom: 18 }}>{error}</div>}

          <div className="stack">
            {seats.map((seat, i) => (
              <div className="card card-pad" key={seat.id}>
                <div className="row-between" style={{ marginBottom: 14 }}>
                  <strong>Passenger {i + 1}</strong>
                  <span className="badge badge-neutral">Seat {seat.seatNumber} · {seat.seatClass === 'BUSINESS' ? 'Business' : 'Economy'}</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1.4fr', gap: 14 }}>
                  <div className="field">
                    <label>Full name</label>
                    <input value={forms[i].fullName} onChange={(e) => updateField(i, 'fullName', e.target.value)} required />
                  </div>
                  <div className="field">
                    <label>Age</label>
                    <input type="number" min="1" max="120" value={forms[i].age} onChange={(e) => updateField(i, 'age', e.target.value)} required />
                  </div>
                  <div className="field">
                    <label>Passport no. (optional)</label>
                    <input value={forms[i].passportNo} onChange={(e) => updateField(i, 'passportNo', e.target.value)} />
                  </div>
                </div>
              </div>
            ))}
          </div>

          <button type="submit" className="btn btn-primary" style={{ marginTop: 20 }} disabled={submitting}>
            {submitting ? 'Reserving your seats…' : 'Continue to payment'}
          </button>
        </form>

        <div className="card card-pad" style={{ position: 'sticky', top: 90 }}>
          <h3 style={{ fontSize: 16, marginBottom: 14 }}>Trip summary</h3>
          <div className="stack" style={{ gap: 8, fontSize: 14 }}>
            <div className="row-between"><span className="muted">Route</span><span>{flight.originCode} → {flight.destinationCode}</span></div>
            <div className="row-between"><span className="muted">Flight</span><span>{flight.flightNumber}</span></div>
            <div className="row-between"><span className="muted">Passengers</span><span>{seats.length}</span></div>
          </div>
          <hr className="divider" />
          <div className="row-between" style={{ fontWeight: 700 }}>
            <span>Estimated total</span>
            <span>{formatMoney(Number(flight.basePriceEconomy) * seats.length)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
