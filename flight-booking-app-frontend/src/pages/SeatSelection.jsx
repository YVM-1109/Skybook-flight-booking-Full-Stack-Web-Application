import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getFlight, getSeatMap } from '../api/flights';
import SeatMap from '../components/SeatMap';
import { PageLoader, Stepper } from '../components/Ui';
import { useBookingDraft } from '../context/BookingDraftContext';
import { formatDate, formatMoney, formatTime } from '../utils/format';

export default function SeatSelection() {
  const { flightId } = useParams();
  const navigate = useNavigate();
  const { draft, updateDraft } = useBookingDraft();
  const [flight, setFlight] = useState(draft.flight?.id === Number(flightId) ? draft.flight : null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const passengerCount = draft.passengerCount || 1;
  const seatClass = draft.seatClass || 'ECONOMY';

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([
      flight ? Promise.resolve(flight) : getFlight(flightId),
      getSeatMap(flightId),
    ])
      .then(([flightData, seatData]) => {
        setFlight(flightData);
        setSeats(seatData);
        updateDraft({ flight: flightData });
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flightId]);

  const toggleSeat = (seat) => {
    const already = draft.selectedSeats.some((s) => s.id === seat.id);
    if (already) {
      updateDraft({ selectedSeats: draft.selectedSeats.filter((s) => s.id !== seat.id) });
    } else if (draft.selectedSeats.length < passengerCount) {
      updateDraft({ selectedSeats: [...draft.selectedSeats, seat] });
    }
  };

  const canContinue = draft.selectedSeats.length === passengerCount;

  const handleContinue = () => {
    navigate(`/book/${flightId}/passengers`);
  };

  if (loading) return <div className="container page-section"><PageLoader label="Loading seat map…" /></div>;
  if (error) return <div className="container page-section"><div className="alert alert-danger">{error}</div></div>;
  if (!flight) return null;

  return (
    <div className="container page-section">
      <Stepper steps={['Select seats', 'Passenger details', 'Payment']} currentIndex={0} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 28, alignItems: 'start' }}>
        <div>
          <div className="section-head">
            <h2>Choose {passengerCount} seat{passengerCount > 1 ? 's' : ''}</h2>
            <p>{flight.originCode} → {flight.destinationCode} · {formatDate(flight.departureTime)} · {formatTime(flight.departureTime)}</p>
          </div>
          <SeatMap
            seats={seats}
            selectedSeatIds={draft.selectedSeats.map((s) => s.id)}
            onToggle={toggleSeat}
            maxSelectable={passengerCount}
          />
        </div>

        <div className="card card-pad" style={{ position: 'sticky', top: 90 }}>
          <h3 style={{ fontSize: 16, marginBottom: 14 }}>Your selection</h3>
          <div className="stack" style={{ gap: 10 }}>
            {Array.from({ length: passengerCount }).map((_, i) => {
              const s = draft.selectedSeats[i];
              return (
                <div key={i} className="row-between" style={{ fontSize: 14 }}>
                  <span className="muted">Passenger {i + 1}</span>
                  <span style={{ fontWeight: 700 }}>{s ? `${s.seatNumber} · ${s.seatClass === 'BUSINESS' ? 'Business' : 'Economy'}` : 'Not selected'}</span>
                </div>
              );
            })}
          </div>
          <hr className="divider" />
          <div className="row-between" style={{ fontSize: 14.5, fontWeight: 700 }}>
            <span>Fare (economy base × pax)</span>
            <span>{formatMoney(Number(flight.basePriceEconomy) * passengerCount)}</span>
          </div>
          <p className="dim" style={{ fontSize: 12, marginTop: 6 }}>
            Final total is confirmed on the payment step.
          </p>
          <button className="btn btn-primary btn-block" style={{ marginTop: 16 }} disabled={!canContinue} onClick={handleContinue}>
            Continue
          </button>
        </div>
      </div>
    </div>
  );
}
