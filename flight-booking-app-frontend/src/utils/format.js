export function formatMoney(amount) {
  const n = Number(amount || 0);
  return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

export function formatTime(dateTimeStr) {
  if (!dateTimeStr) return '--:--';
  const d = new Date(dateTimeStr);
  return d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
}

export function formatDate(dateTimeStr) {
  if (!dateTimeStr) return '';
  const d = new Date(dateTimeStr);
  return d.toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
}

export function formatDuration(minutes) {
  if (!minutes && minutes !== 0) return '';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
}

export function statusBadgeClass(status) {
  switch (status) {
    case 'CONFIRMED':
    case 'SUCCESS':
    case 'SCHEDULED':
      return 'badge-success';
    case 'PENDING':
    case 'CREATED':
    case 'DELAYED':
      return 'badge-warning';
    case 'CANCELLED':
    case 'FAILED':
      return 'badge-danger';
    default:
      return 'badge-neutral';
  }
}
