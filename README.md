# Demo 2 – Observe Circuit Breaker Recovery and Graceful Degradation

This demo extends Demo 1 by adding fallback behaviour, Circuit Breaker state-transition logging and Actuator-based verification.

## Problem Statement

The Booking Service already uses Retry and Circuit Breaker to handle Event Service failures.

However, learners also need to understand what happens **after the circuit opens** and how the system recovers when Event Service becomes available again.

Enhance the Booking Service so that it:

* executes a controlled fallback when Event Service is unavailable,
* logs Circuit Breaker state transitions,
* shows how the circuit moves from `CLOSED → OPEN → HALF_OPEN → CLOSED`,
* returns a clear `503 Service Unavailable` response instead of an unhandled failure,
* verifies Circuit Breaker health using Spring Boot Actuator,
* resumes normal booking creation after Event Service recovers.

Verify the behavior by stopping and restarting Event Service and observing the Booking Service logs and Actuator health output.

## Demo Flow

```text
Booking Request
      ↓
Booking Service
      ↓
Retry
      ↓
Circuit Breaker
      ↓
Event Service
      ↓
Fallback when unavailable
```

The goal is to observe how the Circuit Breaker reacts when Event Service fails and how it recovers when Event Service becomes available again.

---

## What Changes in This Demo

| File | Purpose |
|---|---|
| `client/EventClient.java` | Adds fallback methods for Event Service failures and OPEN Circuit Breaker state. |
| `config/CircuitBreakerEventLogger.java` | Logs Circuit Breaker state transitions. |
| `application.properties` | Enables Circuit Breaker health information through Actuator. |
| `exception/GlobalExceptionHandler.java` | Returns clear `503 Service Unavailable` responses. |

---

## Start the Application

Start:

- PostgreSQL
- Discovery Server
- Event Service
- Booking Service
- API Gateway

Confirm that Event Service is registered in Eureka.

---

## Test Request

Use Postman:

**POST**

```text
http://localhost:8080/bookings
```

Request body:

```json
{
  "eventId": 9,
  "customerId": 101,
  "numberOfTickets": 2
}
```

Use an Event ID that exists.

---

## Step 1 – Verify Normal Behaviour

Keep Event Service running and send the booking request.

Expected console output:

```text
Attempting Event Service call for eventId: 9
```

Expected result:

```text
Booking created successfully.
Circuit Breaker remains CLOSED.
```

### Verified

- Event Service call succeeds.
- Booking is created.
- No fallback is required.

---

## Step 2 – Stop Event Service

Stop only Event Service.

Keep Booking Service, Discovery Server and API Gateway running.

Send the same booking request again.

Expected console output:

```text
Attempting Event Service call for eventId: 9
Fallback executed: Event Service is unavailable.
```

After repeated failures:

```text
Circuit Breaker State Changed: State transition from CLOSED to OPEN
```

Further requests may show:

```text
Fallback executed: Circuit Breaker is OPEN.
```

Expected Postman response:

```text
503 Service Unavailable
```

### Verified

- Event Service failures trigger fallback.
- Booking is not created when the Event cannot be validated.
- Repeated failures move the Circuit Breaker to OPEN.
- OPEN Circuit Breaker blocks unnecessary calls.

---

## Step 3 – Observe OPEN State

Check Booking Service health:

```text
GET http://localhost:8083/actuator/health
```

The Circuit Breaker section should show:

```text
state: OPEN
status: DOWN
```

The health details may also show:

- failure rate
- failed calls
- buffered calls
- not permitted calls

---

## Step 4 – Observe HALF-OPEN State

The Circuit Breaker is configured to remain OPEN for a short period.

After the wait duration, send another booking request.

Expected console output:

```text
Circuit Breaker State Changed: State transition from OPEN to HALF_OPEN
```

HALF-OPEN allows a limited number of test calls to check whether Event Service has recovered.

---

## Step 5 – Recovery Failure

If Event Service is still stopped during HALF-OPEN, the test calls fail.

Expected console output:

```text
Circuit Breaker State Changed: State transition from HALF_OPEN to OPEN
```

### Verified

HALF-OPEN does not automatically mean recovery.

If Event Service is still unavailable, the Circuit Breaker returns to OPEN.

---

## Step 6 – Restart Event Service

Start Event Service again.

Wait for the Circuit Breaker to allow HALF-OPEN test calls and send the booking request.

Expected console output:

```text
Circuit Breaker State Changed: State transition from OPEN to HALF_OPEN
Attempting Event Service call for eventId: 9
```

After successful test calls:

```text
Circuit Breaker State Changed: State transition from HALF_OPEN to CLOSED
```

The booking should now be created successfully again.

### Verified

- Event Service recovery is detected.
- Successful HALF-OPEN calls move the Circuit Breaker back to CLOSED.
- Normal booking operations resume.

---

## Circuit Breaker State Flow

```text
CLOSED
  ↓
Repeated failures
  ↓
OPEN
  ↓
Wait
  ↓
HALF-OPEN
  ↓
Failure ─────────→ OPEN

HALF-OPEN
  ↓
Successful test calls
  ↓
CLOSED
```

---

## Fallback Behaviour

The fallback does not create fake Event data.

If Event Service cannot be reached:

```text
Event validation fails
      ↓
Fallback executes
      ↓
Controlled 503 response
      ↓
Booking is not created
```

This prevents invalid bookings while keeping failure handling predictable.

---

## Verify Using Actuator

Use:

```text
GET http://localhost:8083/actuator/health
```

This provides Circuit Breaker health information such as:

```text
CLOSED    → service calls allowed
OPEN      → calls blocked
HALF_OPEN → test calls allowed
```

---

## Key Takeaway

A Circuit Breaker does more than detect failure.

It protects the Booking Service while Event Service is unavailable and automatically tests whether the downstream service has recovered.

```text
Failure
  ↓
Protection
  ↓
Fallback
  ↓
Recovery check
  ↓
Normal operation
```