import { formatDuration, formatMoney, formatTime } from '../utils/format';

export default function FlightCard({ flight, seatClass, onSelect }) {
  const price = seatClass === 'BUSINESS' ? flight.basePriceBusiness : flight.basePriceEconomy;
  const availableSeats =
    seatClass === 'BUSINESS' ? flight.availableBusinessSeats : flight.availableEconomySeats;

  return (
    <div className="ticket-card">
      <div className="ticket-main">
        <div className="row-between">
          <div className="airline-row">
            <div className="airline-logo">{flight.airlineCode || flight.airlineName?.slice(0, 2) || '✈'}</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 14.5 }}>{flight.airlineName || 'Airline'}</div>
              <div className="dim" style={{ fontSize: 12.5 }}>
                {flight.flightNumber} · {flight.aircraftModel}
              </div>
            </div>
          </div>
          {flight.stops === 0 ? (
            <span className="badge badge-success">Non-stop</span>
          ) : (
            <span className="badge badge-neutral">{flight.stops} stop</span>
          )}
        </div>

        <div className="flight-route">
          <div>
            <div className="flight-time">{formatTime(flight.departureTime)}</div>
            <div className="flight-code">{flight.originCode} · {flight.originCity}</div>
          </div>
          <div className="flight-path">
            <div className="flight-duration">{formatDuration(flight.durationMinutes)}</div>
            <div className="line" />
            <div className="dim" style={{ fontSize: 11.5 }}>Terminal {flight.terminal}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className="flight-time">{formatTime(flight.arrivalTime)}</div>
            <div className="flight-code">{flight.destinationCode} · {flight.destinationCity}</div>
          </div>
        </div>

        <div className="dim" style={{ fontSize: 12.5 }}>
          {availableSeats > 0
            ? `${availableSeats} ${seatClass.toLowerCase()} seats left`
            : 'Sold out in this class'}
        </div>
      </div>

      <div className="ticket-stub">
        <div className="dim" style={{ color: 'rgba(244,247,255,0.6)', fontSize: 12 }}>
          {seatClass === 'BUSINESS' ? 'Business' : 'Economy'} fare
        </div>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 25, fontWeight: 700 }}>
          {formatMoney(price)}
        </div>
        <div className="dim" style={{ color: 'rgba(244,247,255,0.55)', fontSize: 11.5 }}>per passenger</div>
        <button
          className="btn btn-primary btn-block btn-sm"
          disabled={availableSeats <= 0}
          onClick={() => onSelect(flight)}
        >
          {availableSeats > 0 ? 'Select' : 'Unavailable'}
        </button>
      </div>
    </div>
  );
}
