import { createContext, useContext, useState } from 'react';

const BookingDraftContext = createContext(null);

export function BookingDraftProvider({ children }) {
  const [draft, setDraft] = useState({
    flight: null,
    seatClass: 'ECONOMY',
    passengerCount: 1,
    selectedSeats: [],
    passengers: [],
    searchParams: null,
  });

  const updateDraft = (patch) => setDraft((prev) => ({ ...prev, ...patch }));
  const resetDraft = () =>
    setDraft({
      flight: null,
      seatClass: 'ECONOMY',
      passengerCount: 1,
      selectedSeats: [],
      passengers: [],
      searchParams: null,
    });

  return (
    <BookingDraftContext.Provider value={{ draft, updateDraft, resetDraft }}>
      {children}
    </BookingDraftContext.Provider>
  );
}

export function useBookingDraft() {
  const ctx = useContext(BookingDraftContext);
  if (!ctx) throw new Error('useBookingDraft must be used within BookingDraftProvider');
  return ctx;
}
