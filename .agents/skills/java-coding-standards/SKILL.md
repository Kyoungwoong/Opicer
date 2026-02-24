---
name: java-coding-standards
description: Java coding standards for Spring Boot services naming, immutability, Optional usage, streams, exceptions, generics, and project layout.
allowed-tools: filesystem shell
---

# Java Coding Standards

Standards for readable, maintainable Java (17+) code in Spring Boot services.

## Project Note (Opicer)

Backend is Java + Spring Boot + Spring Data JPA. Use Java 17 features
(records, sealed types where appropriate) and keep Spring usage idiomatic.

## Core Principles

- Prefer clarity over cleverness
- Immutable by default; minimize shared mutable state
- Fail fast with meaningful exceptions
- Consistent naming and package structure

## Naming

```java
// ✅ Classes/Records: PascalCase
public class MarketService {}
public record Money(BigDecimal amount, Currency currency) {}

// ✅ Methods/fields: camelCase
private final MarketRepository marketRepository;
public Market findBySlug(String slug) {}

// ✅ Constants: UPPER_SNAKE_CASE
private static final int MAX_PAGE_SIZE = 100;
```

## Immutability

```java
// ✅ Favor records and final fields
public record MarketDto(Long id, String name, MarketStatus status) {}

public class Market {
  private final Long id;
  private final String name;
  // getters only, no setters
}
```

## Optional Usage

```java
// ✅ Return Optional from find* methods
Optional<Market> market = marketRepository.findBySlug(slug);

// ✅ Map/flatMap instead of get()
return market
    .map(MarketResponse::from)
    .orElseThrow(() -> new EntityNotFoundException("Market not found"));
```

## Streams Best Practices

```java
// ✅ Use streams for transformations, keep pipelines short
List<String> names = markets.stream()
    .map(Market::name)
    .filter(Objects::nonNull)
    .toList();

// ❌ Avoid complex nested streams; prefer loops for clarity
```

## Exceptions

- Use unchecked exceptions for domain errors; wrap technical exceptions with context
- Create domain-specific exceptions (e.g., `MarketNotFoundException`)
- Avoid broad `catch (Exception ex)` unless rethrowing/logging centrally

```java
throw new MarketNotFoundException(slug);
```

## Generics and Type Safety

- Avoid raw types; declare generic parameters
- Prefer bounded generics for reusable utilities

```java
public <T extends Identifiable> Map<Long, T> indexById(Collection<T> items) { ... }
```

## Project Structure (Maven/Gradle)

```
src/main/java/com/example/app/
  config/
  controller/
  dto/
  error/
  service/
  repository/
  domain/
  util/
src/main/resources/
  application.yml
src/test/java/... (mirrors main)
```

## Spring & JPA Guidance

- Controllers only handle HTTP and validation; no business rules.
- Services contain use-case logic and transactions.
- Repositories contain data access only.
- Do not expose JPA entities directly in API responses; map to DTOs.

```java
@Service
public class QuestionService {
  private final QuestionRepository repository;

  public QuestionService(QuestionRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Question create(CreateQuestionRequest req) {
    Question entity = Question.create(req);
    return repository.save(entity);
  }
}
```

### JPA Entity Rules
- Use `@Entity` only on persistence models.
- Keep entities small; avoid public setters unless needed by JPA.
- Use `@Enumerated(EnumType.STRING)` for enums.
- Prefer `@ManyToOne(fetch = FetchType.LAZY)` and explicit fetch joins for lists.

### DTO Mapping
- Use records for request/response DTOs.
- Keep mapping in a dedicated mapper or in DTO factory methods.

```java
public record QuestionResponse(UUID id, String topic, String promptText) {
  public static QuestionResponse from(Question q) {
    return new QuestionResponse(q.getId(), q.getTopic(), q.getPromptText());
  }
}
```

## Formatting and Style

- Use 2 or 4 spaces consistently (project standard)
- One public top-level type per file
- Keep methods short and focused; extract helpers
- Order members: constants, fields, constructors, public methods, protected, private

## Code Smells to Avoid

- Long parameter lists → use DTO/builders
- Deep nesting → early returns
- Magic numbers → named constants
- Static mutable state → prefer dependency injection
- Silent catch blocks → log and act or rethrow

## Logging

```java
private static final Logger log = LoggerFactory.getLogger(MarketService.class);
log.info("fetch_market slug={}", slug);
log.error("failed_fetch_market slug={}", slug, ex);
```

## Null Handling

- Accept `@Nullable` only when unavoidable; otherwise use `@NonNull`
- Use Bean Validation (`@NotNull`, `@NotBlank`) on inputs

## Testing Expectations

- JUnit 5 + AssertJ for fluent assertions
- Mockito for mocking; avoid partial mocks where possible
- Favor deterministic tests; no hidden sleeps

**Remember**: Keep code intentional, typed, and observable. Optimize for maintainability over micro-optimizations unless proven necessary.
