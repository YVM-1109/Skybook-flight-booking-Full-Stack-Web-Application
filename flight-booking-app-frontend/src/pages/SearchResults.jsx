import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { searchFlights } from '../api/flights';
import FlightCard from '../components/FlightCard';
import { PageLoader, EmptyState } from '../components/Ui';
import { useBookingDraft } from '../context/BookingDraftContext';

export default function SearchResults() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { updateDraft } = useBookingDraft();
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const origin = searchParams.get('origin');
  const destination = searchParams.get('destination');
  const date = searchParams.get('date');
  const passengers = Number(searchParams.get('passengers') || 1);
  const seatClass = searchParams.get('seatClass') || 'ECONOMY';

  useEffect(() => {
    if (!origin || !destination || !date) return;
    setLoading(true);
    setError('');
    searchFlights({ origin, destination, date, passengers, seatClass })
      .then(setFlights)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [origin, destination, date, passengers, seatClass]);

  const handleSelect = (flight) => {
    updateDraft({ flight, seatClass, passengerCount: passengers, selectedSeats: [], passengers: [] });
    navigate(`/book/${flight.id}/seats`);
  };

  return (
    <div className="container page-section">
      <div className="section-head">
        <h2>{origin} → {destination}</h2>
        <p>{date} · {passengers} passenger{passengers > 1 ? 's' : ''} · {seatClass === 'BUSINESS' ? 'Business' : 'Economy'}</p>
      </div>

      {loading && <PageLoader label="Searching flights…" />}
      {!loading && error && <div className="alert alert-danger">{error}</div>}

      {!loading && !error && flights.length === 0 && (
        <EmptyState
          title="No flights found"
          message="Try a different date, route, or fewer passengers."
        />
      )}

      {!loading && !error && flights.length > 0 && (
        <div className="stack">
          {flights.map((f) => (
            <FlightCard key={f.id} flight={f} seatClass={seatClass} onSelect={handleSelect} />
          ))}
        </div>
      )}
    </div>
  );
}
