package com.flightbooking.config;

import com.flightbooking.entity.Airline;
import com.flightbooking.repository.AirlineRepository;
import com.flightbooking.entity.Aircraft;
import com.flightbooking.entity.Airport;
import com.flightbooking.entity.User;
import com.flightbooking.repository.AircraftRepository;
import com.flightbooking.repository.AirportRepository;
import com.flightbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs once when the app starts. Seeds reference data so you can
// test the API immediately without manually inserting rows.
// Safe to run multiple times — checks if data already exists first.

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAirports();
        seedAircraft();
        seedAirlines();
        seedAdminUser();
    }

    // Expanded from the original 6 metro airports to 26 major Indian airports
    // spanning every region of the country — north, south, east, west, and
    // northeast — so route search isn't limited to a handful of metro pairs.
    private void seedAirports() {
        if (airportRepository.count() > 0) return;

        // Metro / Tier-1
        airportRepository.save(Airport.builder().code("DEL").name("Indira Gandhi International Airport").city("Delhi").country("India").build());
        airportRepository.save(Airport.builder().code("BOM").name("Chhatrapati Shivaji Maharaj International Airport").city("Mumbai").country("India").build());
        airportRepository.save(Airport.builder().code("HYD").name("Rajiv Gandhi International Airport").city("Hyderabad").country("India").build());
        airportRepository.save(Airport.builder().code("BLR").name("Kempegowda International Airport").city("Bangalore").country("India").build());
        airportRepository.save(Airport.builder().code("MAA").name("Chennai International Airport").city("Chennai").country("India").build());
        airportRepository.save(Airport.builder().code("CCU").name("Netaji Subhas Chandra Bose International Airport").city("Kolkata").country("India").build());

        // North India
        airportRepository.save(Airport.builder().code("VNS").name("Lal Bahadur Shastri Airport").city("Varanasi").country("India").build());
        airportRepository.save(Airport.builder().code("LKO").name("Chaudhary Charan Singh International Airport").city("Lucknow").country("India").build());
        airportRepository.save(Airport.builder().code("JAI").name("Jaipur International Airport").city("Jaipur").country("India").build());
        airportRepository.save(Airport.builder().code("CHD").name("Chandigarh International Airport").city("Chandigarh").country("India").build());
        airportRepository.save(Airport.builder().code("ATQ").name("Sri Guru Ram Dass Jee International Airport").city("Amritsar").country("India").build());
        airportRepository.save(Airport.builder().code("DED").name("Jolly Grant Airport").city("Dehradun").country("India").build());

        // West India
        airportRepository.save(Airport.builder().code("PNQ").name("Pune Airport").city("Pune").country("India").build());
        airportRepository.save(Airport.builder().code("AMD").name("Sardar Vallabhbhai Patel International Airport").city("Ahmedabad").country("India").build());
        airportRepository.save(Airport.builder().code("GOI").name("Goa International Airport (Dabolim)").city("Goa").country("India").build());
        airportRepository.save(Airport.builder().code("IDR").name("Devi Ahilyabai Holkar Airport").city("Indore").country("India").build());
        airportRepository.save(Airport.builder().code("UDR").name("Maharana Pratap Airport").city("Udaipur").country("India").build());

        // South India
        airportRepository.save(Airport.builder().code("COK").name("Cochin International Airport").city("Kochi").country("India").build());
        airportRepository.save(Airport.builder().code("TRV").name("Trivandrum International Airport").city("Thiruvananthapuram").country("India").build());
        airportRepository.save(Airport.builder().code("CJB").name("Coimbatore International Airport").city("Coimbatore").country("India").build());
        airportRepository.save(Airport.builder().code("VGA").name("Vijayawada Airport").city("Vijayawada").country("India").build());

        // East & Central India
        airportRepository.save(Airport.builder().code("PAT").name("Jay Prakash Narayan International Airport").city("Patna").country("India").build());
        airportRepository.save(Airport.builder().code("BBI").name("Biju Patnaik International Airport").city("Bhubaneswar").country("India").build());
        airportRepository.save(Airport.builder().code("RPR").name("Swami Vivekananda Airport").city("Raipur").country("India").build());
        airportRepository.save(Airport.builder().code("NAG").name("Dr. Babasaheb Ambedkar International Airport").city("Nagpur").country("India").build());

        // Northeast India
        airportRepository.save(Airport.builder().code("GAU").name("Lokpriya Gopinath Bordoloi International Airport").city("Guwahati").country("India").build());
        airportRepository.save(Airport.builder().code("IXB").name("Bagdogra Airport").city("Siliguri").country("India").build());

        log.info("Seeded 26 airports across India");
    }

    private void seedAircraft() {
        if (aircraftRepository.count() > 0) return;

        aircraftRepository.save(Aircraft.builder()
                .model("Boeing 737")
                .totalSeats(144)
                .economySeats(126)
                .businessSeats(18)
                .build());

        aircraftRepository.save(Aircraft.builder()
                .model("Airbus A320")
                .totalSeats(150)
                .economySeats(138)
                .businessSeats(12)
                .build());

        log.info("Seeded 2 aircraft types");
    }

    private void seedAirlines() {
        if (airlineRepository.count() > 0) return;

        airlineRepository.save(
                Airline.builder()
                        .name("Air India")
                        .code("AI")
                        .country("India")
                        .logoUrl("/logos/airindia.png")
                        .build());

        airlineRepository.save(
                Airline.builder()
                        .name("IndiGo")
                        .code("6E")
                        .country("India")
                        .logoUrl("/logos/indigo.png")
                        .build());

        airlineRepository.save(
                Airline.builder()
                        .name("Akasa Air")
                        .code("QP")
                        .country("India")
                        .logoUrl("/logos/akasa.png")
                        .build());

        airlineRepository.save(
                Airline.builder()
                        .name("SpiceJet")
                        .code("SG")
                        .country("India")
                        .logoUrl("/logos/spicejet.png")
                        .build());

        log.info("Seeded 4 airlines");
    }

    private void seedAdminUser() {
        if (userRepository.existsByEmail("admin@flightbooking.com")) return;

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin password not configured via app.admin.password - skipping admin user creation");
            return;
        }

        User admin = User.builder()
                .fullName("System Admin")
                .email("admin@flightbooking.com")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("Seeded admin user: admin@flightbooking.com (password from env)");
    }
}
