import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listAirports } from '../api/flights';
import { useBookingDraft } from '../context/BookingDraftContext';

const todayISO = () => new Date().toISOString().split('T')[0];

export default function Home() {
  const navigate = useNavigate();
  const { updateDraft } = useBookingDraft();
  const [airports, setAirports] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    origin: '',
    destination: '',
    date: todayISO(),
    passengers: 1,
    seatClass: 'ECONOMY',
  });

  useEffect(() => {
    listAirports()
      .then((data) => {
        setAirports(data);
        if (data.length >= 2) {
          setForm((f) => ({ ...f, origin: data[0].code, destination: data[1].code }));
        }
      })
      .catch(() => setError('Could not load airports — is the backend running?'));
  }, []);

  const swap = () => setForm((f) => ({ ...f, origin: f.destination, destination: f.origin }));

  const submit = (e) => {
    e.preventDefault();
    if (form.origin === form.destination) {
      setError('Origin and destination can\u2019t be the same airport.');
      return;
    }
    setError('');
    updateDraft({ searchParams: form, flight: null, selectedSeats: [], passengers: [] });
    const params = new URLSearchParams({
      origin: form.origin,
      destination: form.destination,
      date: form.date,
      passengers: String(form.passengers),
      seatClass: form.seatClass,
    });
    navigate(`/search?${params.toString()}`);
  };

  return (
    <>
      <section className="hero">
        <div className="container">
          <span className="hero-eyebrow">● Live seat inventory · Secure Razorpay checkout</span>
          <h1>Find your next flight, and actually get a seat that&apos;s yours.</h1>
          <p className="sub">
            Search real-time availability, lock in a seat, and confirm with a payment —
            no placeholder bookings that go nowhere.
          </p>
        </div>
      </section>

      <div className="container">
        <form className="search-panel" onSubmit={submit}>
          {error && <div className="alert alert-danger" style={{ marginBottom: 16 }}>{error}</div>}
          <div className="search-grid">
            <div className="field">
              <label htmlFor="origin">From</label>
              <select id="origin" value={form.origin} onChange={(e) => setForm({ ...form, origin: e.target.value })} required>
                {airports.map((a) => (
                  <option key={a.code} value={a.code}>{a.city} ({a.code})</option>
                ))}
              </select>
            </div>

            <button type="button" className="search-swap" onClick={swap} aria-label="Swap origin and destination">⇄</button>

            <div className="field">
              <label htmlFor="destination">To</label>
              <select id="destination" value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} required>
                {airports.map((a) => (
                  <option key={a.code} value={a.code}>{a.city} ({a.code})</option>
                ))}
              </select>
            </div>

            <div className="field">
              <label htmlFor="date">Departure</label>
              <input id="date" type="date" min={todayISO()} value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
            </div>

            <div className="field">
              <label htmlFor="passengers">Passengers</label>
              <select id="passengers" value={form.passengers} onChange={(e) => setForm({ ...form, passengers: Number(e.target.value) })}>
                {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
            </div>

            <div className="field">
              <label htmlFor="seatClass">Class</label>
              <select id="seatClass" value={form.seatClass} onChange={(e) => setForm({ ...form, seatClass: e.target.value })}>
                <option value="ECONOMY">Economy</option>
                <option value="BUSINESS">Business</option>
              </select>
            </div>
          </div>

          <div style={{ marginTop: 18 }}>
            <button type="submit" className="btn btn-primary">Search flights</button>
          </div>
        </form>
      </div>

      <section className="page-section">
        <div className="container">
          <div className="section-head">
            <h2>Fly with confidence</h2>
            <p>Every booking on SkyBook goes through the same checks a real airline uses.</p>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 20 }}>
            {[
              { title: 'Seat locking', text: 'Your seat is held for 10 minutes while you pay — no double-booking.' },
              { title: 'Verified payments', text: 'Bookings only confirm once Razorpay verifies the payment server-side.' },
              { title: 'Instant e-tickets', text: 'Your ticket appears the moment your booking is confirmed.' },
            ].map((f) => (
              <div className="card card-pad" key={f.title}>
                <h3 style={{ fontSize: 16.5, marginBottom: 8 }}>{f.title}</h3>
                <p className="muted" style={{ fontSize: 14 }}>{f.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
