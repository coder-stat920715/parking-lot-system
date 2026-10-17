# Parking Lot Management System - Spring Cloud microservices

| Module | Port | Role |
|---|---|---|
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | Single entry point: routing (`lb://`), global logging filter, Redis rate limiter, circuit breaker + fallback |
| `spot-allocation-service` | 8081, 8091, ... | Spot inventory + thread-safe reservation (Redis lock + `@Version`), Strategy pattern |
| `parking-ticket-service` | 8082 | Entry/exit workflow, fee Strategy pattern, Feign + Resilience4j to spot service |
| `payment-service` | 8083 | Simulated PSP, updates ticket status through the ticket service |

```
client -> api-gateway:8080 --lb--> parking-ticket-service:8082 --Feign+LB--> spot-allocation-service (8081 | 8091)
                           \--lb--> payment-service:8083 --Feign+LB--> parking-ticket-service
                           \--lb--> spot-allocation-service (GET /api/v1/spots/available only)
all services register with eureka-server:8761 ; redis:6379 = distributed lock + gateway rate limiter
```

Public API (through the gateway): `POST /api/v1/tickets/entry`, `POST /api/v1/tickets/exit`,
`GET /api/v1/tickets/{n}`, `GET /api/v1/spots/available`, `POST /api/v1/payments`, `GET /api/v1/payments/{ref}`.
`/internal/**` endpoints (allocate, release, mark-paid) are service-to-service only and are **not** routed by the gateway.

## Prerequisites
JDK 17+, Maven 3.9+, `curl` + `jq`, Docker (only for Redis).

## Build and run
```bash
mvn clean package                    # runs the unit tests too
docker compose up -d redis           # Redis: distributed lock + rate limiter
./scripts/start-all.sh               # starts everything in order (logs in ./logs)
```
Manual order: Eureka -> spot-allocation (8081, wait until healthy, then 8091) -> ticket -> payment -> gateway:
```bash
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar spot-allocation-service/target/spot-allocation-service-1.0.0.jar
java -jar spot-allocation-service/target/spot-allocation-service-1.0.0.jar --server.port=8091
java -jar parking-ticket-service/target/parking-ticket-service-1.0.0.jar
java -jar payment-service/target/payment-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```
Start the second spot instance only after the first is up (the first seeds the 45 spots).
Eureka dashboard: http://localhost:8761. Reset data: stop services, `./scripts/reset-data.sh`.
Run the whole scenario: `./scripts/demo.sh`.

## curl collection
```bash
GW=http://localhost:8080

# availability
curl -s "$GW/api/v1/spots/available" | jq
curl -s "$GW/api/v1/spots/available?vehicleType=EV_CAR&floor=1" | jq

# 1. entry (gateway -> ticket-service -> load-balanced spot-allocation)
curl -s -X POST $GW/api/v1/tickets/entry -H 'Content-Type: application/json' \
  -d '{"licensePlate":"KA-01-AB-1234","vehicleType":"CAR","strategy":"NEAREST"}' | jq
#    "spotServiceInstance" shows which instance served the allocation

# 2. concurrent entries: no spot may appear twice; instances alternate (round robin)
seq 1 20 | xargs -P 20 -I{} sh -c "curl -s -X POST $GW/api/v1/tickets/entry -H 'Content-Type: application/json' \
  -d '{\"licensePlate\":\"CC-{}\",\"vehicleType\":\"CAR\"}' | jq -r '[.spotCode,.spotServiceInstance]|@tsv'" | sort
#    (add | cut -f1 | sort | uniq -d to prove there are no duplicates)

# 3. exit (demo switch simulates a 3h10m stay) then pay
TICKET=TKT-...   # from step 1
curl -s -X POST $GW/api/v1/tickets/exit -H 'Content-Type: application/json' \
  -d "{\"ticketNumber\":\"$TICKET\",\"simulatedDurationMinutes\":190}" | jq
curl -s -X POST $GW/api/v1/payments -H 'Content-Type: application/json' \
  -d "{\"ticketNumber\":\"$TICKET\",\"method\":\"CARD\",\"paymentToken\":\"tok_ok\"}" | jq     # 200
#   paymentToken "tok_decline" -> HTTP 402
curl -s $GW/api/v1/tickets/$TICKET | jq '{status,spotReleased}'                               # PAID, true

# 4. rate limit (spots route: 10 req/s, burst 20) -> expect 429s
for i in $(seq 1 60); do curl -s -o /dev/null -w "%{http_code}\n" $GW/api/v1/spots/available & done | sort | uniq -c

# 5. fail-over: kill the :8081 instance, keep sending entries -> still 201 (Resilience4j retry re-balances to :8091)
```

## Design notes
**Double-booking protection.** (1) A Redis lock (`SET NX PX` + Lua compare-and-delete) serialises allocation per
vehicle type across all instances, acquired *outside* the DB transaction and released after commit. (2) `@Version`
optimistic locking on `ParkingSpot` is the safety net (cross-type overlap, lease expiry, Redis down -> automatic
fallback to a JVM-local `ReentrantLock`); the loser retries. A JVM-local lock alone could not protect two instances.

**Shared database.** The demo uses one H2 *file* database opened in `AUTO_SERVER` mode so all spot instances see the same
state (private in-memory DBs would make load balancing meaningless). Works on one machine only; use PostgreSQL/MySQL
in production (no code change besides the datasource).

**Idempotency + consistency.** Allocate/release are keyed by ticket number, so retries are safe (Resilience4j `@Retry`
re-balances to another instance = fail-over). Failed ticket persistence triggers a compensating release. DB unique keys
enforce "one open ticket per vehicle" and "one in-flight/successful payment per ticket". If the spot release or the
ticket update fails after a successful payment, scheduled reconcilers retry (`spotReleased` / `ticketSynced` flags).

**Resilience.** Gateway: CircuitBreaker filter with `/fallback/**`, RequestRateLimiter (Redis; fails open if Redis is
down). Services: Resilience4j circuit breaker + retry around Feign; 4xx business errors are mapped to exceptions and
never trip the breaker. Ticket entry is also guarded by a Resilience4j `@RateLimiter`.

**Service ids.** Eureka ids are the upper-cased application names: `PARKING-TICKET-SERVICE`, `SPOT-ALLOCATION-SERVICE`,
`PAYMENT-SERVICE` (used in the `lb://` routes and `@FeignClient` names).

## Before production
Authentication/authorization (JWT at the gateway, mTLS or network policy for `/internal/**`), real database + Flyway,
Redis Sentinel/Cluster (or Redisson), a real PSP with webhooks, distributed tracing (Micrometer Tracing + Zipkin),
config server/secrets, Eureka with self-preservation and security enabled, and integration tests (Testcontainers).
Remove `parking.demo.allow-simulated-duration`.
