export function PageLoader({ label = 'Loading…' }) {
  return (
    <div className="page-loader">
      <span className="spinner" />
      {label}
    </div>
  );
}

export function EmptyState({ title, message, action }) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      <p>{message}</p>
      {action && <div style={{ marginTop: 18 }}>{action}</div>}
    </div>
  );
}

export function Stepper({ steps, currentIndex }) {
  return (
    <div className="stepper">
      {steps.map((label, i) => (
        <div key={label} style={{ display: 'flex', alignItems: 'center', flex: i < steps.length - 1 ? 1 : 'none' }}>
          <div className={`step ${i < currentIndex ? 'done' : i === currentIndex ? 'active' : ''}`}>
            <span className="step-dot">{i < currentIndex ? '✓' : i + 1}</span>
            <span>{label}</span>
          </div>
          {i < steps.length - 1 && <span className="step-line" />}
        </div>
      ))}
    </div>
  );
}
