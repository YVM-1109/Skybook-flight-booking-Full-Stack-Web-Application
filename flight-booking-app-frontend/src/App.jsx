import { Route, Routes } from 'react-router-dom';
import Footer from './components/Footer';
import Navbar from './components/Navbar';
import { AdminRoute, ProtectedRoute } from './components/RouteGuards';
import AdminDashboard from './pages/AdminDashboard';
import BookingDetail from './pages/BookingDetail';
import Home from './pages/Home';
import Login from './pages/Login';
import MyBookings from './pages/MyBookings';
import NotFound from './pages/NotFound';
import PassengerDetails from './pages/PassengerDetails';
import Payment from './pages/Payment';
import Register from './pages/Register';
import SearchResults from './pages/SearchResults';
import SeatSelection from './pages/SeatSelection';

export default function App() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />
      <main style={{ flex: 1 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/search" element={<SearchResults />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/book/:flightId/seats" element={<ProtectedRoute><SeatSelection /></ProtectedRoute>} />
          <Route path="/book/:flightId/passengers" element={<ProtectedRoute><PassengerDetails /></ProtectedRoute>} />
          <Route path="/book/payment/:bookingRef" element={<ProtectedRoute><Payment /></ProtectedRoute>} />

          <Route path="/bookings" element={<ProtectedRoute><MyBookings /></ProtectedRoute>} />
          <Route path="/bookings/:ref" element={<ProtectedRoute><BookingDetail /></ProtectedRoute>} />

          <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />

          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
