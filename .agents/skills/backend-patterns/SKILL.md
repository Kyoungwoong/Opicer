---
name: backend-patterns
description: Backend architecture patterns, REST API design, and server-side best practices for Java (Spring Boot + Spring Data JPA).
version: 1.0
license: MIT
compatibility: Java 17+; Spring Boot (Web + Validation + Data/JDBC) recommended
metadata:
  category: backend
  priority: high
  agent-safe: true
allowed-tools: filesystem shell
---

# Backend Development Patterns (Java / Spring Boot)

This document describes backend patterns and best practices optimized for:
- clean, reviewable architecture
- testability (TDD-friendly)
- REST APIs with Spring Boot + Spring Data JPA
- avoiding overspecification unless explicitly required by `Opicer.md` or user instructions

Rule of thumb:
- Prefer simple, standard patterns first.
- Apply advanced patterns ONLY when the requirements explicitly justify them.

---

## 0. Architecture Approach (Recommended)

Start with a clean, layered structure. If the system grows or requires strict boundaries,
adopt a Hexagonal (Ports & Adapters) style. Both are compatible with Spring Boot.

### 0.1 Clean Layered (Default)
- Controller (HTTP) -> Service (use case) -> Repository (data access)
- Keep domain rules in service/domain, not in controllers.
- Use DTOs at the boundary; do not expose JPA entities directly.

### 0.2 Hexagonal (When Boundaries Matter)
Use when:
- multiple input adapters (REST + batch + messaging)
- multiple output adapters (DB + external APIs)
- higher testability/replaceability is needed

Structure (example):
```
com.opicer
  domain/            # entities, value objects, domain services
  application/       # use cases (application services)
  ports/
    in/              # use case interfaces
    out/             # repository/external client interfaces
  adapters/
    in/web/          # REST controllers, request/response DTOs
    out/persistence/ # JPA entities + Spring Data repositories
  config/            # Spring configuration
```

Rules:
- `ports` are pure Java interfaces (no Spring annotations required).
- `adapters` depend on Spring/JPA; domain does not.
- Controllers only call `ports.in` (use case interfaces).
- JPA repositories implement `ports.out` via adapter classes.

---

## 1. API Design Patterns

### 1.1 RESTful API Structure

Use resource-based URLs and HTTP verbs.

```http
GET    /api/markets            # List
GET    /api/markets/{id}       # Get
POST   /api/markets            # Create
PUT    /api/markets/{id}       # Replace (full update)
PATCH  /api/markets/{id}       # Partial update
DELETE /api/markets/{id}       # Delete
```
Use query parameters for filtering/sorting/pagination.

```http
GET /api/markets?status=active&sort=volume,desc&limit=20&offset=0
```

MUST:
- Validate pagination params (limit upper bound)
- Use stable ordering when paginating

---

## 2. Repository Pattern (Data Access Separation)

### 2.1 Repository Interface (Domain-Facing)
Repository hides data-source details (JPA/JDBC/external API).
```java
public interface MarketRepository {
    List<Market> findAll(MarketFilters filters);
    Optional<Market> findById(String id);
    Market save(Market market);
    void deleteById(String id);
}
```
If using Spring Data JPA, this MAY be implemented by JpaRepository adapters.
If using JDBC, implement manually.

MUST:
- Keep repositories free of business rules 
- Do not put HTTP concerns in repositories

---

## 3. Service Layer Pattern (Business Logic)
Services hold business rules and orchestrate repositories.

```java
@Service
public class MarketService {
    private final MarketRepository marketRepository;

    public MarketService(MarketRepository marketRepository) {
        this.marketRepository = marketRepository;
    }

    public List<Market> listMarkets(MarketFilters filters) {
        return marketRepository.findAll(filters);
    }

    public Market getMarket(String id) {
        return marketRepository.findById(id)
            .orElseThrow(() -> new ApiException(404, "Market not found"));
    }
}
```

MUST:
- Keep services deterministic and testable 
- Put domain rules here (not in controller, not in repository)

---

## 4. Controller Pattern (HTTP Boundary)
Controllers parse requests, validate inputs, call services, map responses.

```java
@RestController
@RequestMapping("/api/markets")
public class MarketController {
    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping
    public List<MarketResponse> list(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        MarketFilters filters = new MarketFilters(status, limit, offset);
        return marketService.listMarkets(filters).stream()
            .map(MarketResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public MarketResponse get(@PathVariable String id) {
        return MarketResponse.from(marketService.getMarket(id));
    }
}

```

MUST:
- Validate and clamp limit (e.g., max 100)
- Keep controllers thin

---

## 5. Validation Pattern (Java Bean Validation)

Validate at the HTTP boundary using @Valid and constraints.
```java
public record CreateMarketRequest(
    @NotBlank String name,
    @NotNull @PositiveOrZero Integer volume
) {}
```

```java
@PostMapping
public MarketResponse create(@Valid @RequestBody CreateMarketRequest req) {
    Market created = marketService.create(req);
    return MarketResponse.from(created);
}
```

MUST:
- Do not trust raw inputs 
- Return 400 on validation errors

---

## 6. Error Handling Pattern (Centralized)
Use a consistent error response and centralized exception handling.

```java
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}

```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus())
            .body(new ErrorResponse(false, e.getMessage(), "API_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse(false, "Validation failed", "VALIDATION_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // Log unexpected errors
        System.err.println("Unexpected error: " + e.getMessage());
        return ResponseEntity.status(500)
            .body(new ErrorResponse(false, "Internal server error", "INTERNAL_ERROR"));
    }
}

```

MUST:
- Never swallow errors silently 
- Unexpected errors -> 500 + logging

---

## 7. Database Patterns

### 7.1 Query Optimization
MUST:
- Fetch only needed columns for list endpoints 
- Avoid returning huge payloads by default 
- If using JPA, prefer DTO projections for list views.

### 7.2 N+1 Query Prevention
MUST:
- Avoid lazy-loading loops in list endpoints 
- Use joins / fetch joins / batch fetching when needed
Example rule:
- If you render Market with Creator, do not call creatorRepository.findById() per item.

---

## 8. Transaction Pattern (Recommended When Needed)

Transaction design often improves correctness and review quality.
However, transactions are not “always required”.

### When to apply transactions
MUST apply a transaction if:

- A use case performs multiple writes that must succeed or fail together <br/> (e.g., create market + create initial position)
- You must enforce invariants across multiple tables/aggregates

MUST NOT force transactions if:
- The use case is a single atomic write 
- Opicer.md or the user explicitly expects eventual consistency or async processing

### Spring Transaction Example
```java
@Service
public class MarketWriteService {
    private final MarketRepository marketRepository;
    private final PositionRepository positionRepository;

    public MarketWriteService(MarketRepository marketRepository,
                              PositionRepository positionRepository) {
        this.marketRepository = marketRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public Market createMarketWithPosition(CreateMarketRequest req, CreatePositionRequest posReq) {
        Market market = Market.create(req);
        Market saved = marketRepository.save(market);

        Position position = Position.create(saved.getId(), posReq);
        positionRepository.save(position);

        return saved;
    }
}

```

Notes:
- Transactions improve correctness, but only when the use case needs atomicity.
- If requirements mention “atomic”, “rollback”, “consistency”, “all-or-nothing”, this pattern SHOULD be applied.

---

## 9. Optional Patterns (ONLY if requirements require)
The following patterns MAY look impressive, but are commonly overspec in assessments.
Apply them ONLY when requirements explicitly demand them.

### 9.1 Caching (Redis, Cache-Aside)
ONLY apply if requirements include:
- performance SLA, high read load, repeated reads, caching explicitly mentioned
If applied:
- Prefer cache-aside with TTL 
- Define invalidation rules clearly

### 9.2 Retry / Exponential Backoff
ONLY apply if requirements include:
- unstable external dependency, retries required, transient failure handling
If applied:
- Only retry idempotent operations 
- Bound the retry count and total time

### 9.3 Rate Limiting
ONLY apply if requirements include:
- abuse prevention, rate limit requirement, quota enforcement 
Avoid in-memory rate limiting unless explicitly acceptable in the spec.

### 9.4 Background Jobs / Queues
ONLY apply if requirements include:
- async processing requirement, job queue, background indexing, non-blocking tasks
In-memory queues are acceptable only if the assessment explicitly allows single-process assumptions.

### 9.5 Authentication / Authorization
ONLY apply if requirements include:
- auth requirement, user identity, roles/permissions
If applied:
- Authenticate at boundary (filter/interceptor)
- Authorize in service layer
