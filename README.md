# Ride Matching Service

An in-memory Spring Boot service that matches riders to drivers. Drivers report their location and availability, a rider asking for a ride gets the nearest available driver (straight-line distance), and completing the ride puts that driver back into the pool. Everything is thread-safe without a global lock.

## How to run

Requires **JDK 21** and Maven 3.6.3+. If `mvn -version` shows an older Java, point `JAVA_HOME` at a JDK 21 first.

```bash
mvn test
```

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

### With Docker

No local JDK or Maven needed.

```bash
docker compose up --build        # build and run on http://localhost:8080
BACKEND_PORT=9090 docker compose up --build   # if 8080 is taken
docker compose down
```

To deploy, build and push the image; any container platform can run it:

```bash
docker build -t <registry>/ride-matching-service:<tag> .
docker push <registry>/ride-matching-service:<tag>
docker run -p 8080:8080 <registry>/ride-matching-service:<tag>
```

Profiles (`local` / `prod`), Docker and Google App Engine deployment are covered step by step in [DEPLOY.md](DEPLOY.md).

### Initial data

On startup `SeedDataConfig` loads `src/main/resources/data/drivers.json` into the in-memory
repositories through `DriverService`, so seeded drivers follow the same rules as ones registered over HTTP.
Each entry is `{ "id", "x", "y", "available" }`. Point `app.seed.drivers-file` at another file,
or set `app.seed.enabled=false` to start empty.

## API docs (Swagger)

With the app running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## API

| Method | Path | Body / params | Success | Errors |
|---|---|---|---|---|
| PUT | `/api/v1/drivers/{driverId}` | `{"x":1.0,"y":2.0,"available":true}` | 200 `DriverResponse` | 400, 409 `DRIVER_ON_RIDE` |
| PUT | `/api/v1/drivers/{driverId}/location` | `{"x":1.0,"y":2.0}` | 200 `DriverResponse` | 400, 404 |
| GET | `/api/v1/drivers/nearest` | `x`, `y`, `limit` (default 5, 1–100) | 200 `NearbyDriverResponse[]` | 400 |
| POST | `/api/v1/rides` | `{"riderId":"r1","pickup":{"x":0,"y":0}}` | 201 `RideResponse` + `Location` header | 400, 409 `NO_DRIVER_AVAILABLE` / `RIDER_HAS_ACTIVE_RIDE` |
| POST | `/api/v1/rides/{rideId}/complete` | `{"riderId":"r1"}` | 200 `RideResponse` | 400, 403, 404, 409 `RIDE_ALREADY_COMPLETED` |
| GET | `/api/v1/rides/{rideId}` | – | 200 `RideResponse` | 404 |

Register a driver, or change their location and availability:

```bash
curl -X PUT localhost:8080/api/v1/drivers/d1 -H 'Content-Type: application/json' -d '{"x":3,"y":4,"available":true}'
```

Move a driver without touching availability (works while on a ride):

```bash
curl -X PUT localhost:8080/api/v1/drivers/d1/location -H 'Content-Type: application/json' -d '{"x":5,"y":5}'
```

Nearest available drivers:

```bash
curl 'localhost:8080/api/v1/drivers/nearest?x=0&y=0&limit=3'
```

Request a ride:

```bash
curl -X POST localhost:8080/api/v1/rides -H 'Content-Type: application/json' -d '{"riderId":"r1","pickup":{"x":0,"y":0}}'
```

Complete a ride:

```bash
curl -X POST localhost:8080/api/v1/rides/<rideId>/complete -H 'Content-Type: application/json' -d '{"riderId":"r1"}'
```

Fetch a ride:

```bash
curl localhost:8080/api/v1/rides/<rideId>
```

Every error has the same shape:

```json
{"code":"RIDE_ALREADY_COMPLETED","message":"Ride 42 is already completed","timestamp":"2026-01-01T09:00:00Z"}
```

## Design

```
api  ──►  api.impl  ──►  service (interfaces)  ──►  service.impl
                                                        │
                                     ┌────────────-─────┼──────────────────┐
                                     ▼                  ▼                  ▼
                               repository          matching            domain
                          (DriverRepository,   (DriverMatchingStrategy,  (Driver, Ride,
                           RideRepository)      DistanceCalculator)       DriverState…)
```

| Package | Responsibility |
|---|---|
| `api` | `DriverController`, `RideController` interfaces (routes + validation); `GlobalExceptionHandler`. |
| `api.impl` | `DriverControllerImpl`, `RideControllerImpl` — HTTP ↔ service translation, DTO mapping. No business rules. |
| `service` | `DriverService`, `RideService` interfaces; `ActiveRiderRegistry`; `IdGenerator`. |
| `service.impl` | `DriverServiceImpl`, `RideServiceImpl` — all business rules; `UuidIdGenerator`. |
| `matching` | `DistanceCalculator` / `DriverMatchingStrategy` interfaces and `RankedDriver`. |
| `matching.impl` | `EuclideanDistanceCalculator`, `NearestDriverMatchingStrategy`. |
| `repository` | `DriverRepository` / `RideRepository` interfaces. |
| `repository.impl` | `InMemoryDriverRepository`, `InMemoryRideRepository` on `ConcurrentHashMap`. |
| `domain` | `Driver` and `Ride` own their state transitions atomically. |
| `exception` | Business exceptions, all extending `RideMatchingException`. No HTTP knowledge. |

**SOLID in practice**

- *Single responsibility*: controllers map, services decide, repositories store, the calculator measures, the registry enforces "one ride per rider", the exception handler owns HTTP status codes.
- *Open/closed*: a grid or k-d tree matcher can replace `NearestDriverMatchingStrategy` without touching `RideServiceImpl`.
- *Liskov*: any `DriverRepository` works where `InMemoryDriverRepository` does; tests rely on that.
- *Interface segregation*: small, separate repository interfaces.
- *Dependency inversion*: services depend on `DriverRepository`, `RideRepository`, `DriverMatchingStrategy`, `IdGenerator` and `Clock`, all constructor-injected.

Patterns: Strategy (matching, distance), Repository, State (guarded status transitions), DTO with static `from(...)` mappers.

**Request-a-ride flow**

```
POST /api/v1/rides → RideControllerImpl → RideServiceImpl.requestRide
  1. ActiveRiderRegistry.tryRegister(riderId)      false → RiderHasActiveRideException (409)
  2. DriverRepository.findAvailable()              point-in-time copy
  3. DriverMatchingStrategy.rank(pickup, drivers)  one snapshot per driver, then sort
  4. driver.tryReserve() down the ranked list      first success wins
  5. nobody reserved → NoDriverAvailableException  (409)
  6. Ride.start(...), save, return 201
  Any failure after step 1 unregisters the rider; any failure after step 4 releases the driver.
```

## Concurrency

| Race | Protection |
|---|---|
| Two requests pick the same driver | `Driver.tryReserve()` is a compare-and-set. The loser moves to its next candidate. |
| A location update races a reservation | `tryReserve()` retries while the driver is still `AVAILABLE`, so a moving driver is not skipped. |
| Driver toggles availability while on a ride | `updateAvailability` refuses while `ON_RIDE` (409). Location-only updates stay allowed. |
| Same ride completed twice | `Ride.markCompleted()` CAS `IN_PROGRESS → COMPLETED`; only the winner releases the driver. |
| Same rider requests twice at once | `ActiveRiderRegistry` is a `ConcurrentHashMap.newKeySet()`; `add()` is the atomic claim. |
| Driver moves during sorting | Each driver's state is read once before sorting; the comparator never reads live state. |
| Duplicate driver registration | `ConcurrentHashMap.computeIfAbsent`; the loser falls through to a normal update. |

`RideServiceConcurrencyTest` covers each of these with a 32-thread pool, a `CountDownLatch` start gate and `@RepeatedTest(20)`. As a check, replacing the CAS in `tryReserve` with a plain `set` makes `neverAssignsSameDriverTwice` fail on every repetition.

## Assumptions

| # | Decision | Reason |
|---|---|---|
| A1 | Spring Boot 4.1.1 REST API, Java 21, Maven. | "Java backend" usually implies an HTTP service. |
| A2 | Locations are `(x, y)` doubles on a flat plane; NaN/infinite values are rejected. | Needed for Euclidean distance. |
| A3 | Riders are identified by a `riderId` string; no rider registration. | The brief does not cover rider management. |
| A4 | One active ride per rider. | Stops one rider holding several drivers. |
| A5 | A ride is `IN_PROGRESS` as soon as it is allocated. | The rider gets ride details on request. |
| A6 | Driver statuses `AVAILABLE`, `OFFLINE`, `ON_RIDE`. On a ride, availability cannot change (409), but location can through `PUT /api/v1/drivers/{id}/location`. | A driver on a ride must not be matched again, yet should still report position. |
| A7 | Only the requesting rider can complete a ride (403 otherwise). | The brief says "the rider" completes it. |
| A8 | Equal distances are ordered by driver ID. | Deterministic results and tests. |
| A9 | "No driver available" is 409 Conflict. | Judgment call; 503 is also defensible. |
## Trade-offs and next steps

- **O(n log n) scan per request.** Fine for an in-memory fleet; a grid, geohash or k-d tree strategy can sit behind `DriverMatchingStrategy` for large fleets.
- **Weakly consistent reads.** `/api/v1/drivers/nearest` may list a driver who is reserved a millisecond later.
- **Drivers may move between ranking and reservation.** The rider still gets a driver who was nearest at ranking time.
- **Completed rides live in memory forever.** Needs eviction or persistence.
- **Persistence** would move the CAS logic into optimistic locking / conditional updates in the database.
- **Completion timestamp.** A reader can briefly see `COMPLETED` with `completedAt` still null; storing status and timestamp in one atomic record would close that gap.
