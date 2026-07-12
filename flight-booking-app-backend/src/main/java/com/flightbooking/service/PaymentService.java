package com.flightbooking.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightbooking.dto.response.PaymentOrderResponse;
import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Payment;
import com.flightbooking.exception.PaymentVerificationException;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
// Handles the two halves of the Razorpay flow described in the payment
// business logic: (1) initiating an order when the user is ready to pay for
// a PENDING booking, and (2) processing the payment.captured webhook that
// Razorpay calls once money has actually moved, which is the ONLY place a
// booking is allowed to become CONFIRMED. The frontend's checkout success
// callback is not trusted for confirmation — only the signed webhook is.
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final SeatLockingService seatLockingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void validateConfig() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret (app.razorpay.webhook-secret) is not configured. Set a non-empty secret in environment or application.properties.");
        }
    }

    @Transactional
    public PaymentOrderResponse createOrder(String bookingRef, Long userId) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this booking");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Booking is " + booking.getStatus().name().toLowerCase() + " — it cannot be paid for again");
        }

        long amountInPaise = booking.getTotalAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValueExact();

        Order order;
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", booking.getBookingRef());
            order = razorpayClient.orders.create(orderRequest);
        } catch (com.razorpay.RazorpayException e) {
    log.error("Razorpay order creation failed for booking {}", bookingRef, e);
    throw new PaymentVerificationException("Could not initiate payment. Please try again.");
}

        String orderId = order.get("id");

        // A booking has at most one Payment row (OneToOne, unique booking_id).
        // Reuse it on retry instead of trying to insert a second row.
        Payment payment = paymentRepository.findByBooking(booking).orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .booking(booking)
                    .razorpayOrderId(orderId)
                    .amount(booking.getTotalAmount())
                    .currency("INR")
                    .status(Payment.PaymentStatus.CREATED)
                    .build();
        } else {
            payment.setRazorpayOrderId(orderId);
            payment.setStatus(Payment.PaymentStatus.CREATED);
        }
        paymentRepository.save(payment);

        return PaymentOrderResponse.builder()
                .razorpayOrderId(orderId)
                .razorpayKeyId(razorpayKeyId)
                .amountInPaise(amountInPaise)
                .currency("INR")
                .bookingRef(booking.getBookingRef())
                .description("Flight booking " + booking.getBookingRef())
                .build();
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (signature == null || signature.isBlank() || !isSignatureValid(payload, signature)) {
            throw new PaymentVerificationException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new PaymentVerificationException("Malformed webhook payload");
        }

        String event = root.path("event").asText();
        if (!"payment.captured".equals(event)) {
            // Not an event we act on (e.g. payment.failed) — acknowledge and ignore.
            log.info("Ignoring Razorpay webhook event: {}", event);
            return;
        }

        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String orderId = paymentEntity.path("order_id").asText(null);
        String paymentId = paymentEntity.path("id").asText(null);

        if (orderId == null) {
            throw new PaymentVerificationException("Webhook payload missing order_id");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for order " + orderId));

        // Idempotency: Razorpay may retry webhook delivery. Don't double-confirm.
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            log.info("Payment for order {} already processed, ignoring duplicate webhook", orderId);
            return;
        }

        payment.setRazorpayPaymentId(paymentId);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Ticket-issuance guard: booking only ever moves to CONFIRMED here,
        // triggered exclusively by a verified payment.captured event.
        Booking booking = payment.getBooking();
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        List<Long> seatIds = booking.getPassengers().stream()
                .map(p -> p.getSeat().getId())
                .toList();
        seatLockingService.confirmSeats(seatIds);

        log.info("Booking {} confirmed via payment {}", booking.getBookingRef(), paymentId);
    }

    private boolean isSignatureValid(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Webhook secret not configured - cannot verify signature");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
