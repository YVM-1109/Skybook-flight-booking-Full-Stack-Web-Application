# SkyBook — Flight Booking System

A full-stack flight booking application with **real-time seat locking**, **Razorpay payment integration**, **JWT authentication**, and **admin management**. Built with **Java Spring Boot 3.2** (backend) and **React 18 + Vite** (frontend).

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Flight Search** | Search by origin, destination, date, passengers, class (Economy/Business) |
| **Live Seat Map** | Visual cabin layout with real-time availability (AVAILABLE / LOCKED / BOOKED) |
| **Seat Locking** | Optimistic locking — seats held for 10 min during checkout; auto-released if abandoned |
| **Passenger Details** | Per-seat passenger info (name, age, passport) |
| **Razorpay Checkout** | Secure payment via Razorpay; booking only confirms on verified `payment.captured` webhook |
| **E-Tickets** | Instant e-ticket view/print after confirmation |
| **My Bookings** | User dashboard with status badges (PENDING / CONFIRMED / CANCELLED) |
| **Admin Dashboard** | Create flights, view all flights/bookings, update flight status |
| **Security** | JWT (15 min access / 7 day refresh), BCrypt, rate limiting (5 req/min per IP on auth), account lockout (5 failures = 15 min), security headers (HSTS, X-Frame-Options, CSP-ready) |

---

## 🏗 Architecture

```
skybook-flight-booking-app/
├── flight-booking-app-backend/    # Spring Boot 3.2 (Java 17, Maven)
│   ├── src/main/java/com/flightbooking/
│   │   ├── config/                # Security, CORS, Razorpay, DataSeeder
│   │   ├── controller/            # REST endpoints (auth, flights, bookings, payments, admin)
│   │   ├── dto/                   # Request/Response DTOs
│   │   ├── entity/                # JPA entities (User, Flight, Seat, Booking, Payment, RefreshToken)
│   │   ├── exception/             # Custom exceptions + GlobalExceptionHandler
│   │   ├── repository/            # Spring Data JPA repositories
│   │   ├── security/              # JWT, RateLimitFilter, UserDetailsService
│   │   ├── service/               # Business logic
│   │   └── util/                  # BookingRefGenerator
│   └── src/test/                  # 25 unit/integration tests (all passing)
│
├── flight-booking-app-frontend/   # React 18 + Vite 5
│   ├── src/
│   │   ├── api/                   # Axios client + endpoint modules
│   │   ├── components/            # Reusable UI (SeatMap, FlightCard, Navbar, etc.)
│   │   ├── context/               # AuthContext, BookingDraftContext
│   │   ├── pages/                 # Route-level components (Home, Search, SeatSelection, Payment, etc.)
│   │   ├── utils/                 # Formatters (money, time, date, duration)
│   │   └── styles/                # Global CSS (CSS variables, no framework)
│   └── index.html
│
└── README.md
```

---

## 🔄 Booking Flow

```
1. SEARCH FLIGHTS
   GET /api/flights/search?origin=DEL&destination=BOM&date=2025-07-20&passengers=2&seatClass=ECONOMY

2. SELECT SEATS
   GET /api/flights/{id}/seats  →  visual SeatMap
   POST /api/bookings/initiate  →  locks seats (LOCKED, 10 min TTL), creates PENDING booking

3. PASSENGER DETAILS
   Frontend collects per-seat passenger info

4. PAYMENT
   POST /api/payments/initiate/{bookingRef}  →  creates Razorpay order
   Razorpay Checkout opens in browser
   User pays → Razorpay calls POST /api/payments/webhook (server-side)

5. CONFIRMATION (webhook only)
   payment.captured → Payment.SUCCESS, Booking.CONFIRMED, Seats.BOOKED
   Frontend polls GET /api/bookings/{ref} until CONFIRMED (max 60s)

6. E-TICKET
   GET /bookings/{ref}  →  printable ticket with QR-ready booking reference
```

> **Critical**: The frontend's "payment success" callback is **not trusted**. Only the signed Razorpay webhook can transition a booking to `CONFIRMED`.

---

## 🛡 Security Highlights

| Layer | Implementation |
|-------|----------------|
| **Authentication** | JWT (HS256) — 15 min access token, 7 day refresh token (stored in DB with revocation) |
| **Passwords** | BCrypt strength 12 |
| **Rate Limiting** | Bucket4j token bucket — 5 req/min per IP on `/api/auth/login`, `/api/auth/register` |
| **Account Lockout** | 5 failed logins → 15 min lock (tracked per account, not per IP) |
| **IDOR Protection** | All booking endpoints verify `booking.user.id == currentUser.id` (unless admin) |
| **Concurrency** | `@Version` on `Seat` — optimistic locking prevents double-booking |
| **Webhook Security** | HMAC-SHA256 verification of `X-Razorpay-Signature` |
| **Security Headers** | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security`, `Referrer-Policy: strict-origin-when-cross-origin` |
| **CORS** | Restricted to configured origin (`http://localhost:5173` dev) with `allowCredentials: true` |

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK)
- **Maven 3.9+**
- **Node.js 18+** & **npm**
- **MySQL 8.0+** (or use the H2 test profile)

---

### 1. Database Setup
```sql
CREATE DATABASE flight_booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'skybook'@'%' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON flight_booking_db.* TO 'skybook'@'%';
FLUSH PRIVILEGES;
```

---

### 2. Backend Configuration

```bash
cd flight-booking-app-backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

Edit `application-local.properties` with your values:
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/flight_booking_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=skybook
spring.datasource.password=your_strong_password

# JWT (generate with: openssl rand -base64 32)
app.jwt.secret=YOUR_BASE64_ENCODED_256_BIT_SECRET

# Razorpay (get from https://dashboard.razorpay.com/)
app.razorpay.key-id=rzp_test_xxxxx
app.razorpay.key-secret=xxxxxxxx
app.razorpay.webhook-secret=whsec_xxxxx

# Admin user (seeded on first run)
app.admin.password=YourSecureAdminPassword123
```

> **Note**: `application-local.properties` is gitignored. The example file shows all required keys.

---

### 3. Run Backend

```bash
# Option A: With local profile (uses application-local.properties)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Option B: Export env vars and run default profile
export DB_PASSWORD=your_strong_password
export JWT_SECRET=$(openssl rand -base64 32)
export RAZORPAY_KEY_ID=rzp_test_xxxxx
export RAZORPAY_KEY_SECRET=xxxxxxxx
export RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx
export ADMIN_PASSWORD=YourSecureAdminPassword123
mvn spring-boot:run
```

Backend starts on **http://localhost:8080**

- Swagger/OpenAPI: not included (add `springdoc-openapi-starter-webmvc-ui` if needed)
- Health check: `GET /actuator/health` (add `spring-boot-starter-actuator` if needed)
- Seeded admin: `admin@flightbooking.com` / `app.admin.password`

---

### 4. Frontend Configuration

```bash
cd ../flight-booking-app-frontend
cp .env.example .env
```

Edit `.env`:
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

### 5. Run Frontend

```bash
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**

---

### 6. Razorpay Webhook (Local Testing)

For the webhook to reach your local backend, use a tunnel:

```bash
# Option 1: ngrok
ngrok http 8080
# → copy the https URL, e.g. https://abcd-1234.ngrok-free.app

# Option 2: Cloudflare Tunnel
cloudflared tunnel --url http://localhost:8080
```

In **Razorpay Dashboard → Settings → Webhooks**:
- URL: `https://your-tunnel-url/api/payments/webhook`
- Secret: same as `app.razorpay.webhook-secret`
- Events: `payment.captured` (at minimum)

---

## 🧪 Running Tests

```bash
cd flight-booking-app-backend
mvn test
```

**25 tests** covering:
- AuthService lockout behavior (6 tests)
- FlightService search & seat map
- PaymentService webhook idempotency, order creation
- SeatLockingService concurrent locking
- Integration: flight search end-to-end

All tests use **H2 in-memory** database — no external DB required.

---

## 📦 Build for Production

### Backend (JAR)
```bash
cd flight-booking-app-backend
mvn clean package -DskipTests
# Output: target/flight-booking-api-1.0.0.jar
java -jar target/flight-booking-api-1.0.0.jar --spring.profiles.active=prod
```

### Frontend (Static Assets)
```bash
cd flight-booking-app-frontend
npm run build
# Output: dist/  →  serve with nginx, Apache, or any static host
```

---

## 🐳 Docker (Optional)

```dockerfile
# Dockerfile.backend
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY flight-booking-app-backend/ .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

```dockerfile
# Dockerfile.frontend
FROM node:20-alpine AS builder
WORKDIR /app
COPY flight-booking-app-frontend/ .
RUN npm ci && npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

---

## 📁 Project Structure Details

### Backend Packages
```
com.flightbooking
├── config/                 # SecurityConfig, CorsConfig, RazorpayConfig, DataSeeder
├── controller/
│   ├── AuthController.java         # POST /api/auth/{register,login,refresh,logout}
│   ├── BookingController.java      # POST/GET/DELETE /api/bookings
│   ├── FlightController.java       # GET /api/flights/{search,{id},{id}/seats}
│   ├── PaymentController.java      # POST /api/payments/{initiate/{ref},webhook}
│   ├── AdminController.java        # POST/GET/PATCH /api/admin/{flights,bookings}
│   └── ReferenceDataController.java# GET /api/{airports,airlines,aircraft}
├── dto/
│   ├── request/            # LoginRequest, RegisterRequest, BookingRequest, AddFlightRequest, etc.
│   └── response/           # ApiResponse<T>, AuthResponse, BookingResponse, FlightResponse, etc.
├── entity/                 # User, Flight, Seat, Booking, BookingPassenger, Payment, RefreshToken, Airport, Airline, Aircraft
├── exception/              # AccountLockedException, SeatUnavailableException, PaymentVerificationException, etc.
├── repository/             # Spring Data JPA interfaces + custom @Query methods
├── security/
│   ├── JwtTokenProvider.java        # JWT generation/validation
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter
│   ├── RateLimitFilter.java         # Bucket4j per-IP limiting
│   └── CustomUserDetailsService.java
├── service/                # AuthService, BookingService, FlightService, PaymentService, SeatLockingService, AdminFlightService, ScheduledTaskService
└── util/                   # BookingRefGenerator
```

### Frontend Routes
| Path | Component | Auth |
|------|-----------|------|
| `/` | `Home` | Public |
| `/search` | `SearchResults` | Public |
| `/login` | `Login` | Public |
| `/register` | `Register` | Public |
| `/book/:flightId/seats` | `SeatSelection` | Protected |
| `/book/:flightId/passengers` | `PassengerDetails` | Protected |
| `/book/payment/:bookingRef` | `Payment` | Protected |
| `/bookings` | `MyBookings` | Protected |
| `/bookings/:ref` | `BookingDetail` | Protected |
| `/admin` | `AdminDashboard` | Admin only |
| `*` | `NotFound` | Public |

### State Management
- **AuthContext** — user, tokens, login/register/logout, auto-refresh on 401
- **BookingDraftContext** — multi-step wizard state (flight, seats, passengers, search params)

---

## 🔧 Configuration Reference

### Backend (`application.properties` / `application-local.properties`)
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/flight_booking_db?...
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.open-in-view=false

# JWT
app.jwt.secret=${JWT_SECRET}                    # REQUIRED: base64, ≥32 bytes
app.jwt.access-token-expiry=900000              # 15 min (ms)
app.jwt.refresh-token-expiry=604800000          # 7 days (ms)

# CORS
app.cors.allowed-origins=http://localhost:5173

# Razorpay
app.razorpay.key-id=${RAZORPAY_KEY_ID}
app.razorpay.key-secret=${RAZORPAY_KEY_SECRET}
app.razorpay.webhook-secret=${RAZORPAY_WEBHOOK_SECRET}

# Admin seeding
app.admin.password=${ADMIN_PASSWORD}

# Logging
logging.level.com.flightbooking=DEBUG
logging.level.org.springframework.security=INFO
```

### Frontend (`.env`)
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 🌱 Seeded Data (on first run)

| Type | Count | Details |
|------|-------|---------|
| Airports | 26 | Major Indian airports across all regions (DEL, BOM, BLR, HYD, MAA, CCU, VNS, LKO, JAI, CHD, ATQ, DED, PNQ, AMD, GOI, IDR, UDR, COK, TRV, CJB, VGA, PAT, BBI, RPR, NAG, GAU, IXB) |
| Aircraft | 2 | Boeing 737 (144 seats), Airbus A320 (150 seats) |
| Airlines | 4 | Air India, IndiGo, Akasa Air, SpiceJet |
| Admin User | 1 | `admin@flightbooking.com` / `app.admin.password` |

---

## 📝 API Reference (Core Endpoints)

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | ❌ | Register new passenger |
| POST | `/api/auth/login` | ❌ | Login → returns access + refresh tokens |
| POST | `/api/auth/refresh` | ❌ | Refresh access token using refresh token |
| POST | `/api/auth/logout` | ✅ | Revoke all refresh tokens for user |

### Flights (Public)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/flights/search` | Search flights (query: origin, destination, date, passengers, seatClass) |
| GET | `/api/flights/{id}` | Flight details |
| GET | `/api/flights/{id}/seats` | Seat map for flight |
| GET | `/api/airports` | All airports |
| GET | `/api/airlines` | All airlines |
| GET | `/api/aircraft` | All aircraft |

### Bookings (Protected)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/bookings/initiate` | Lock seats + create PENDING booking |
| GET | `/api/bookings` | Current user's bookings |
| GET | `/api/bookings/{ref}` | Booking detail (owner or admin) |
| DELETE | `/api/bookings/{ref}/cancel` | Cancel + release seats |

### Payments
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/payments/initiate/{bookingRef}` | ✅ | Create Razorpay order for PENDING booking |
| POST | `/api/payments/webhook` | ❌ (public) | Razorpay callback — **only trusted confirmation path** |

### Admin (Admin only)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/admin/flights` | Create flight + auto-generate seat map |
| GET | `/api/admin/flights` | List all flights |
| GET | `/api/admin/bookings` | List all bookings |
| PATCH | `/api/admin/flights/{id}/status` | Update flight status (SCHEDULED/DELAYED/CANCELLED/COMPLETED) |

---

## 🐛 Known Limitations (Documented, Not Bugs)

1. **Pricing** — Total = `basePriceEconomy × passengerCount` regardless of seat class. Business seats show business fare on search but charge economy at payment.
2. **PENDING Cancellation** — Cancelling a PENDING booking releases seats but does **not** void the Razorpay order. If user pays after cancel, webhook will re-confirm.
3. **No Email/PDF** — E-tickets render in browser only; no SMTP or PDF generation.
4. **Webhook Async** — Frontend polls up to 60s; slow webhook = "still confirming" UI.
5. **Single-instance Rate Limiting** — Bucket4j uses in-memory `ConcurrentHashMap`. For clustered deploy, back with Redis (`bucket4j-redis`).

---

## 🗺 Roadmap Ideas

- [ ] Per-seat pricing (business vs economy)
- [ ] Razorpay order cancellation on booking cancel
- [ ] Email confirmations + PDF ticket generation
- [ ] Redis-backed rate limiting & distributed seat locking
- [ ] Flight status real-time updates (WebSocket / SSE)
- [ ] Passenger PNR lookup (guest access via booking ref + email)
- [ ] Multi-city / return trip booking
- [ ] Admin analytics dashboard (revenue, load factor, popular routes)

---

## 🤝 Contributing

1. Fork the repo
2. Create feature branch: `git checkout -b feat/amazing-feature`
3. Commit with conventional messages: `feat: add amazing feature`
4. Push and open PR

**Code style**: 
- Backend: Google Java Format (add plugin to IDE)
- Frontend: ESLint + Prettier (run `npm run lint` / `npm run format`)

---

## 🙏 Acknowledgments

- **Spring Boot** team for an excellent framework
- **Razorpay** for developer-friendly payment APIs
- **Bucket4j** for elegant rate limiting
- **React Router v6** & **Vite** for the frontend DX
- All open-source libraries listed in `pom.xml` and `package.json`

---

> **Built with care for learning and demonstration.** Not a production-ready airline system — but a solid foundation for one.
