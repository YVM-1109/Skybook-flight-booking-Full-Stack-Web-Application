import { client, unwrap } from './client';

export const initiateBooking = (payload) => unwrap(client.post('/bookings/initiate', payload));
export const getMyBookings = () => unwrap(client.get('/bookings'));
export const getBookingByRef = (ref) => unwrap(client.get(`/bookings/${ref}`));
export const cancelBooking = (ref) => unwrap(client.delete(`/bookings/${ref}/cancel`));

export const initiatePayment = (bookingRef) => unwrap(client.post(`/payments/initiate/${bookingRef}`));

export const adminAddFlight = (payload) => unwrap(client.post('/admin/flights', payload));
export const adminGetAllFlights = () => unwrap(client.get('/admin/flights'));
export const adminGetAllBookings = () => unwrap(client.get('/admin/bookings'));
export const adminUpdateFlightStatus = (id, status) =>
  unwrap(client.patch(`/admin/flights/${id}/status`, null, { params: { status } }));
