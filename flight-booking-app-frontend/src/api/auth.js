import { client, unwrap } from './client';

export const registerUser = (payload) => unwrap(client.post('/auth/register', payload));
export const loginUser = (payload) => unwrap(client.post('/auth/login', payload));
export const logoutUser = () => unwrap(client.post('/auth/logout'));
