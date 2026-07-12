const ECONOMY_LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];
const BUSINESS_LETTERS = ['A', 'B', 'C', 'D'];

function groupByRow(seats, letters) {
  const rows = {};
  seats.forEach((s) => {
    const row = s.seatNumber.slice(0, -1);
    rows[row] = rows[row] || {};
    rows[row][s.seatNumber.slice(-1)] = s;
  });
  return Object.keys(rows)
    .sort((a, b) => Number(a) - Number(b))
    .map((row) => ({ row, seats: letters.map((l) => rows[row][l]).filter(Boolean) }));
}

export default function SeatMap({ seats, selectedSeatIds, onToggle, maxSelectable }) {
  const business = seats.filter((s) => s.seatClass === 'BUSINESS');
  const economy = seats.filter((s) => s.seatClass === 'ECONOMY');

  const renderSeat = (seat) => {
    const isSelected = selectedSeatIds.includes(seat.id);
    const isTaken = seat.status !== 'AVAILABLE';
    const classes = [
      'seat',
      seat.seatClass === 'BUSINESS' ? 'seat-business' : '',
      isSelected ? 'seat-selected' : '',
      isTaken ? (seat.status === 'BOOKED' ? 'seat-booked' : 'seat-locked') : '',
    ].join(' ').trim();

    const disabled = isTaken || (!isSelected && selectedSeatIds.length >= maxSelectable);

    return (
      <button
        key={seat.id}
        type="button"
        className={classes}
        disabled={disabled}
        title={isTaken ? `${seat.seatNumber} — unavailable` : seat.seatNumber}
        onClick={() => onToggle(seat)}
      >
        {seat.seatNumber}
      </button>
    );
  };

  return (
    <div className="seat-cabin">
      <div className="seat-legend">
        <span className="seat-legend-item"><span className="legend-swatch" style={{ background: '#fff', border: '1.5px solid var(--border-strong)' }} /> Available</span>
        <span className="seat-legend-item"><span className="legend-swatch" style={{ background: 'var(--blue-100)' }} /> Business</span>
        <span className="seat-legend-item"><span className="legend-swatch" style={{ background: 'var(--coral-500)' }} /> Selected</span>
        <span className="seat-legend-item"><span className="legend-swatch" style={{ background: 'var(--border)' }} /> Taken</span>
      </div>

      {business.length > 0 && (
        <>
          {groupByRow(business, BUSINESS_LETTERS).map(({ row, seats: rowSeats }) => (
            <div className="seat-row" key={`b-${row}`}>
              {rowSeats.slice(0, 2).map(renderSeat)}
              <span className="seat-aisle-gap" />
              {rowSeats.slice(2, 4).map(renderSeat)}
            </div>
          ))}
          <hr className="divider" />
        </>
      )}

      {groupByRow(economy, ECONOMY_LETTERS).map(({ row, seats: rowSeats }) => (
        <div className="seat-row" key={`e-${row}`}>
          {rowSeats.slice(0, 3).map(renderSeat)}
          <span className="seat-aisle-gap" />
          {rowSeats.slice(3, 6).map(renderSeat)}
        </div>
      ))}
    </div>
  );
}
