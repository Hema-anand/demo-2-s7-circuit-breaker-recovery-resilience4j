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
Circuit Breaker ──── OPEN ────→ call blocked → 503 (exception handler)
      ↓
Event Service
      ↓
Failure → Fallback → 503
```

The goal is to observe how the Circuit Breaker reacts when Event Service fails and how it recovers when Event Service becomes available again.

---

## What Changes in This Demo

| File | Purpose |
|---|---|
| `client/EventClient.java` | Adds a fallback method for Event Service failures. |
| `config/CircuitBreakerEventLogger.java` | Logs Circuit Breaker state transitions. |
| `application.properties` | Enables Circuit Breaker health information through Actuator. |

`exception/GlobalExceptionHandler.java` is reused without changes. It already returns `404` for a missing Event and `503` when the Circuit Breaker is OPEN.

---

## Configuration Used in This Demo

The Retry and Circuit Breaker settings are the same as in Demo 1.

| Setting | Value |
|---|---|
| Retry maximum attempts | 3 |
| Retry wait between attempts | 1 second |
| Retry applies to | Event Service unavailable only |
| Circuit Breaker sliding window size | 5 calls |
| Minimum number of calls | 3 |
| Failure rate threshold | 50% |
| Wait duration in OPEN state | 10 seconds |
| Permitted calls in HALF-OPEN state | 2 |
| Circuit Breaker records | Event Service unavailable only |

Every Retry attempt is a separate call as far as the Circuit Breaker is concerned. Retry runs outside the Circuit Breaker, so a single booking request can add up to 3 calls to the Circuit Breaker's window.

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

## Step 2 – Verify a Missing Event

Keep Event Service running.

Send the same request with an Event ID that does not exist, for example `900000`.

Expected console output:

```text
Attempting Event Service call for eventId: 900000
```

Expected Postman response:

```text
404 Event Not Found
```

### Verified

- The call appears only once, so Retry is not triggered.
- No fallback line appears.
- A missing Event is a business error, not a service failure, so the Circuit Breaker does not count it.

---

## Step 3 – Stop Event Service

Stop only Event Service.

Keep Booking Service, Discovery Server and API Gateway running.

Send the booking request for Event ID `9`.

Expected console output:

```text
Attempting Event Service call for eventId: 9
Fallback executed: Event Service is unavailable.
Attempting Event Service call for eventId: 9
Fallback executed: Event Service is unavailable.
Attempting Event Service call for eventId: 9
Circuit Breaker State Changed: State transition from CLOSED to OPEN
Fallback executed: Event Service is unavailable.
```

Expected Postman response:

```text
503 Service Unavailable
```

Send the request again within 10 seconds.

Expected console output:

```text
Circuit Breaker OPEN: Event Service calls are temporarily blocked.
```

Expected Postman response:

```text
503 Service Unavailable
```

### Verified

- Event Service failures trigger Retry, and each failed attempt runs the fallback.
- The third failure reaches the minimum number of calls at a 100% failure rate, so the Circuit Breaker moves to OPEN during this first request.
- Booking is not created when the Event cannot be validated.
- While the circuit is OPEN, the call is blocked immediately: there is no `Attempting` line, no fallback and no retry.

---

## Step 4 – Observe OPEN State

While the circuit is OPEN, check Booking Service health:

```text
GET http://localhost:8083/actuator/health
```

The Circuit Breaker section should show:

```text
state: OPEN
status: CIRCUIT_OPEN
```
The parent `circuitBreakers` entry shows `UNKNOWN`, and the overall `status` stays `UP`. Spring Boot does not recognise `CIRCUIT_OPEN`, so an OPEN circuit does not mark the Booking Service itself as DOWN.

The health details may also show:

- failure rate
- failed calls
- buffered calls
- not permitted calls

---

## Step 5 – Observe HALF-OPEN State While Event Service Is Still Down

Keep Event Service stopped.

The Circuit Breaker stays OPEN for 10 seconds. It does not move to HALF-OPEN by itself: the next request after the wait triggers the transition.

Wait at least 10 seconds after the circuit opened, then send the booking request.

Expected console output:

```text
Circuit Breaker State Changed: State transition from OPEN to HALF_OPEN
Attempting Event Service call for eventId: 9
Fallback executed: Event Service is unavailable.
Attempting Event Service call for eventId: 9
Circuit Breaker State Changed: State transition from HALF_OPEN to OPEN
Fallback executed: Event Service is unavailable.
Circuit Breaker OPEN: Event Service calls are temporarily blocked.
```

Expected Postman response:

```text
503 Service Unavailable
```

### Verified

- HALF-OPEN allows a limited number of test calls (2 in this demo).
- Retry attempts are counted as those test calls, so both fail within this single request.
- HALF-OPEN does not automatically mean recovery. If Event Service is still unavailable, the Circuit Breaker returns to OPEN.
- The third attempt is blocked by the OPEN circuit and is not retried.

---

## Step 6 – Restart Event Service

Start Event Service again and confirm that it is registered in Eureka.

Wait until at least 10 seconds have passed since the circuit last went OPEN, then send the booking request.

Expected console output:

```text
Circuit Breaker State Changed: State transition from OPEN to HALF_OPEN
Attempting Event Service call for eventId: 9
```

The booking is created, but the circuit stays HALF-OPEN: only one of the two test calls has been used.

Send the booking request again.

Expected console output:

```text
Attempting Event Service call for eventId: 9
Circuit Breaker State Changed: State transition from HALF_OPEN to CLOSED
```

### Verified

- Event Service recovery is detected.
- Both test calls succeeded, so the Circuit Breaker moved back to CLOSED.
- Normal booking operations resume.

---

## Circuit Breaker State Flow

```text
CLOSED
  ↓
Failure rate reaches threshold
  ↓
OPEN
  ↓
Wait 10 seconds, then next request
  ↓
HALF-OPEN
  ↓
Test calls fail ─────────→ OPEN

HALF-OPEN
  ↓
Test calls succeed
  ↓
CLOSED
```

---

## Fallback Behaviour

The fallback handles Event Service failures only. It does not create fake Event data.

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

The fallback is matched by exception type. Other exceptions pass through it unchanged:

```text
Event not found            → no fallback → 404
Circuit Breaker is OPEN    → no fallback → 503 from the exception handler
```

A blocked call is rejected immediately, and it is not retried.

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
