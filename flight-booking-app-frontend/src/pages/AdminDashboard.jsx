import { useEffect, useState } from 'react';
import { adminAddFlight, adminGetAllBookings, adminGetAllFlights, adminUpdateFlightStatus } from '../api/bookings';
import { listAircraft, listAirlines, listAirports } from '../api/flights';
import { EmptyState, PageLoader } from '../components/Ui';
import { formatDate, formatMoney, formatTime, statusBadgeClass } from '../utils/format';

const TABS = [
  { key: 'add', label: 'Add flight' },
  { key: 'flights', label: 'All flights' },
  { key: 'bookings', label: 'All bookings' },
];

const emptyFlight = {
  flightNumber: '',
  originCode: '',
  destinationCode: '',
  aircraftId: '',
  airlineId: '',
  departureTime: '',
  arrivalTime: '',
  boardingTime: '',
  terminal: '',
  gate: '',
  stops: 0,
  basePriceEconomy: '',
  basePriceBusiness: '',
};

export default function AdminDashboard() {
  const [tab, setTab] = useState('add');
  const [airports, setAirports] = useState([]);
  const [airlines, setAirlines] = useState([]);
  const [aircraft, setAircraft] = useState([]);

  useEffect(() => {
    listAirports().then(setAirports).catch(() => {});
    listAirlines().then(setAirlines).catch(() => {});
    listAircraft().then(setAircraft).catch(() => {});
  }, []);

  return (
    <div className="container page-section">
      <div className="section-head">
        <h2>Admin</h2>
        <p>Manage flights and view all bookings across every user.</p>
      </div>

      <div className="admin-tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`admin-tab ${tab === t.key ? 'active' : ''}`}
            style={{ background: 'none', border: 'none', borderBottom: '2.5px solid transparent' }}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'add' && <AddFlightTab airports={airports} airlines={airlines} aircraft={aircraft} />}
      {tab === 'flights' && <AllFlightsTab airlines={airlines} />}
      {tab === 'bookings' && <AllBookingsTab />}
    </div>
  );
}

function AddFlightTab({ airports, airlines, aircraft }) {
  const [form, setForm] = useState(emptyFlight);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const set = (field, value) => setForm((f) => ({ ...f, [field]: value }));

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (form.originCode === form.destinationCode) {
      setError('Origin and destination can\u2019t be the same airport.');
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        ...form,
        aircraftId: Number(form.aircraftId),
        airlineId: Number(form.airlineId),
        stops: Number(form.stops) || 0,
        basePriceEconomy: Number(form.basePriceEconomy),
        basePriceBusiness: Number(form.basePriceBusiness),
      };
      const flight = await adminAddFlight(payload);
      setSuccess(`Flight ${flight.flightNumber} created with a full seat map.`);
      setForm(emptyFlight);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="card card-pad" onSubmit={submit} style={{ maxWidth: 640 }}>
      {error && <div className="alert alert-danger" style={{ marginBottom: 16 }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: 16 }}>{success}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div className="field">
          <label>Flight number</label>
          <input value={form.flightNumber} onChange={(e) => set('flightNumber', e.target.value)} required placeholder="SB-101" />
        </div>
        <div className="field">
          <label>Stops</label>
          <input type="number" min="0" max="3" value={form.stops} onChange={(e) => set('stops', e.target.value)} />
        </div>

        <div className="field">
          <label>Origin</label>
          <select value={form.originCode} onChange={(e) => set('originCode', e.target.value)} required>
            <option value="">Select</option>
            {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
          </select>
        </div>
        <div className="field">
          <label>Destination</label>
          <select value={form.destinationCode} onChange={(e) => set('destinationCode', e.target.value)} required>
            <option value="">Select</option>
            {airports.map((a) => <option key={a.code} value={a.code}>{a.city} ({a.code})</option>)}
          </select>
        </div>

        <div className="field">
          <label>Airline</label>
          <select value={form.airlineId} onChange={(e) => set('airlineId', e.target.value)} required>
            <option value="">Select</option>
            {airlines.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Aircraft</label>
          <select value={form.aircraftId} onChange={(e) => set('aircraftId', e.target.value)} required>
            <option value="">Select</option>
            {aircraft.map((a) => <option key={a.id} value={a.id}>{a.model} ({a.totalSeats} seats)</option>)}
          </select>
        </div>

        <div className="field">
          <label>Departure time</label>
          <input type="datetime-local" value={form.departureTime} onChange={(e) => set('departureTime', e.target.value)} required />
        </div>
        <div className="field">
          <label>Arrival time</label>
          <input type="datetime-local" value={form.arrivalTime} onChange={(e) => set('arrivalTime', e.target.value)} required />
        </div>
        <div className="field">
          <label>Boarding time</label>
          <input type="datetime-local" value={form.boardingTime} onChange={(e) => set('boardingTime', e.target.value)} required />
        </div>
        <div className="field" />

        <div className="field">
          <label>Terminal</label>
          <input value={form.terminal} onChange={(e) => set('terminal', e.target.value)} required placeholder="T2" />
        </div>
        <div className="field">
          <label>Gate</label>
          <input value={form.gate} onChange={(e) => set('gate', e.target.value)} required placeholder="G14" />
        </div>

        <div className="field">
          <label>Economy price (₹)</label>
          <input type="number" min="1" value={form.basePriceEconomy} onChange={(e) => set('basePriceEconomy', e.target.value)} required />
        </div>
        <div className="field">
          <label>Business price (₹)</label>
          <input type="number" min="1" value={form.basePriceBusiness} onChange={(e) => set('basePriceBusiness', e.target.value)} required />
        </div>
      </div>

      <button type="submit" className="btn btn-primary" style={{ marginTop: 20 }} disabled={submitting}>
        {submitting ? 'Creating…' : 'Create flight'}
      </button>
    </form>
  );
}

function AllFlightsTab({ airlines }) {
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingId, setUpdatingId] = useState(null);

  const load = () => adminGetAllFlights().then(setFlights).catch((e) => setError(e.message));

  useEffect(() => { setLoading(true); load().finally(() => setLoading(false)); }, []);

  const airlineName = (id, fallback) => airlines.find((a) => a.id === id)?.name || fallback;

  const changeStatus = async (id, status) => {
    setUpdatingId(id);
    try {
      await adminUpdateFlightStatus(id, status);
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setUpdatingId(null);
    }
  };

  if (loading) return <PageLoader />;
  if (error) return <div className="alert alert-danger">{error}</div>;
  if (flights.length === 0) return <EmptyState title="No flights yet" message="Add one from the 'Add flight' tab." />;

  return (
    <div className="card" style={{ overflowX: 'auto' }}>
      <table className="data-table">
        <thead>
          <tr>
            <th>Flight</th><th>Route</th><th>Departure</th><th>Airline</th><th>Status</th><th>Change status</th>
          </tr>
        </thead>
        <tbody>
          {flights.map((f) => (
            <tr key={f.id}>
              <td>{f.flightNumber}</td>
              <td>{f.originCode} → {f.destinationCode}</td>
              <td>{formatDate(f.departureTime)}, {formatTime(f.departureTime)}</td>
              <td>{airlineName(f.airlineId, f.airlineName)}</td>
              <td><span className={`badge ${statusBadgeClass(f.status)}`}>{f.status}</span></td>
              <td>
                <select
                  defaultValue=""
                  disabled={updatingId === f.id}
                  onChange={(e) => { if (e.target.value) changeStatus(f.id, e.target.value); e.target.value = ''; }}
                >
                  <option value="">Set status…</option>
                  {['SCHEDULED', 'DELAYED', 'CANCELLED', 'COMPLETED'].map((s) => (
                    <option key={s} value={s} disabled={s === f.status}>{s}</option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AllBookingsTab() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    adminGetAllBookings().then(setBookings).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;
  if (error) return <div className="alert alert-danger">{error}</div>;
  if (bookings.length === 0) return <EmptyState title="No bookings yet" message="Bookings will show up here as users make them." />;

  return (
    <div className="card" style={{ overflowX: 'auto' }}>
      <table className="data-table">
        <thead>
          <tr>
            <th>Ref</th><th>Flight</th><th>Passengers</th><th>Amount</th><th>Status</th><th>Booked</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map((b) => (
            <tr key={b.id}>
              <td>{b.bookingRef}</td>
              <td>{b.flightNumber} · {b.originCity} → {b.destinationCity}</td>
              <td>{b.passengers.length}</td>
              <td>{formatMoney(b.totalAmount)}</td>
              <td><span className={`badge ${statusBadgeClass(b.status)}`}>{b.status}</span></td>
              <td>{formatDate(b.bookedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
