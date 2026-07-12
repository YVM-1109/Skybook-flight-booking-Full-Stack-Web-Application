package com.flightbooking.service;
import com.razorpay.RazorpayException;
import com.flightbooking.entity.Booking;
import com.flightbooking.entity.BookingPassenger;
import com.flightbooking.entity.Payment;
import com.flightbooking.entity.Seat;
import com.flightbooking.entity.User;
import com.flightbooking.exception.PaymentVerificationException;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock RazorpayClient razorpayClient;
    @Mock SeatLockingService seatLockingService;
    @Mock OrderClient mockOrderClient;

    @InjectMocks
    PaymentService paymentService;

    private static final String TEST_WEBHOOK_SECRET = "test_webhook_secret_for_unit_tests";

    private User owner;
    private Booking pendingBooking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", TEST_WEBHOOK_SECRET);

        // RazorpayClient.orders is a public field, not a Spring bean.
        // We assign our mock manually here since @InjectMocks won't do it.
        razorpayClient.orders = mockOrderClient;

        owner = User.builder()
                .id(1L)
                .email("yash@test.com")
                .passwordHash("doesntmatter")
                .role(User.Role.PASSENGER)
                .build();

        pendingBooking = Booking.builder()
                .id(10L)
                .bookingRef("SKY123")
                .user(owner)
                .totalAmount(new BigDecimal("4500.00"))
                .status(Booking.BookingStatus.PENDING)
                .build();
    }

    // -------------------------------------------------------
    // Utility — builds a valid HMAC-SHA256 hex signature
    // matching what Razorpay sends in X-Razorpay-Signature.
    // This lets us test the happy-path webhook without
    // needing a live Razorpay connection.
    // -------------------------------------------------------
    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                TEST_WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // -------------------------------------------------------
    // createOrder — failure paths
    // -------------------------------------------------------

    @Test
    void createOrder_throwsWhenBookingNotFound() {
        when(bookingRepository.findByBookingRef("GHOST")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createOrder("GHOST", 1L));
    }

    @Test
    void createOrder_throwsAccessDeniedWhenUserDoesNotOwnBooking() {
        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));

        // User ID 99 is not user ID 1 — the booking's owner
        assertThrows(AccessDeniedException.class,
                () -> paymentService.createOrder("SKY123", 99L));
    }

    @Test
    void createOrder_throwsWhenBookingAlreadyConfirmed() {
        pendingBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));

        assertThrows(IllegalStateException.class,
                () -> paymentService.createOrder("SKY123", 1L));
    }

    @Test
    void createOrder_throwsWhenBookingCancelled() {
        pendingBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));

        assertThrows(IllegalStateException.class,
                () -> paymentService.createOrder("SKY123", 1L));
    }

    @Test
    void createOrder_throwsPaymentVerificationExceptionWhenRazorpayFails() throws Exception {
        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));
        when(mockOrderClient.create(any(JSONObject.class))).thenThrow(new com.razorpay.RazorpayException("Razorpay unreachable"));

        assertThrows(PaymentVerificationException.class,
                () -> paymentService.createOrder("SKY123", 1L));
    }

    // -------------------------------------------------------
    // createOrder — success paths
    // -------------------------------------------------------

    @Test
    void createOrder_createsNewPaymentRowOnFirstAttempt() throws Exception {
        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.findByBooking(pendingBooking)).thenReturn(Optional.empty());

        Order fakeOrder = mock(Order.class);
        when(fakeOrder.get("id")).thenReturn("order_rp_001");
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(fakeOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        var result = paymentService.createOrder("SKY123", 1L);

        assertNotNull(result);
        assertEquals("order_rp_001", result.getRazorpayOrderId());
        assertEquals("SKY123", result.getBookingRef());
        // 4500.00 INR → 450000 paise
        assertEquals(450000L, result.getAmountInPaise());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createOrder_reusesExistingPaymentRowOnRetry() throws Exception {
        // User started payment, closed the tab, came back — should update the
        // existing Payment row rather than inserting a duplicate
        Payment existingPayment = Payment.builder()
                .id(5L)
                .booking(pendingBooking)
                .razorpayOrderId("old_order_id")
                .amount(new BigDecimal("4500.00"))
                .status(Payment.PaymentStatus.CREATED)
                .build();

        when(bookingRepository.findByBookingRef("SKY123")).thenReturn(Optional.of(pendingBooking));
        when(paymentRepository.findByBooking(pendingBooking)).thenReturn(Optional.of(existingPayment));

        Order fakeOrder = mock(Order.class);
        when(fakeOrder.get("id")).thenReturn("order_rp_002");
        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(fakeOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.createOrder("SKY123", 1L);

        // Must update the existing row, not create a second one
        verify(paymentRepository, times(1)).save(existingPayment);
        assertEquals("order_rp_002", existingPayment.getRazorpayOrderId());
    }

    // -------------------------------------------------------
    // handleWebhook — signature verification
    // -------------------------------------------------------

    @Test
    void handleWebhook_throwsOnNullSignature() {
        assertThrows(PaymentVerificationException.class,
                () -> paymentService.handleWebhook("{\"event\":\"payment.captured\"}", null));
    }

    @Test
    void handleWebhook_throwsOnInvalidSignature() {
        assertThrows(PaymentVerificationException.class,
                () -> paymentService.handleWebhook("{\"event\":\"payment.captured\"}", "badsignature"));
    }

    @Test
    void handleWebhook_throwsOnTamperedPayloadWithValidSignatureForDifferentPayload() throws Exception {
        // Sign one payload, send a different one — must reject
        String signedPayload = "{\"event\":\"payment.captured\",\"payload\":{}}";
        String tamperedPayload = "{\"event\":\"payment.captured\",\"payload\":{\"injected\":true}}";
        String sig = sign(signedPayload);

        assertThrows(PaymentVerificationException.class,
                () -> paymentService.handleWebhook(tamperedPayload, sig));
    }

    // -------------------------------------------------------
    // handleWebhook — event routing
    // -------------------------------------------------------

    @Test
    void handleWebhook_silentlyIgnoresNonCaptureEvents() throws Exception {
        String payload = """
            {
              "event": "payment.failed",
              "payload": {
                "payment": { "entity": { "id": "pay_x", "order_id": "order_x" } }
              }
            }
            """;
        String sig = sign(payload);

        // Must not throw and must not touch any repository
        paymentService.handleWebhook(payload, sig);

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(seatLockingService);
    }

    @Test
    void handleWebhook_confirmsBookingAndSeatsOnPaymentCaptured() throws Exception {
        String payload = """
            {
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": { "id": "pay_live_abc", "order_id": "order_live_xyz" }
                }
              }
            }
            """;
        String sig = sign(payload);

        Seat seat = Seat.builder().id(7L).status(Seat.SeatStatus.LOCKED).build();
        BookingPassenger passenger = BookingPassenger.builder().seat(seat).build();
        pendingBooking.setPassengers(List.of(passenger));

        Payment payment = Payment.builder()
                .id(3L)
                .booking(pendingBooking)
                .razorpayOrderId("order_live_xyz")
                .amount(new BigDecimal("4500.00"))
                .status(Payment.PaymentStatus.CREATED)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_live_xyz"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        paymentService.handleWebhook(payload, sig);

        // Payment row updated correctly
        assertEquals(Payment.PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("pay_live_abc", payment.getRazorpayPaymentId());
        assertNotNull(payment.getCompletedAt());

        // Booking confirmed
        assertEquals(Booking.BookingStatus.CONFIRMED, pendingBooking.getStatus());

        // Seats locked → booked
        verify(seatLockingService, times(1)).confirmSeats(List.of(7L));
    }

    // -------------------------------------------------------
    // handleWebhook — idempotency
    // -------------------------------------------------------

    @Test
    void handleWebhook_isIdempotent_razorpayRedeliveryDoesNotDoubleConfirm() throws Exception {
        // Razorpay retries webhook delivery if your server returns non-200.
        // If the payment is already SUCCESS, we must do nothing on re-delivery.
        String payload = """
            {
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": { "id": "pay_live_abc", "order_id": "order_live_xyz" }
                }
              }
            }
            """;
        String sig = sign(payload);

        Payment alreadySuccessful = Payment.builder()
                .id(3L)
                .booking(pendingBooking)
                .razorpayOrderId("order_live_xyz")
                .status(Payment.PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_live_xyz"))
                .thenReturn(Optional.of(alreadySuccessful));

        paymentService.handleWebhook(payload, sig);

        // Nothing should be saved or confirmed again
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(seatLockingService);
    }
}