import { client, unwrap } from './client';

export const searchFlights = (params) => unwrap(client.get('/flights/search', { params }));
export const getFlight = (id) => unwrap(client.get(`/flights/${id}`));
export const getSeatMap = (id) => unwrap(client.get(`/flights/${id}/seats`));

export const listAirports = () => unwrap(client.get('/airports'));
export const listAirlines = () => unwrap(client.get('/airlines'));
export const listAircraft = () => unwrap(client.get('/aircraft'));
