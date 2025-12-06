# Ticket Reservation System REST API

A REST API for a ticket reservation system built with Java 17 and Spring Boot 3.2.

## Requirements

- Java 17 or higher
- Gradle 8.5 or higher (not required if using Gradle Wrapper)

## Setup

```bash
cd ticket-reservation-api
```

## Running the Application

### Development Mode (H2 Database)

```bash
./gradlew bootRun
```

The application will start at `http://localhost:8080`.

H2 Database Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ticketdb`
- Username: `sa`
- Password: (leave empty)

### Production Mode (PostgreSQL)

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

Set the database connection information via environment variables:
- `DB_USERNAME`: PostgreSQL username (default: postgres)
- `DB_PASSWORD`: PostgreSQL password (default: postgres)

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

### Reservation Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/reservations` | Get all reservations |
| GET | `/api/reservations/{id}` | Get reservation by ID |
| GET | `/api/reservations/code/{code}` | Search by reservation code |
| GET | `/api/reservations/email/{email}` | Search by email address |
| GET | `/api/reservations/event/{eventId}` | Search by event ID |
| POST | `/api/reservations` | Create reservation |
| PATCH | `/api/reservations/{id}/cancel` | Cancel reservation |

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

### Create Reservation

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "numberOfSeats": 2
  }'
```

### Search by Reservation Code

```bash
curl http://localhost:8080/api/reservations/code/RES-XXXXXXXX
```

### Cancel Reservation

```bash
curl -X PATCH http://localhost:8080/api/reservations/1/cancel
```

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
