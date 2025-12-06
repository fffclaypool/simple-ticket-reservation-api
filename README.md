# Ticket Reservation System REST API

A REST API for a ticket reservation system built with Java 17 and Spring Boot 3.2.

## Requirements

- Java 17 or higher
- Gradle 8.5 or higher (not required if using Gradle Wrapper)
- Docker & Docker Compose (for DevContainer development)

## Running the Application

### DevContainer Mode (PostgreSQL + Redis) - Recommended

The recommended way to develop is using VS Code DevContainers with Docker Compose. This provides:
- PostgreSQL 15 database
- Redis 7 for caching
- Pre-configured Java development environment

1. Open the project in VS Code
2. Click "Reopen in Container" when prompted (or use Command Palette: "Dev Containers: Reopen in Container")
3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

The `docker` profile is automatically activated in DevContainer.

### Test Mode (H2 Database)

For local testing without Docker:

```bash
./gradlew bootRun --args='--spring.profiles.active=test'
```

The application will start at `http://localhost:8080`.

H2 Database Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ticketdb`
- Username: `sa`
- Password: (leave empty)

### Profiles

| Profile | Database | Cache | Usage |
|---------|----------|-------|-------|
| `docker` | PostgreSQL | Redis | DevContainer development (default) |
| `test` | H2 | None | Local testing without Docker |
| `ci` | H2 | None | GitHub Actions CI/CD |

### Docker Compose Services

When using DevContainer, the following services are started:

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| `app` | Custom | - | Java development container |
| `postgres` | postgres:15 | 5432 | PostgreSQL database |
| `redis` | redis:7 | 6379 | Redis cache |

## API Endpoints

### Event Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Get all events |
| GET | `/api/events/{id}` | Get event by ID |
| GET | `/api/events/available` | Get available events |
| GET | `/api/events/search?name={name}` | Search events by name |
| POST | `/api/events` | Create event |
| PUT | `/api/events/{id}` | Update event |
| DELETE | `/api/events/{id}` | Delete event |

### Ticket Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tickets` | Get all tickets |
| GET | `/api/tickets/{id}` | Get ticket by ID |
| GET | `/api/tickets/code/{code}` | Search by ticket code |
| GET | `/api/tickets/email/{email}` | Search by email address |
| GET | `/api/events/{eventId}/tickets` | Get tickets for an event |
| POST | `/api/events/{eventId}/tickets` | Create ticket (with pessimistic locking) |
| PATCH | `/api/tickets/{id}/cancel` | Cancel ticket |

## Usage Examples

### Create Event

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Concert 2024",
    "description": "Outdoor summer concert",
    "venue": "Tokyo Dome",
    "eventDate": "2024-08-15T18:00:00",
    "totalSeats": 500,
    "price": 8000
  }'
```

### Get All Events

```bash
curl http://localhost:8080/api/events
```

### Create Ticket

```bash
curl -X POST http://localhost:8080/api/events/1/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "numberOfSeats": 2
  }'
```

This endpoint uses pessimistic locking to prevent overbooking when multiple concurrent requests are made.

### Search by Ticket Code

```bash
curl http://localhost:8080/api/tickets/code/TKT-XXXXXXXX
```

### Cancel Ticket

```bash
curl -X PATCH http://localhost:8080/api/tickets/1/cancel
```

## Caching

In the `docker` profile, Redis caching is enabled for improved performance:

- **Events**: Cached on read, invalidated on create/update/delete
- **Tickets**: Not cached (ticket creation/cancellation invalidates related event cache)

Cache is disabled in `test` and `ci` profiles for simpler testing.

## Health Check

Health check endpoint provided by Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health
```

## Build

```bash
./gradlew build
```

The executable JAR file will be generated at `build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar`.

```bash
java -jar build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar
```

## Test

```bash
./gradlew test
```

## Load Testing

JMeter is used to verify that pessimistic locking prevents overbooking under concurrent load.

### Requirements

- JMeter 5.6.3 or higher

### Run Load Test

1. Start the application with `test` or `ci` profile (uses H2 database):
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=test'
   ```

2. Run JMeter test (in another terminal):
   ```bash
   jmeter -n -t jmeter/ticket_booking_load_test.jmx -l results.jtl
   ```

The test creates an event with 10 seats and sends 20 concurrent booking requests. With pessimistic locking, exactly 10 bookings succeed and 10 fail with "No seats available".

Note: CI/CD uses the `ci` profile which automatically disables Redis caching.

## Code Quality

### Formatter (Spotless)

```bash
# Check format
./gradlew spotlessCheck

# Apply format
./gradlew spotlessApply
```

### Linter (Checkstyle)

```bash
./gradlew checkstyleMain checkstyleTest
```

## CI/CD

GitHub Actions workflow runs on push/PR to main:

1. **lint**: Code format check (Spotless) and static analysis (Checkstyle)
2. **test**: Unit and integration tests
3. **build**: Build application JAR
4. **load-test**: JMeter load test to verify no overbooking
