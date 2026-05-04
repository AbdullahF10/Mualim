# Smart Office Codebase — Learning Curriculum

> This document has two parts:
> 1. **The Learning Project** — a step-by-step project you build from scratch that introduces each concept organically
> 2. **The Concept Reference** — detailed explanation of every keyword, annotation, and tool you'll encounter in the smart office codebase
>
> Work through the project phases in order. Each phase introduces new concepts and adds them to the running application. When you hit a concept in a phase, go read its entry in the reference section below.
>
> Check off items as you genuinely master them — not just "I read about it" but "I can explain it, read code using it, and write new code with it correctly."

---

# Part 1 — The Learning Project

## The Project: LearnHub (Online Course Platform)

You will build a **simplified online learning management system**. Students enroll in courses, complete lessons, submit assignments, get graded, and receive certificates. Instructors create content and track progress. Admins manage everything.

### Why This Domain
This is as far from a smart office as you can get. Smart office is about workplaces: desk booking, room booking, access control, employee management. LearnHub is about education: courses, lessons, grades, certifications.

But it needs **every single technology in the smart office codebase** because:
- Multiple microservices that call each other → **Feign**
- Students completing lessons triggers downstream actions → **Kafka**
- Course catalog needs full-text search → **OpenSearch**
- Student profile images, assignment file uploads, certificates → **MinIO**
- Bulk student enrollment via spreadsheet → **Apache POI**
- Instructor uploads grade reports for download → **Apache POI**
- Popular courses served from memory → **Redis + Guava Cache**
- Daily reminders for assignment deadlines → **Scheduling + ShedLock**
- Real-time quiz countdowns → **SSE**
- Email confirmation on enrollment → **Jinjava templates**
- The current student's identity must flow into every service call → **ThreadLocal context**
- Prevent duplicate enrollment clicks → **AOP idempotency**
- Auto-log every API call for debugging → **AOP logging**

### Final Architecture (you build up to this gradually)

```
lms-parent/                       ← Root POM, manages all versions
  └── lms-common/                 ← Shared: exceptions, ReturnWrapper, utils, base classes

lms-user/                         ← Manages students, instructors, admin accounts
  ├── lms-user-facade/            ← Public API: DTOs, enums, Feign client interface
  └── lms-user-core/              ← Implementation: controllers, services, mappers, config

lms-course/                       ← Manages courses, lessons, enrollments
  ├── lms-course-facade/
  └── lms-course-core/

lms-assessment/                   ← Manages quizzes, assignments, grades, certificates
  ├── lms-assessment-facade/
  └── lms-assessment-core/

lms-notification/                 ← Sends emails and push notifications
  ├── lms-notification-facade/
  └── lms-notification-core/

lms-search/                       ← OpenSearch-backed course & instructor search
  └── lms-search-core/            ← No facade needed; only called internally
```

---

## Phase 1 — The Skeleton (Weeks 1–2)
**Goal**: Get a single working Spring Boot app that can receive and respond to HTTP requests. No database, no nothing fancy. Just the bones.

**What you build**: A single `lms-user` Spring Boot project (not yet multi-module). One controller. Two endpoints. Returns hardcoded data.

**Concepts introduced**:
- Maven `pom.xml` basics (`groupId`, `artifactId`, `version`, `dependencies`)
- `spring-boot-starter-web` dependency
- `@SpringBootApplication`, `SpringApplication.run()`
- `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`
- `@PathVariable`, `@RequestParam`, `@RequestBody`
- Returning plain strings and objects as JSON (auto-serialization)
- Running the app locally (`mvn spring-boot:run`)
- The fat JAR (`mvn package`, then `java -jar target/app.jar`)
- `application.yml` basics (changing the server port)

**Deliverable**: 
```
GET /v1/users/{id}         → returns { "id": 1, "name": "Alice", "email": "alice@example.com" }
POST /v1/users             → accepts { "name": "Bob", "email": "bob@example.com" }, returns "created"
```
Both endpoints return hardcoded data. No DB yet.

**Concept reference sections**: 3, 4

---

## Phase 2 — Lombok & Response Wrapper (Week 2–3)
**Goal**: Stop writing boilerplate Java. Add the standard API response envelope.

**What you add**:
- Lombok on all your POJOs (VO classes, request classes)
- A `ReturnWrapper<T>` class — your API now always returns `{ code, message, data }` instead of raw objects
- A `UserCreateRequest` class with proper fields
- A `UserDetailResponse` class

**Concepts introduced**:
- `@Data`, `@Getter`, `@Setter`, `@Builder`, `@Slf4j`
- `@AllArgsConstructor`, `@NoArgsConstructor`
- Lombok on enums (`@Getter` for enum fields)
- Generics in Java (`ReturnWrapper<T>`)
- Static factory methods (`ReturnWrapper.success(data)`, `ReturnWrapper.error(code, msg)`)

**Deliverable**:
```
GET /v1/users/{id} → { "code": 0, "message": "Succeed", "data": { "id": 1, "name": "Alice" } }
```

**Concept reference sections**: 6, 7 (partial — just serialization basics)

---

## Phase 3 — Exception Handling (Week 3)
**Goal**: Handle errors consistently. Never let raw stack traces reach the client.

**What you add**:
- `UserNotFoundException extends RuntimeException`
- A custom `ErrorCode` interface and `LmsErrorCode` enum (your own version of `RspCode`)
- `LmsException extends RuntimeException` (your version of `CommonErrorException`)
- `@ControllerAdvice` + `@ExceptionHandler` to catch them all
- An `AssertUtils` class with `notNull()`, `isTrue()` helpers
- Update your GET endpoint to throw when user not found

**Concepts introduced**:
- `RuntimeException` vs checked exceptions
- Custom exception classes
- `@ControllerAdvice`, `@ExceptionHandler`
- `@Order` on `@ControllerAdvice`
- `@ResponseBody` on exception handler methods
- The "throw don't return" mindset — services throw, the handler returns the response
- Why `RuntimeException` auto-triggers transaction rollback (important for later)

**Deliverable**:
```
GET /v1/users/999 → { "code": 4, "message": "User not found", "data": null }
GET /v1/users/abc → { "code": 10000, "message": "Unknown error", "data": null }
```

**Concept reference sections**: 5

---

## Phase 4 — Database with MyBatis Plus (Weeks 4–5)
**Goal**: Persist data. Connect to a real MySQL database.

**What you add**:
- MySQL running locally (Docker: `docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=123456 mysql:8`)
- `mybatis-plus-boot-starter` dependency
- `UserEntity` class with `@TableName`, `@TableId`, `@TableField`
- `UserMapper extends BaseMapper<UserEntity>` with `@Mapper`
- `@MapperScan` configuration
- `datasource` config in `application.yml`
- A `UserService` interface and `UserServiceImpl` with `@Service`
- Wire everything: controller → service → mapper → DB

**Concepts introduced**:
- `@TableName`, `@TableId(type = IdType.AUTO)`, `@TableField(exist = false)`
- `BaseMapper<T>` and its built-in methods: `insert()`, `selectById()`, `updateById()`, `deleteById()`, `selectList()`
- `@Mapper` and `@MapperScan`
- `LambdaQueryWrapper` and `LambdaQueryChainWrapper` for building queries
- `QueryWrapper` for raw string column queries
- The service layer pattern (interface + impl)
- `@Configuration` + `@Bean` for manual bean definitions
- Druid connection pool config (`spring.datasource.type: com.alibaba.druid.pool.DruidDataSource`)

**Deliverable**: Real CRUD — create a user in DB, fetch by ID, list all users, delete a user.

**Concept reference sections**: 8a, 8b, 8c

---

## Phase 5 — Validation & Request DTOs (Week 5)
**Goal**: Reject bad input at the boundary. Don't let garbage into your service.

**What you add**:
- `@NotBlank`, `@Size`, `@NotNull`, `@Email` on `UserCreateRequest` fields
- `@Valid` on the controller parameter
- `BindingResult` handling (extend `BaseController`, add `check()` method)
- Return `ParamErrorException` for validation failures

**Concepts introduced**:
- `jakarta.validation.constraints.*` annotations
- `@Valid` on `@RequestBody`
- `BindingResult` and `bindingResult.getFieldErrors()`
- `@NotBlank` vs `@NotNull` vs `@NotEmpty`
- `@Size(min=, max=)`, `@Min`, `@Max`
- Why constraints live on DTOs not entities

**Deliverable**: `POST /v1/users` with `{ "name": "" }` returns `{ "code": 2, "message": "name cannot be blank" }`

**Concept reference sections**: 10

---

## Phase 6 — Transactions (Week 6)
**Goal**: Understand when and how the database should treat multiple operations as one atomic unit.

**What you add**:
- A `UserProfileEntity` table that stores extra student info (bio, avatar URL, institution)
- When creating a user, you insert into both `t_user` AND `t_user_profile` in one operation
- If inserting into `t_user_profile` fails, the `t_user` insert must also roll back
- Add `@Transactional` to the create method
- Add a test where you deliberately make the second insert fail (throw an exception in the middle)

**Concepts introduced**:
- `@Transactional` and what it does
- Rollback on `RuntimeException`
- `@Transactional(rollbackFor = Exception.class)` for checked exceptions
- The self-invocation pitfall — what happens if you call a `@Transactional` method from within the same class
- Declarative vs programmatic transactions (`PlatformTransactionManager`)
- `DataSourceTransactionManager` in `MybatisConfig`

**Deliverable**: Creating a user atomically inserts into two tables. Forced failure rolls both back and the DB stays clean.

**Concept reference sections**: 9

---

## Phase 7 — Multi-Module Maven (Week 7)
**Goal**: Split your project into `facade` and `core` modules the way the smart office codebase does. This is architecturally important.

**What you do**: Restructure `lms-user` from a single project into:
```
lms-user/
  ├── lms-user-facade/   ← Move all DTOs, enums, the Feign client interface here
  └── lms-user-core/     ← Keep all implementation (controllers, services, mappers) here
lms-common/              ← Move ReturnWrapper, exceptions, AssertUtils here
lms-parent/              ← New root POM with dependencyManagement
```

**Concepts introduced**:
- `<packaging>pom</packaging>` for parent/aggregator modules
- `<modules>` declaring child modules
- `<dependencyManagement>` vs `<dependencies>` — the critical difference
- BOM imports (`<scope>import</scope>`, `<type>pom</type>`)
- Property-based versioning (`${spring-boot.version}`)
- Maven build order based on dependency graph
- Why the facade module must NOT depend on the core module (only core depends on facade)
- `<scope>provided</scope>` for annotation processors

**Deliverable**: Same behavior as before, but now split into modules. `mvn clean install` builds them in the right order.

**Concept reference sections**: 1

---

## Phase 8 — Mapping with MapStruct (Week 8)
**Goal**: Stop manually copying fields between Entity, Request, and Response objects.

**What you add**:
- `UserMapper` interface (MapStruct, not MyBatis — different thing) in the user-facade module
- `@Mapper` on it
- Mapping: `UserCreateRequest` → `UserEntity`
- Mapping: `UserEntity` → `UserDetailResponse`
- Handle one field that has a different name in the two classes (`@Mapping`)
- Handle one field you want to ignore during conversion (`ignore = true`)

**Concepts introduced**:
- MapStruct `@Mapper` vs MyBatis `@Mapper` (same annotation name, completely different libraries — context determines which)
- `Mappers.getMapper(UserMapping.class)` pattern and `INSTANCE` static singleton
- `@Mapping(target = "...", source = "...")` for different field names
- `@Mapping(target = "...", ignore = true)` to skip a field
- `@Mappings({...})` for multiple mappings
- How MapStruct generates code at compile time (look in `target/generated-sources/`)
- Why MapStruct > `BeanUtils.copyProperties()` (type-safe, refactor-safe, faster)
- The compile-time setup: `mapstruct-processor` in `<scope>provided</scope>`

**Deliverable**: Services use `UserMapping.INSTANCE.toEntity(request)` and `UserMapping.INSTANCE.toResponse(entity)` instead of manual field copying.

**Concept reference sections**: 11

---

## Phase 9 — AOP: Custom Logging (Week 9)
**Goal**: Add automatic method logging without touching every method.

**What you build**:
- `@LogPrint` custom annotation (your version of `@LogPrintAnnotation`)
- `LogPrintAspect` with `@Aspect` + `@Around`
- Annotate a few controller methods and service methods with `@LogPrint`
- The aspect should log: method name, all parameters as JSON, return value if configured to, execution time

**Concepts introduced**:
- `@Aspect`, `@Around`, `@Pointcut`
- `ProceedingJoinPoint` — calling `proceed()` to execute the real method
- `MethodSignature` — getting parameter names and types
- Custom annotation creation: `@Target`, `@Retention`, `@Documented`
- `ElementType.METHOD` — restricts annotation to methods
- `RetentionPolicy.RUNTIME` — annotation visible at runtime (required for AOP)
- Extracting parameter names from `MethodSignature.getParameterNames()`
- Extracting parameter values from `ProceedingJoinPoint.getArgs()`
- The `try/finally` pattern in aspects — `finally` for cleanup even if the method throws
- What "proxy" means — Spring wraps your bean in a proxy that intercepts calls

**Deliverable**: Any method annotated with `@LogPrint` automatically logs `[MethodName] start parameters: {key=value}` and `[MethodName] end result: {...}` to the log, with no code change in the method itself.

**Concept reference sections**: 16

---

## Phase 10 — Redis (Week 10)
**Goal**: Cache hot data. Stop hitting the database for the same data repeatedly.

**Context**: Course categories (like "Programming", "Data Science", "Design") rarely change. Fetching them from DB on every request is wasteful.

**What you add**:
- Add `lms-course` service with a `CourseCategory` entity
- `RedisConfig` in course-core: configure `RedisTemplate<String, Object>` with Jackson serializer
- Cache the list of categories in Redis after first DB fetch
- On subsequent requests, serve from Redis
- When a category is added or updated, invalidate the cache
- Add a `StringRedisTemplate` usage for a simple string-valued cache (e.g., caching a course's active enrollment count as a string)

**Concepts introduced**:
- `RedisTemplate<K, V>` and how to configure it
- `StringRedisTemplate` for simple string values
- `RedisConnectionFactory` — Spring auto-configures from YAML
- `StringRedisSerializer` for keys
- `Jackson2JsonRedisSerializer` for values — why you need this config, what happens without it
- `ObjectMapper.DefaultTyping.EVERYTHING` — why you need type information stored in Redis for correct deserialization
- `opsForValue()`, `opsForHash()`, `opsForSet()` — the different Redis data structure operations
- Key naming conventions (use `:` as a separator, e.g., `lms:course:categories:all`)
- TTL (time-to-live): `template.opsForValue().set(key, value, Duration.ofMinutes(30))`
- Cache invalidation: `template.delete(key)` after a write

**Deliverable**: `GET /v1/courses/categories` — first call hits DB, subsequent calls hit Redis. Adding a category via API invalidates the cache.

**Concept reference sections**: 12a

---

## Phase 11 — Guava Local Cache (Week 10–11)
**Goal**: Cache reference data in-memory (not Redis) for ultra-fast access without network round trips.

**Context**: Every time you look up a course by its instructor's ID (to get the instructor's name), you call the user service or hit Redis. But instructor data changes rarely. Cache it locally in the process.

**What you add**:
- `InstructorCache` class that implements `InitializingBean`
- It builds a `LoadingCache<Long, InstructorDetailResponse>` in `afterPropertiesSet()`
- The `CacheLoader.load()` calls the user Feign client to fetch instructor info
- Services call `instructorCache.get(instructorId)` instead of Feign directly
- When an instructor's info changes, call `instructorCache.invalidate(id)`

**Concepts introduced**:
- `InitializingBean.afterPropertiesSet()` — why you use this instead of a constructor for setup that requires `@Autowired` fields
- `CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)).build(CacheLoader)`
- `CacheLoader<K, V>` and the `load(K key)` method
- `LoadingCache<K, V>.get(key)` — triggers `load()` on miss
- `cache.invalidate(key)` for invalidation
- `CacheLoader.InvalidCacheLoadException` — when `load()` returns null
- Local cache vs Redis cache — when to use each: local cache for reference data that rarely changes and is read by a single service; Redis for data that needs to be shared across multiple pods or services

**Deliverable**: Instructor name lookup in course responses uses the Guava cache — first lookup is slow (Feign call), subsequent lookups are instantaneous.

**Concept reference sections**: 12b

---

## Phase 12 — OpenFeign: Service-to-Service Calls (Week 11–12)
**Goal**: `lms-course` needs student and instructor info from `lms-user`. Build the HTTP client.

**What you add**:
- `UserFeignClient` interface in `lms-user-facade` with `@FeignClient(value = "lms-user", path = "/v1/common")`
- Methods matching the endpoints in `lms-user-core`'s controllers
- `lms-course-core` imports `lms-user-facade` as a dependency and `@Autowired UserFeignClient`
- Wire `@EnableFeignClients(basePackages = "com.yourpackage")` on course's main class
- For local dev: use simple discovery in YAML pointing `lms-user` to `localhost:8081`

**Concepts introduced**:
- `@FeignClient(value = "...", path = "...", contextId = "...")`
- `@EnableFeignClients`
- Feign method annotations mirroring controller annotations exactly
- `@SpringQueryMap` for GET requests with object parameters
- The facade module as a shared contract — the client and the server agree on the interface
- `contextId` — why you need it when two Feign clients point to the same service name
- `ReturnWrapper<T>` as the return type of Feign methods
- Local development service discovery override in YAML
- What happens when the called service is down (connection refused exception)
- Feign timeout configuration

**Deliverable**: `GET /v1/courses/{id}` returns a course with the instructor's full name fetched from user-service via Feign.

**Concept reference sections**: 14

---

## Phase 13 — Context Propagation with ThreadLocal (Week 12)
**Goal**: Know who is making each request, and pass that identity to every downstream service call automatically.

**Context**: When a student enrolls in a course, the course service needs to know which student is enrolling. The student's ID comes from the HTTP header (set by an API gateway or auth service). It should flow through automatically — you shouldn't have to pass `studentId` as a parameter to every method.

**What you add**:
- `StudentRequestContext` record: holds `studentId`, `email`, `role`
- `StudentRequestContextHolder` with a `ThreadLocal<StudentRequestContext>` (static field)
- A `Filter` or `HandlerInterceptor` that reads headers from the incoming request and calls `StudentRequestContextHolder.set(context)`
- The context is cleared in `afterCompletion` / `finally` to prevent leaks
- A `FeignInterceptor implements RequestInterceptor` that reads the context and adds it as headers to every outgoing Feign call
- Update `MyBatisConfig`'s `MetaObjectHandler` to read `createBy` from the context

**Concepts introduced**:
- `ThreadLocal<T>` — stores one value per thread. The HTTP request thread stores the context; that same thread (and only that thread) reads it from anywhere in the call stack
- The memory leak risk — always clear ThreadLocal in `finally` blocks or use `remove()`
- `HandlerInterceptor` — Spring MVC's hook into the request lifecycle (preHandle, postHandle, afterCompletion)
- `RequestInterceptor` (Feign) — how Feign lets you add headers to every request
- `TransmittableThreadLocal` (TTL) — why regular `ThreadLocal` breaks with `@Async` (async tasks run on different threads, so the context is lost). TTL propagates the context to child threads. Relevant when you later add async operations

**Deliverable**: Hit `GET /v1/courses/{id}` with header `X-Student-Id: 42`. The request context is available throughout the call, logged in the `@LogPrint` aspect, and forwarded to the user-service Feign call automatically.

**Concept reference sections**: 26

---

## Phase 14 — Kafka: Async Event Messaging (Weeks 13–14)
**Goal**: Decouple services. When a student enrolls in a course, publish an event rather than synchronously calling every interested service.

**What you add**:
- Kafka running locally (Docker Compose: Kafka + Zookeeper)
- `EnrollmentEventProducer` in `lms-course-core` — publishes `{ eventType: "ENROLLED", studentId, courseId, courseTitle }` to the `lms.enrollment.events` topic
- `EnrollmentEventConsumer` in `lms-notification-core` — listens on that topic and sends a welcome email
- `EnrollmentEventConsumer` in `lms-assessment-core` — creates the student's grade record when they enroll
- Use the `EventMessageDTO<T>` envelope pattern (your own version)

**Concepts introduced**:
- Topics, producers, consumers, consumer groups
- Why Kafka: decoupling, async processing, fan-out (one event → multiple consumers)
- `@KafkaListener(topics = "...", groupId = "...")`
- `ConsumerRecord<?, ?>` — the raw record
- `Acknowledgment` — manual commit: `ack.acknowledge()` after successful processing
- AckMode.MANUAL — configure this on the listener container factory
- `kafkaListenerContainerFactory` bean configuration
- Message deserialization: `byte[] bytes = (byte[]) record.value()` → `String message = new String(bytes)` → `objectMapper.readValue(message, ...)`
- `EventMessageDTO<T>` wrapper: always wrap your payload in a standard envelope with `type` and `body`
- Consumer group semantics: if both notification-core and assessment-core are in different groups, both get every message. If two pods of notification-core are in the same group, only one pod gets each message
- Error handling in consumers: log but don't rethrow unless you want Kafka to retry

**Deliverable**: Student enrolls via API → enrollment is saved in DB → Kafka event fires → notification service sends email → assessment service creates grade record. All async, no direct service-to-service HTTP calls for these flows.

**Concept reference sections**: 15

---

## Phase 15 — Scheduling + ShedLock (Week 14–15)
**Goal**: Run background jobs on a schedule. Prevent them from running on every pod simultaneously.

**What you add**:
- `DeadlineReminderTask` in lms-notification-core: every day at 8am, find assignments due in the next 48 hours and email each student a reminder
- `GradeExpiryTask` in lms-assessment-core: every week, mark quiz attempts older than 90 days as "archived"
- Add `@EnableScheduling` + `@EnableSchedulerLock` + `RedisLockProvider` configuration
- Annotate tasks with `@Scheduled(cron = "0 0 8 * * ?")` and `@SchedulerLock(name = "...:reminder:daily")`

**Concepts introduced**:
- `@EnableScheduling` — required to activate `@Scheduled`
- `@Scheduled(cron = "...")` — 6-field cron (second, minute, hour, day, month, weekday)
- Cron expression syntax — `0 0 8 * * ?` = every day at 08:00:00
- The Kubernetes pod scaling problem: if you run 3 pods, your 8am job fires 3 times simultaneously unless you coordinate
- ShedLock solution: one pod wins a Redis lock; the others skip
- `@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")` — ISO-8601 duration: 5 minutes max lock hold time (protects against a pod crash holding the lock forever)
- `@SchedulerLock(name = "unique:name", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")`
- `RedisLockProvider(redisConnectionFactory)` bean
- `lockAtMostFor` vs `lockAtLeastFor` — at most: release the lock this long after start even if still running (safety). At least: hold the lock for at minimum this long (prevents re-entry if the job finishes too fast)

**Deliverable**: The reminder job fires on schedule. When you run two instances of the notification service simultaneously, only one of them executes the job each cycle.

**Concept reference sections**: 13 (scheduling), 12c (ShedLock)

---

## Phase 16 — Async Execution (Week 15)
**Goal**: Don't make the API wait for slow operations like sending emails.

**What you add**:
- `@EnableAsync` on the notification application
- A `ThreadPoolTaskExecutor` bean with sensible settings (core=4, max=10, queue=100)
- Mark `EmailSenderService.sendEnrollmentEmail()` with `@Async("notificationPool")`
- The Feign call chain that triggers email sending now returns immediately; the email is sent in the background

**Concepts introduced**:
- `@EnableAsync` — required for `@Async` to work
- `@Async` — method runs on a thread pool thread; caller continues immediately
- `@Async("poolName")` — specifying which executor bean to use (otherwise Spring uses a single-threaded default which is bad in production)
- `ThreadPoolTaskExecutor` — Spring's thread pool abstraction
  - `corePoolSize` — threads always alive
  - `maxPoolSize` — maximum concurrent threads
  - `queueCapacity` — tasks waiting when all cores are busy
  - `keepAliveSeconds` — how long extra threads live when idle
  - `rejectedExecutionHandler` — what happens when queue is full and max threads reached
- `CallerRunsPolicy` — the queue is full so the calling thread executes the task itself (slows the caller, never drops tasks)
- Return type of `@Async` methods: `void` (fire-and-forget) or `CompletableFuture<T>` (if you need the result later)
- `CompletableFuture.allOf(f1, f2).join()` — wait for parallel async operations to both complete

**Deliverable**: `POST /v1/courses/{id}/enroll` returns immediately (HTTP 200). The welcome email is sent in the background 1-3 seconds later.

**Concept reference sections**: 13

---

## Phase 17 — AOP: Idempotency Guard (Week 16)
**Goal**: Prevent duplicate enrollments if a student double-clicks the enroll button.

**What you build**:
- `@Idempotent` custom annotation (your version of `@RepeatSubmit`)
- `IdempotentAspect` that uses Redis to store a lock for a short duration
- Annotate `enroll()` method with `@Idempotent(fields = {"studentId", "courseId"}, ttlSeconds = 5)`
- The aspect extracts the field values from the method arguments using reflection, builds a Redis key, and tries to `SET NX EX` (set if not exists with expiry)
- If the key already exists, throw a `DuplicateRequestException`

**Concepts introduced**:
- `ReflectionUtils.findField()`, `ReflectionUtils.makeAccessible()`, `ReflectionUtils.getField()` — reading field values dynamically from an object
- `SET NX EX` pattern in Redis — atomic "set if not exists with expiry" — the distributed lock for idempotency
- `AtomicReference<Object>` — thread-safe holder used in aspects when you need to capture the return value of `proceed()` across lambda boundaries
- The difference between distributed locks for idempotency (short TTL, per-request) vs ShedLock (per-job-execution)

**Deliverable**: Double-clicking enroll within 5 seconds returns `{ "code": 5, "message": "Duplicate request, please wait" }` instead of creating two enrollments.

**Concept reference sections**: 16 (AOP), 12a (Redis)

---

## Phase 18 — Spring Boot Lifecycle (Week 16–17)
**Goal**: Initialize state at startup — create default data if it doesn't exist.

**What you add**:
- `CourseSystemInitRunner implements ApplicationRunner` — on startup, checks if the default course categories exist in DB (Programming, Design, Business, etc.). If not, inserts them. Also warms up the Redis category cache
- `InstructorCache implements InitializingBean` — builds its Guava cache in `afterPropertiesSet()` (you likely already did this in Phase 11, now understand WHY it works here)

**Concepts introduced**:
- `ApplicationRunner.run(ApplicationArguments)` — fires after the application is fully started and HTTP server is ready. Safe to call any bean
- `CommandLineRunner.run(String...)` — same timing, gets raw command-line args
- `InitializingBean.afterPropertiesSet()` — fires after `@Autowired` injection completes but BEFORE the HTTP server starts. Good for internal state setup
- `@PostConstruct` — annotation equivalent of `afterPropertiesSet()`
- Startup order: IoC container builds → @Autowired injection → `@PostConstruct` / `afterPropertiesSet()` → HTTP server starts → `ApplicationRunner` / `CommandLineRunner`
- Why you CANNOT call `@Autowired` services from a bean's constructor — injection hasn't happened yet at that point

**Deliverable**: Fresh database startup automatically seeds default course categories. No manual SQL seeding needed.

**Concept reference sections**: 17

---

## Phase 19 — Bean Validation Deep Dive + XML Mapper Queries (Week 17–18)
**Goal**: Master more complex validations and write multi-join SQL queries in XML.

**What you add**:
- Course search with filters: category, instructor, price range, rating
- The search request has cross-field validation: if `minPrice` is provided, `maxPrice` must be too
- The mapper XML file has a complex multi-condition query with `<if>`, `<where>`, `<foreach>`
- Add pagination to the results

**Concepts introduced (Validation)**:
- Cross-field validation with custom `@Constraint` (basic custom validator)
- `ConstraintValidator<A, T>` — the interface to implement
- Group validation — validating different subsets of fields for different operations

**Concepts introduced (XML Mapper)**:
- `<sql id="baseColumns">` + `<include refid="baseColumns">` — reusable SQL fragments
- `<where>` tag — smart WHERE clause generation (removes leading AND)
- `<if test="condition">` — OGNL expression language: `!= null and != ''`, `.size() > 0`
- `<foreach collection="list" item="item" open="(" separator="," close=")">` — IN clause generation
- Multi-table JOIN queries in XML
- `@Param("request")` on mapper method parameters and `#{request.field}` in XML

**Concepts introduced (Pagination)**:
- `Page<T>` + `IPage<T>` with MyBatis Plus `PaginationInnerInterceptor`
- `MybatisPlusInterceptor` configuration in `MybatisConfig`
- `PageHelper.startPage(page, size)` + `PageInfo<T>` (alternative approach)
- When to use each: MyBatis Plus pagination for code using `BaseMapper`; PageHelper for XML mappers

**Concept reference sections**: 8d, 8e, 10, 21

---

## Phase 20 — Apache POI: Excel Import & Export (Week 18–19)
**Goal**: Let admins bulk-import students via Excel. Let instructors download grade reports.

**What you add**:
- `POST /v1/admin/students/import` — upload an `.xlsx` file, parse rows, create student accounts, return an error file if any rows fail
- `GET /v1/assessment/grades/export?courseId=1` — generate and download an `.xlsx` file with student names, grades, and submission dates
- Use the `Closeable` task pattern for the import: a class that opens the workbook in constructor and writes the result in `close()`, used in try-with-resources

**Concepts introduced**:
- `XSSFWorkbook` for `.xlsx` (XML-based). `HSSFWorkbook` for `.xls` (old binary format) — always use XSSF
- `XSSFSheet`, `Row`, `Cell` — the object hierarchy
- `cell.getCellType()` — check type before getting value (`CellType.STRING`, `CellType.NUMERIC`, `CellType.BLANK`)
- `cell.getStringCellValue()`, `cell.getNumericCellValue()` — type-specific getters
- `sheet.getLastRowNum()` — total rows
- `row.createCell(colNum)` — create a new cell
- `cell.setCellValue(...)` — write a value
- `XSSFCellStyle` + `XSSFFont` — styling (colors, bold, borders)
- `IndexedColors.RED.getIndex()` — built-in color index
- `workbook.write(outputStream)` — write the workbook to a stream
- `MultipartFile` — Spring's type for file uploads. `multipartFile.getInputStream()` to read the file
- `Closeable` pattern — resource management with try-with-resources

**Deliverable**: `POST /admin/students/import` with an Excel file. Rows that fail validation get an error message in a new column. The result file is returned to download.

**Concept reference sections**: 22

---

## Phase 21 — MinIO: File Storage (Week 19–20)
**Goal**: Store student profile pictures, course thumbnails, and assignment submission files.

**What you add**:
- MinIO running locally (Docker)
- `MinioService` wrapping the MinIO Java client
- Upload profile picture when creating/updating a user
- Download profile picture URL for display
- On student assignment submission: accept file upload, store in MinIO, save the object key in DB
- Initialize default buckets at startup (add to `CourseSystemInitRunner`)

**Concepts introduced**:
- What object storage is vs file system vs DB blobs — and why object storage wins for binary files
- Buckets — named namespaces for objects (like folders at the top level)
- Object keys — the path within a bucket (e.g., `students/42/avatar.jpg`)
- MinIO Java client: `MinioClient.builder().endpoint(...).credentials(...).build()`
- `PutObjectArgs` — for uploads
- `GetPresignedObjectUrlArgs` — for generating temporary download/view URLs
- `BucketExistsArgs` + `MakeBucketArgs` — idempotent bucket creation
- `ContentType` — always set the correct MIME type when uploading
- Why store the object key in DB, not the full URL — the URL may change (different endpoint in prod vs dev), but the key is stable

**Deliverable**: Uploading a profile picture stores it in MinIO. The user detail API returns a presigned URL valid for 1 hour for the client to display the image.

**Concept reference sections**: 23

---

## Phase 22 — OpenSearch: Full-Text Course Search (Week 20–21)
**Goal**: Let students search courses by keyword (title, description, instructor name, tags).

**What you add**:
- `lms-search-core` service
- `CourseDocument` class with `@Document`, `@Field(type = FieldType.Text)` for searchable fields and `@Field(type = FieldType.Keyword)` for exact-match fields
- `CourseSearchRepository extends EnhanceElasticSearchRepository<CourseDocument, String>`
- When a course is published, a Kafka event fires → the search service consumes it and indexes the document in OpenSearch
- `GET /v1/search/courses?q=machine+learning` — full-text search with pagination

**Concepts introduced**:
- Why a dedicated search engine vs MySQL LIKE queries — stemming, relevance ranking, full-text indexing, nested queries
- OpenSearch vs Elasticsearch — same API, OpenSearch is the open-source fork
- Index — like a table, but optimized for search
- Document — like a row, but in JSON
- `@Document(indexName = "lms_courses")` — maps the class to an index
- `@Field(type = FieldType.Text)` — analyzed field (stemmed, tokenized) for full-text search
- `@Field(type = FieldType.Keyword)` — exact match field (not analyzed) for filtering and aggregations
- `@Field(type = FieldType.Long)` / `@Field(type = FieldType.Date)` — typed numeric/date fields
- `ElasticsearchClient` for custom queries beyond what the repository interface provides
- `TermQuery`, `MatchQuery`, `BoolQuery` — the query DSL
- Indexing at write time (via Kafka event) vs at read time — always index asynchronously at write time
- Index aliases for zero-downtime reindexing

**Deliverable**: `GET /v1/search/courses?q=python` returns courses matching "python" in title or description, ordered by relevance.

**Concept reference sections**: 18

---

## Phase 23 — SSE: Real-Time Quiz Timer (Week 21–22)
**Goal**: Push real-time events from the server to the browser — a quiz countdown that stays in sync across devices.

**What you add**:
- `QuizSessionSseManager` in `lms-assessment-core` (your version of `SseEmitterManager`)
- `GET /v1/quiz/{sessionId}/stream` — returns an `SseEmitter` that the browser keeps open
- When a student starts a quiz, a Kafka message fires → the assessment service receives it → pushes the start event to the student's SSE connection
- A `@Scheduled` task every 30 seconds pushes time-remaining updates
- When the quiz ends (timeout or submission), push a "quiz-ended" event and close the emitter

**Concepts introduced**:
- Server-Sent Events (SSE) — one-way, server-to-client push over HTTP. Contrast with WebSockets (bidirectional) and polling (repeated HTTP requests)
- `SseEmitter` — Spring's class for SSE connections
- `new SseEmitter(0L)` — no timeout (0 = indefinite)
- `emitter.send(SseEmitter.event().name("timer").data(payload))` — push an event
- `emitter.onCompletion()`, `emitter.onTimeout()`, `emitter.onError()` — lifecycle callbacks
- `ConcurrentHashMap<String, SseEmitter>` — why you need a concurrent map (multiple HTTP threads)
- The controller method returns the `SseEmitter` — Spring holds the HTTP connection open
- Browser-side: `const es = new EventSource('/v1/quiz/session123/stream')` (good to understand even as a backend dev)

**Deliverable**: A student starts a quiz; their browser receives real-time countdown updates pushed by the server without polling.

**Concept reference sections**: 24

---

## Phase 24 — Email Templates with Jinjava (Week 22)
**Goal**: Send properly formatted HTML emails for enrollment, deadline reminders, and certificates.

**What you add**:
- `JinjavaConfig` — creates the `Jinjava` bean
- HTML templates stored as files in `resources/templates/`:
  - `enrollment_confirmation.html` — `Hi {{ studentName }}, you have enrolled in {{ courseName }}.`
  - `deadline_reminder.html` — with a loop over assignments: `{% for assignment in assignments %}...{% endfor %}`
  - `certificate.html` — conditional: `{% if grade >= 80 %}Congratulations{% else %}You can retry{% endif %}`
- `EmailTemplateService` — calls `jinjava.render(templateString, context)` and passes the result to `JavaMailSender`

**Concepts introduced**:
- `Jinjava` — Java's Jinja2. Template language originally from Python (Flask/Django)
- Template syntax: `{{ variable }}` for output, `{% ... %}` for logic blocks
- `{% if condition %}...{% elif %}...{% else %}...{% endif %}`
- `{% for item in list %}...{% endfor %}`
- `jinjava.render(template, Map<String, Object> context)` — render with data
- `JavaMailSender` — Spring's email sending abstraction (configure SMTP in YAML)
- Loading template files from classpath with `ClassPathResource`
- `IOUtils.toString(inputStream, StandardCharsets.UTF_8)` — reading a resource file to string

**Deliverable**: Students receive a properly formatted HTML enrollment confirmation email within seconds of enrolling.

**Concept reference sections**: 25

---

## Phase 25 — OpenAPI Documentation (Week 23)
**Goal**: Every API endpoint is documented. Other developers (or your future self) can understand the API from the Swagger UI without reading source code.

**What you add**:
- `knife4j-openapi3-jakarta-spring-boot-starter` to each service
- `@Tag(name = "Course Management")` on each controller
- `@Operation(summary = "Enroll student in a course", description = "...")` on each endpoint
- `@Schema(description = "Course title, max 200 characters", example = "Introduction to Python")` on request/response fields
- Access `/doc.html` on each service

**Concepts introduced**:
- OpenAPI 3.0 — the spec. Springdoc reads your annotations and generates a JSON spec
- Knife4j — enhanced Swagger UI, the Chinese standard (exact same thing you see in the smart office codebase)
- `@Tag` — groups endpoints in the UI
- `@Operation` — documents a single endpoint
- `@Schema` — documents a field, with example values and descriptions
- `@Parameter` — documents a query/path parameter
- `@ApiResponse` — documents response codes
- The generated spec at `/v3/api-docs` — the raw JSON that tools like Postman can import

**Deliverable**: Every service has `/doc.html` with a complete, clickable API reference.

**Concept reference sections**: 20

---

## Phase 26 — Spring Cloud: Nacos Config + Kubernetes Discovery (Week 23–24)
**Goal**: Move all config out of code/YAMLs into a central Nacos server. Use Kubernetes for service discovery.

**What you add**:
- Nacos running locally (Docker)
- Move database URL, Redis password, MinIO credentials into Nacos
- Add `spring-cloud-starter-alibaba-nacos-config` to each service
- `bootstrap.yml` sets profile and app name; `bootstrap-local.yml` sets Nacos connection
- Test config refresh: change a value in Nacos, see it reflected in the running app without restart
- For K8s: understand how `@FeignClient(value = "lms-user")` resolves to a K8s Service

**Concepts introduced**:
- What Nacos is — config management + service discovery server
- Why centralized config — security (DB passwords not in code repo), operational flexibility (change config without redeployment)
- `spring.cloud.nacos.config.*` — the config block in `bootstrap-local.yml`
- `extension-configs[0].dataId: mysql.yaml` — pulling multiple config files from Nacos
- `refresh: true` — dynamic config refresh without restart
- `bootstrap.yml` vs `application.yml` loading order
- Kubernetes Service as a load balancer — multiple pods of `lms-user` behind one K8s Service named `lms-user`
- `spring.cloud.kubernetes.discovery.*` — namespace-scoped discovery
- Local dev simple discovery: `spring.cloud.discovery.client.simple.instances.lms-user[0].uri: http://localhost:8081`

**Concept reference sections**: 19

---

## Phase 27 — Docker & CI/CD (Week 24–25)
**Goal**: Package everything into Docker images. Build a CI pipeline that automates it.

**What you add**:
- `Dockerfile` for each service using multi-stage build (Maven build stage + JRE runtime stage)
- `docker-compose.yml` for local dev: all services + MySQL + Redis + Kafka + Nacos + MinIO + OpenSearch
- `.gitlab-ci.yml` with stages: build → docker → deploy (manual trigger)
- JVM tuning in Dockerfile ENV: ZGC, heap sizes, OpenTelemetry agent

**Concepts introduced**:
- Multi-stage Dockerfile — build in one image, copy only the JAR to a smaller runtime image
- `COPY --from=build-stage` — copy artifact from first stage
- `ENV JAVA_OPTS` — JVM flags passed at container start
- ZGC (`-XX:+UseZGC -XX:+ZGenerational`) — Java 21's low-latency garbage collector
- `-Xms` / `-Xmx` — initial and max heap
- `--add-opens` — Java module system workarounds for libraries using reflection
- `-javaagent:opentelemetry-javaagent.jar` — automatic instrumentation for distributed tracing
- `docker-compose.yml` — orchestrating multiple containers locally
- GitLab CI stages, `when: manual`, `tags`, `image`, `services` (Docker-in-Docker)
- Image tagging strategy: `${version}-${branch}-${commit_sha}`
- `Makefile` targets (`image`, `push`) called from CI

**Deliverable**: `docker-compose up` starts all 5 services and all dependencies. Every commit can trigger a Docker build.

**Concept reference sections**: 27, 28

---

## Phase 28 — Java 21 Language Features (Ongoing)
**Goal**: Use modern Java features as they become natural in the code you're writing.

Introduce these organically — don't force all of them at once:

- [ ] **Switch expressions** → `String label = switch (role) { case STUDENT -> "Student"; case INSTRUCTOR -> "Instructor"; default -> "Unknown"; };`
- [ ] **`var`** → `var courses = courseMapper.selectList(wrapper);` — inferred type
- [ ] **Records** → `record StudentContext(Long id, String email, String role) {}` — use for the request context
- [ ] **Stream API** → `.stream().filter(c -> c.isPublished()).map(CourseMapping.INSTANCE::toResponse).toList()`
- [ ] **`.toList()`** (Java 16) → shorthand for `.collect(Collectors.toUnmodifiableList())`
- [ ] **`List.of()`, `Map.of()`** → immutable collections for test data
- [ ] **Pattern matching instanceof** → `if (event instanceof EnrollmentEvent e) { use e.studentId(); }`
- [ ] **Text blocks** → multi-line SQL strings or template strings

**Concept reference sections**: 29

---

# Part 2 — Concept Reference

## Concept 1 — Maven & Multi-Module Projects

- [ ] **What a POM file is** — `groupId`, `artifactId`, `version`, `packaging`
- [ ] **`<packaging>pom</packaging>`** — this module has no code; it's a container/aggregator
- [ ] **`<modules>`** — parent POM declares which directories are child modules
- [ ] **`<parent>`** — child module inherits version, dependency management, and plugin management from parent
- [ ] **`<dependencyManagement>` vs `<dependencies>`** — `dependencyManagement` centrally declares versions without pulling in the dependency. Child modules still need to declare `<dependencies>` entries to actually use them, but they inherit the version without specifying it. Critical distinction
- [ ] **BOM imports** — `spring-boot-dependencies`, `spring-cloud-dependencies`, etc. are imported with `<scope>import</scope>` and `<type>pom</type>`. This pulls in a whole version set from one declaration
- [ ] **`<scope>provided</scope>`** — available at compile time but NOT bundled in the JAR. Used for `mapstruct-processor` and annotation processors
- [ ] **`<scope>test</scope>`** — only available in test compilation and execution
- [ ] **`<exclusions>`** — remove a transitive dependency that comes in via another dependency
- [ ] **Properties** — `${spring-boot.version}` defined in `<properties>`, referenced in `<dependencyManagement>`
- [ ] **`spring-boot-maven-plugin` + `repackage` goal** — creates the fat JAR (all dependencies bundled). Without this, `java -jar` doesn't work
- [ ] **Build order** — Maven resolves the dependency graph and builds modules in the correct order
- [ ] **`mvn clean install -Dmaven.test.skip=true`** — build all modules, skip tests

---

## Concept 2 — Spring Core: IoC & Dependency Injection

- [ ] **IoC container** — Spring creates objects and wires them. You don't `new` your services
- [ ] **`@Component`** — generic Spring-managed bean
- [ ] **`@Service`** — `@Component` with semantic meaning: business logic
- [ ] **`@Repository`** — `@Component` with semantic meaning: data access
- [ ] **`@Controller` / `@RestController`** — `@Component` with semantic meaning: web layer
- [ ] **`@Configuration`** — class that declares `@Bean` methods
- [ ] **`@Bean`** — method whose return value becomes a Spring bean
- [ ] **`@Autowired`** — inject a bean into a field, constructor, or setter. Spring finds the right bean by type
- [ ] **`@Resource`** — Java standard DI annotation. Injects by name first, then by type
- [ ] **`@Qualifier("name")`** — disambiguate when multiple beans of the same type exist
- [ ] **`@Primary`** — the default bean when multiple beans of the same type exist
- [ ] **Singleton scope** — one instance per Spring context. The default. Every `@Autowired` of the same type gets the same object
- [ ] **Field vs constructor injection** — field injection is what this codebase uses. Constructor injection is considered better practice (makes dependencies explicit, easier testing)
- [ ] **`scanBasePackages`** — tells Spring which packages to scan for `@Component` and its variants

---

## Concept 3 — Spring Boot Fundamentals

- [ ] **`@SpringBootApplication`** — `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` combined
- [ ] **`SpringApplication.run()`** — boots the entire context
- [ ] **Auto-configuration** — Spring Boot reads your classpath and configures beans automatically. Adding `spring-boot-starter-data-redis` is enough to get a `RedisTemplate`
- [ ] **Starter dependencies** — convenience packages like `spring-boot-starter-web` that bundle everything for a feature
- [ ] **`application.yml` vs `bootstrap.yml`** — bootstrap loads first, needed for external config (Nacos)
- [ ] **Spring profiles** — `spring.profiles.active: local` selects `bootstrap-local.yml`
- [ ] **`@Value("${property:default}")`** — inject a single config value. The part after `:` is the default
- [ ] **`@ConfigurationProperties(prefix = "target")`** — bind a whole YAML block to a class. More maintainable than many `@Value` annotations
- [ ] **`@EnableScheduling`**, **`@EnableAsync`**, **`@EnableFeignClients`** — activation flags

---

## Concept 4 — Spring Web MVC

- [ ] **`@RestController`** — `@Controller` + `@ResponseBody`. Every return value serialized to JSON
- [ ] **`@RequestMapping`** — URL prefix for the class or a specific HTTP method for a method
- [ ] **`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`**
- [ ] **`@PathVariable`** — bind a URL segment: `@GetMapping("/{id}")` + `@PathVariable Long id`
- [ ] **`@RequestParam`** — bind a query string param. Supports `required = false` and `defaultValue`
- [ ] **`@RequestBody`** — deserialize JSON body into a Java object
- [ ] **`ReturnWrapper<T>`** — the standard envelope. Always wrap responses
- [ ] **`BindingResult`** — captures validation errors from `@Valid`. Must immediately follow the validated parameter
- [ ] **`@ControllerAdvice`** — global exception interceptor for all controllers
- [ ] **`@ExceptionHandler(SomeException.class)`** — handles a specific exception type
- [ ] **`@Order`** — priority when multiple `@ControllerAdvice` beans exist
- [ ] **`MultipartFile`** — Spring's file upload type

---

## Concept 5 — Exception Handling Pattern

- [ ] **`ErrorCode` interface** — every error has `code`, `message`, `desc`
- [ ] **`RspCode` enum** — standard codes: 0=succeed, 2=param error, 4=not found, 10000=unknown. Implements `ErrorCode`
- [ ] **Custom error enums per service** — `UserErrorCode`, `CourseErrorCode` implement `ErrorCode`
- [ ] **`CommonErrorException extends RuntimeException`** — the base unchecked exception. Wraps an `ErrorCode`
- [ ] **`ParamErrorException`** — subclass for bad input
- [ ] **`CommonExceptionHandler`** — `@ControllerAdvice` that catches all exceptions and returns `ReturnWrapper.error()`
- [ ] **`AssertUtils`** — guard clause helper. `AssertUtils.notNull(x, RspCode.NOT_FOUND)` throws instead of returning
- [ ] **RuntimeException + `@Transactional`** — throwing `RuntimeException` (and subclasses) inside a `@Transactional` method auto-triggers rollback

---

## Concept 6 — Lombok

- [ ] **`@Data`** — getters + setters + `equals()` + `hashCode()` + `toString()`
- [ ] **`@Getter` / `@Setter`** — individual getter/setter generation
- [ ] **`@Slf4j`** — injects `private static final Logger log = LoggerFactory.getLogger(...)`
- [ ] **`@Builder`** — builder pattern: `UserEntity.builder().email("x").build()`
- [ ] **`@AllArgsConstructor`** — constructor with all fields
- [ ] **`@NoArgsConstructor`** — no-arg constructor
- [ ] **`@RequiredArgsConstructor`** — constructor for `final` fields only
- [ ] **`@Getter` on enum** — makes enum fields readable via getter: `RspCode.SUCCEED.getCode()`
- [ ] **Compile-time generation** — Lombok generates code during compilation. The generated code is in `target/`

---

## Concept 7 — Jackson (JSON)

- [ ] **`ObjectMapper`** — the core Jackson class
- [ ] **`@JsonProperty("name")`** — map field to a different JSON key
- [ ] **`@JsonIgnore`** — exclude field from serialization/deserialization
- [ ] **`@JsonAutoDetect`** — control which members (fields, methods) are serialized
- [ ] **`@JsonTypeInfo`** — embed type information in JSON for polymorphic deserialization. Used in Redis config
- [ ] **`TypeReference<T>`** — needed for generic types: `new TypeReference<List<CourseResponse>>() {}`
- [ ] **`JsonNode`** — tree model for dynamic JSON parsing without a fixed type
- [ ] **`jackson-datatype-jsr310`** — makes Jackson understand `LocalDate`, `LocalDateTime`
- [ ] **`Jackson2JsonRedisSerializer`** — Spring Data Redis serializer using Jackson
- [ ] **`writeValueAsString()` / `readValue()`** — the two core operations

---

## Concept 8 — MyBatis Plus

### 8a — Entities
- [ ] **`@TableName("t_user")`** — maps class to DB table
- [ ] **`@TableId(type = IdType.AUTO)`** — auto-increment primary key
- [ ] **`@TableField(exist = false)`** — Java field that is NOT a DB column
- [ ] **`BaseEntity`** — base class that auto-fills `createTime`, `updateTime`, `createBy`, `updateBy`

### 8b — Mappers
- [ ] **`BaseMapper<T>`** — provides free CRUD: `insert()`, `selectById()`, `updateById()`, `deleteById()`, `selectList()`, `selectPage()`
- [ ] **`@Mapper`** — marks interface for MyBatis proxy generation
- [ ] **`@MapperScan`** — scans packages for `@Mapper` interfaces
- [ ] **`@Param("name")`** — required when a mapper method has multiple parameters
- [ ] **Default interface methods** — implement simple queries directly in the interface

### 8c — Query Building
- [ ] **`LambdaQueryWrapper`** — type-safe query builder using method references
- [ ] **`LambdaUpdateWrapper`** — type-safe update builder
- [ ] **`LambdaQueryChainWrapper`** — fluent API: `new LambdaQueryChainWrapper<>(mapper).eq(...).list()`
- [ ] **Common operators**: `eq`, `ne`, `gt`, `lt`, `like`, `in`, `isNull`, `isNotNull`, `orderByDesc`, `orderByAsc`
- [ ] **Terminal operations**: `.one()`, `.list()`, `.count()`
- [ ] **`selectOne()`, `selectList()`, `selectById()`** — `BaseMapper` methods

### 8d — Pagination
- [ ] **`Page<T>` + `IPage<T>`** — request + response pagination objects
- [ ] **`PaginationInnerInterceptor`** — adds LIMIT/OFFSET automatically. Must be configured in `MybatisConfig`

### 8e — XML Mapper Files
- [ ] **`<select id="..." resultType="...">` and `<update>`, `<insert>`, `<delete>`**
- [ ] **`<where>` tag** — smart WHERE clause (strips leading AND)
- [ ] **`<if test="condition">`** — OGNL conditional SQL
- [ ] **`<foreach>`** — generates `IN (...)` clauses
- [ ] **`<include refid="...">` + `<sql id="...">`** — reusable SQL fragments

### 8f — Auto-fill
- [ ] **`MetaObjectHandler`** — intercepts insert/update to auto-fill fields
- [ ] **`strictInsertFill()` / `strictUpdateFill()`** — fill specific fields conditionally

---

## Concept 9 — Transactions

- [ ] **`@Transactional`** — wrap method in a DB transaction. Commits on success, rolls back on RuntimeException
- [ ] **`@Transactional(rollbackFor = Exception.class)`** — roll back on any exception including checked
- [ ] **Self-invocation problem** — calling a `@Transactional` method from within the same class bypasses the proxy
- [ ] **`PlatformTransactionManager`** — programmatic transactions. `getTransaction()`, `commit()`, `rollback()`
- [ ] **`DataSourceTransactionManager`** — the concrete JDBC implementation

---

## Concept 10 — Bean Validation

- [ ] **`@NotNull`, `@NotBlank`, `@NotEmpty`** — null/empty/blank checks
- [ ] **`@Size(min=, max=)`** — string/collection length
- [ ] **`@Min`, `@Max`** — numeric range
- [ ] **`@Email`** — email format
- [ ] **`@Pattern(regexp = "...")`** — regex match
- [ ] **`@Valid`** — triggers validation on a method parameter (must be on the controller parameter)
- [ ] **`BindingResult`** — captures errors (must immediately follow the `@Valid` parameter)
- [ ] **Constraints on DTOs, not entities** — the VO/request class has constraints; the entity does not

---

## Concept 11 — MapStruct

- [ ] **`@Mapper`** — MapStruct mapper interface (not MyBatis `@Mapper` — different library)
- [ ] **`Mappers.getMapper(MyMapping.class)`** — get the generated implementation
- [ ] **`INSTANCE` static singleton** — `MyMapping.INSTANCE.toEntity(request)` usage pattern
- [ ] **`@Mapping(target = "...", source = "...")`** — map different field names
- [ ] **`@Mapping(target = "...", ignore = true)`** — skip a field
- [ ] **`@Mappings({...})`** — group multiple mappings
- [ ] **Compile-time generation** — look in `target/generated-sources/annotations/` to see generated code
- [ ] **`mapstruct-processor` in `<scope>provided</scope>`** — annotation processor, not bundled in JAR

---

## Concept 12 — Redis & Caching

### 12a — Spring Data Redis
- [ ] **`RedisTemplate<K, V>`** — primary Redis abstraction
- [ ] **`StringRedisTemplate`** — pre-configured for `String` keys and values
- [ ] **`RedisConnectionFactory`** — auto-configured from YAML
- [ ] **Key serializer (`StringRedisSerializer`) vs value serializer (`Jackson2JsonRedisSerializer`)**
- [ ] **`opsForValue()`, `opsForHash()`, `opsForSet()`, `opsForList()`, `opsForZSet()`**
- [ ] **TTL**: `template.opsForValue().set(key, value, Duration.ofMinutes(30))`
- [ ] **Invalidation**: `template.delete(key)`

### 12b — Guava Local Cache
- [ ] **`LoadingCache<K, V>`** — in-process cache. No network. Microsecond reads
- [ ] **`CacheBuilder.newBuilder().maximumSize(1000).build(CacheLoader)`**
- [ ] **`CacheLoader.load(key)`** — called on cache miss
- [ ] **`cache.get(key)`** — returns cached value or calls `load()`
- [ ] **`cache.invalidate(key)`** — removes entry
- [ ] **`CacheLoader.InvalidCacheLoadException`** — thrown when `load()` returns null
- [ ] **When to use**: local cache for rarely-changing reference data read by one service; Redis for shared state across pods

### 12c — ShedLock
- [ ] **Problem**: `@Scheduled` fires on every pod simultaneously in Kubernetes
- [ ] **Solution**: ShedLock ensures only one pod holds the lock per execution
- [ ] **`@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")`** — ISO-8601 duration
- [ ] **`@SchedulerLock(name = "unique:name")`** — unique per job
- [ ] **`RedisLockProvider`** — the Redis-backed lock provider bean
- [ ] **`lockAtMostFor`**: force-release the lock after this duration (crash safety). **`lockAtLeastFor`**: minimum hold time (prevents re-entry on fast jobs)

---

## Concept 13 — Scheduling & Async

- [ ] **`@EnableScheduling`** — activates `@Scheduled`
- [ ] **`@Scheduled(cron = "0 0 8 * * ?")`** — 6-field cron (second, minute, hour, day, month, weekday)
- [ ] **`@EnableAsync`** — activates `@Async`
- [ ] **`@Async("poolName")`** — method runs on the named thread pool. Without a name, uses a single-threaded default (bad)
- [ ] **`ThreadPoolTaskExecutor`** — Spring thread pool bean
  - `corePoolSize` — always-on threads
  - `maxPoolSize` — maximum concurrent threads
  - `queueCapacity` — tasks queued when all core threads are busy
  - `keepAliveSeconds` — idle thread expiry
  - `rejectedExecutionHandler` — what happens when queue is full
- [ ] **`CallerRunsPolicy`** — queue full → calling thread executes the task (slows caller, no drops)
- [ ] **`CompletableFuture`** — async computation result. `.allOf(f1, f2).join()` waits for all

---

## Concept 14 — Spring Cloud OpenFeign

- [ ] **`@FeignClient(value = "service-name", path = "/prefix", contextId = "uniqueId")`**
- [ ] **`@EnableFeignClients(basePackages = "...")`** — scans for Feign interfaces
- [ ] **Feign method annotations mirror controller annotations exactly**
- [ ] **`@SpringQueryMap`** — serialize a POJO as GET query parameters
- [ ] **`contextId`** — required when two clients point to the same `value` (prevents bean name conflict)
- [ ] **`ReturnWrapper<T>` as return type** — matching the server's response structure
- [ ] **`RequestInterceptor`** — Feign hook to add headers to every outgoing call (used for context propagation)
- [ ] **Local dev service discovery** — `spring.cloud.discovery.client.simple.instances.service-name[0].uri: http://localhost:PORT`

---

## Concept 15 — Apache Kafka

- [ ] **Topics** — named channels. Use constants for topic name strings
- [ ] **`@KafkaListener(topics = "...", groupId = "...")`** — consume from a topic
- [ ] **`ConsumerRecord<?, ?>`** — the raw Kafka record. Has `.topic()`, `.value()` (raw bytes)
- [ ] **`Acknowledgment.acknowledge()`** — manually commit offset. Required when using `AckMode.MANUAL`
- [ ] **Consumer group semantics** — one message per consumer group. Multiple groups = all get the message. Multiple pods in same group = only one gets each message
- [ ] **`EventMessageDTO<T>`** — standard envelope: `{ type, body }`
- [ ] **Message deserialization**: `byte[] bytes = (byte[]) record.value()` → `String message = new String(bytes)` → `objectMapper.readValue()`
- [ ] **`@Async` on producers** — Kafka sends are async; don't block the caller
- [ ] **Error handling in consumers** — log but consider whether to rethrow (Kafka will retry on exception)

---

## Concept 16 — AOP

- [ ] **`@Aspect`** — marks a class as an aspect
- [ ] **`@Around`** — surrounds the method call. You call `joinPoint.proceed()` to execute it
- [ ] **`@Pointcut`** — reusable expression for which methods to intercept
- [ ] **`ProceedingJoinPoint`** — the intercepted call. `proceed()`, `getArgs()`, `getSignature()`
- [ ] **`MethodSignature`** — cast from `Signature`. Gives `getParameterNames()`, `getMethod()`
- [ ] **Custom annotation as pointcut**: `@Around("(execution(* *(..)) && @annotation(myAnnotation))")` — intercept methods annotated with your custom annotation
- [ ] **Creating a custom annotation**: `@Target(ElementType.METHOD)`, `@Retention(RetentionPolicy.RUNTIME)`, `@Documented`
- [ ] **`ReflectionUtils`** — `findField()`, `makeAccessible()`, `getField()` for dynamic field access
- [ ] **`AtomicReference<Object>`** — thread-safe holder for capturing return values across lambdas

---

## Concept 17 — Spring Boot Application Lifecycle

- [ ] **`InitializingBean.afterPropertiesSet()`** — fires after `@Autowired` injection, before HTTP server. Used for internal state setup that needs dependencies
- [ ] **`ApplicationRunner.run(ApplicationArguments)`** — fires after HTTP server is ready. Safe for calling any bean, including Feign clients
- [ ] **`CommandLineRunner.run(String...)`** — same timing as `ApplicationRunner`, gets raw CLI args
- [ ] **`@PostConstruct`** — annotation equivalent of `afterPropertiesSet()`
- [ ] **Startup order**: IoC builds → injection → `@PostConstruct/afterPropertiesSet` → HTTP server starts → `ApplicationRunner/CommandLineRunner`
- [ ] **Why NOT in constructor** — `@Autowired` fields are null in the constructor

---

## Concept 18 — OpenSearch / Elasticsearch

- [ ] **`@Document(indexName = "...")`** — maps class to an OS/ES index
- [ ] **`@Field(type = FieldType.Text)`** — full-text searchable (analyzed, stemmed)
- [ ] **`@Field(type = FieldType.Keyword)`** — exact-match only (not analyzed). For filters, sorting, aggregations
- [ ] **`@Field(type = FieldType.Long / Date)`** — typed numeric/date fields
- [ ] **`Repository extends EnhanceElasticSearchRepository<T, ID>`** — provides standard data access methods
- [ ] **`@Repository`** on the interface
- [ ] **`EsEntity`** — base class for indexed documents
- [ ] **Index aliases** — the actual index has a date suffix; an alias points to it. Enables zero-downtime reindexing

---

## Concept 19 — Spring Cloud (Config + Discovery)

- [ ] **Nacos** — centralized config management + service discovery server
- [ ] **`spring-cloud-starter-alibaba-nacos-config`** — enables Nacos config
- [ ] **`extension-configs[n]`** — pull specific YAML files from Nacos at startup
- [ ] **`refresh: true`** — live config updates without restart
- [ ] **`bootstrap.yml`** loads before `application.yml` — required for Nacos config to be available at Spring Boot initialization time
- [ ] **Kubernetes Service** — acts as load balancer for pods. Feign resolves service name to K8s Service
- [ ] **`spring.cloud.kubernetes.discovery.*`** — namespace-scoped service discovery
- [ ] **Local dev override** — `spring.cloud.discovery.client.simple.instances.{name}[0].uri: http://localhost:PORT`

---

## Concept 20 — OpenAPI / Swagger (Knife4j)

- [ ] **`@Tag(name = "...")`** — labels a controller in the UI
- [ ] **`@Operation(summary = "...", description = "...")`** — documents an endpoint
- [ ] **`@Schema(description = "...", example = "...")`** — documents a field
- [ ] **`@Parameter`** — documents a query/path parameter
- [ ] **Knife4j** — enhanced Swagger UI. Access at `/doc.html`
- [ ] **Generated spec** at `/v3/api-docs` — importable into Postman

---

## Concept 21 — PageHelper

- [ ] **`PageHelper.startPage(page, size)`** — call immediately before the mapper query
- [ ] **`PageInfo<T>`** — wraps results: `total`, `pages`, `pageNum`, `pageSize`, `list`
- [ ] **Critical constraint** — only the very next query after `startPage()` gets paginated
- [ ] **`pagehelper-spring-boot-starter`** — auto-configures for your SQL dialect

---

## Concept 22 — Apache POI

- [ ] **`XSSFWorkbook`** — `.xlsx` workbook
- [ ] **`XSSFSheet`, `Row`, `Cell`** — hierarchy
- [ ] **`cell.getCellType()`** — check type before getting value
- [ ] **`cell.getStringCellValue()`, `cell.getNumericCellValue()`**
- [ ] **`row.createCell(colNum)`, `cell.setCellValue(...)`** — writing
- [ ] **`XSSFCellStyle`, `XSSFFont`** — styling
- [ ] **`workbook.write(outputStream)`** — serialize to stream
- [ ] **`Closeable` pattern** — try-with-resources guarantees cleanup
- [ ] **`MultipartFile.getInputStream()`** — read uploaded file

---

## Concept 23 — MinIO

- [ ] **Object storage** — stores binary files as objects in named buckets. Better than DB blobs
- [ ] **Bucket** — top-level namespace for objects
- [ ] **Object key** — the path within a bucket (`users/42/avatar.jpg`)
- [ ] **`MinioClient`** — the Java client
- [ ] **`PutObjectArgs`** — upload an object
- [ ] **`GetPresignedObjectUrlArgs`** — generate a temporary URL for download/display
- [ ] **`BucketExistsArgs + MakeBucketArgs`** — idempotent bucket creation
- [ ] **Store the key in DB, not the full URL** — URL may change across environments

---

## Concept 24 — SSE

- [ ] **Server-Sent Events** — one-way, server-to-client push over HTTP. Simpler than WebSockets
- [ ] **`SseEmitter`** — Spring's SSE class
- [ ] **`new SseEmitter(0L)`** — no timeout
- [ ] **`emitter.send(SseEmitter.event().name("event-name").data(payload))`** — push an event
- [ ] **`onCompletion()`, `onTimeout()`, `onError()`** — lifecycle callbacks
- [ ] **`ConcurrentHashMap<String, SseEmitter>`** — thread-safe emitter registry
- [ ] **Controller returns `SseEmitter`** — Spring holds the HTTP connection open

---

## Concept 25 — Jinjava

- [ ] **`Jinjava`** — Java's Jinja2 template engine
- [ ] **`{{ variable }}`** — variable output
- [ ] **`{% if ... %}...{% endif %}`** — conditional
- [ ] **`{% for item in list %}...{% endfor %}`** — iteration
- [ ] **`jinjava.render(template, Map<String, Object> context)`** — render with data
- [ ] **`JavaMailSender`** — Spring's SMTP email abstraction

---

## Concept 26 — Context Propagation

- [ ] **`ThreadLocal<T>`** — one value per thread. Reads from anywhere in the same thread's call stack
- [ ] **`ThreadLocal.remove()`** — must be called in `finally` to prevent memory leaks
- [ ] **`HandlerInterceptor`** — Spring MVC hook. `preHandle` = read headers, set context. `afterCompletion` = clear context
- [ ] **`RequestInterceptor` (Feign)** — add context as headers to every outgoing Feign call
- [ ] **`TransmittableThreadLocal` (TTL)** — propagates context to child threads (`@Async`). Regular `ThreadLocal` doesn't survive thread hops

---

## Concept 27 — Docker & Containerization

- [ ] **Multi-stage Dockerfile** — build stage compiles; production stage has only the JAR
- [ ] **`COPY --from=build-stage`** — copies artifact between stages
- [ ] **`ENV JAVA_OPTS`** — JVM flags
- [ ] **`-XX:+UseZGC -XX:+ZGenerational`** — Java 21 low-latency GC
- [ ] **`-Xms / -Xmx`** — heap size
- [ ] **`--add-opens`** — Java module system workarounds
- [ ] **`-javaagent:opentelemetry-javaagent.jar`** — auto-instrumentation
- [ ] **`docker-compose.yml`** — local multi-container orchestration

---

## Concept 28 — GitLab CI/CD

- [ ] **`.gitlab-ci.yml`** — pipeline definition
- [ ] **`stages`** — ordered phases
- [ ] **`image`** — Docker image for the CI job
- [ ] **`when: manual`** — human trigger required
- [ ] **`services`** — sidecar containers (Docker-in-Docker for building images)
- [ ] **`include`** — reuse pipeline templates from another project
- [ ] **Image tag pattern** — `${version}-${branch}-${commit_sha}` for traceability

---

## Concept 29 — Java 21 Features

- [ ] **Switch expressions** — `String s = switch (x) { case A -> "a"; default -> "other"; };`
- [ ] **`var`** — local variable type inference
- [ ] **Records** — `record Point(int x, int y) {}` — immutable data class
- [ ] **Stream API** — `list.stream().filter(...).map(...).toList()`
- [ ] **`.toList()`** — Java 16 shorthand for unmodifiable list
- [ ] **`List.of()`, `Map.of()`** — immutable collection literals
- [ ] **Pattern matching `instanceof`** — `if (obj instanceof String s) { use s; }`
- [ ] **Text blocks** — `"""multi\nline"""` — raw multi-line strings

---

## Concept 30 — The Internal `agile-*` Framework

You cannot see the source code of these modules. Learn what each provides:

- [ ] **`agile-core`** — `EventMessageDTO<T>`, `@EnableWebRequestContext`, request context plumbing
- [ ] **`agile-orm`** — `BaseEntity` (auto timestamps), `EntityMetaObjectHandler` (auto fill `createBy/updateBy`)
- [ ] **`agile-mq`** — `MessageProducer` (Kafka send wrapper), `@EnableKafka`
- [ ] **`agile-cache`** — `@EnableRedis`, Redis auto-configuration
- [ ] **`agile-io`** — `MinioTemplate` (MinIO operations wrapper)
- [ ] **`agile-observe`** — OpenTelemetry integration
- [ ] **`agile-es`** — `EnhanceElasticSearchRepository<T, ID>`, `EsEntity`, `@Enhance`

---

## Quick Reference: Annotation Cheat Sheet

| Annotation | Library | Where Used | Purpose |
|---|---|---|---|
| `@SpringBootApplication` | Spring Boot | Main class | Entry point |
| `@Configuration` | Spring | Config class | Declares `@Bean` methods |
| `@Bean` | Spring | Method in `@Configuration` | Return value becomes a bean |
| `@Component` | Spring | Any class | Generic Spring bean |
| `@Service` | Spring | Business logic class | Same as `@Component` semantically |
| `@Repository` | Spring | Data access class | Same as `@Component` semantically |
| `@RestController` | Spring Web | Controller | Returns JSON |
| `@RequestMapping` | Spring Web | Class or method | URL mapping |
| `@GetMapping` | Spring Web | Method | GET endpoint |
| `@PostMapping` | Spring Web | Method | POST endpoint |
| `@PathVariable` | Spring Web | Parameter | URL segment binding |
| `@RequestParam` | Spring Web | Parameter | Query string binding |
| `@RequestBody` | Spring Web | Parameter | JSON body binding |
| `@Autowired` | Spring | Field/constructor | Inject a bean |
| `@Value` | Spring | Field | Inject a config value |
| `@ConfigurationProperties` | Spring Boot | Class | Bind a config block |
| `@Transactional` | Spring TX | Method | DB transaction |
| `@Scheduled` | Spring | Method | Cron job |
| `@Async` | Spring | Method | Run on thread pool |
| `@ControllerAdvice` | Spring Web | Class | Global exception handler |
| `@ExceptionHandler` | Spring Web | Method | Handle exception type |
| `@FeignClient` | Spring Cloud | Interface | HTTP client |
| `@KafkaListener` | Spring Kafka | Method | Consume from topic |
| `@Mapper` (MyBatis) | MyBatis | Interface | MyBatis proxy |
| `@TableName` | MyBatis Plus | Entity class | Maps to DB table |
| `@TableId` | MyBatis Plus | Field | Primary key |
| `@TableField` | MyBatis Plus | Field | Column or exclude |
| `@MapperScan` | MyBatis | Config class | Scan for `@Mapper` |
| `@Mapper` (MapStruct) | MapStruct | Interface | Object mapping |
| `@Mapping` | MapStruct | Method | Field mapping rule |
| `@Aspect` | AspectJ | Class | AOP aspect |
| `@Around` | AspectJ | Method | Around advice |
| `@Pointcut` | AspectJ | Method | Interception expression |
| `@Data` | Lombok | Class | All boilerplate |
| `@Slf4j` | Lombok | Class | Inject `log` field |
| `@NotNull` | Jakarta | DTO field | Validation constraint |
| `@NotBlank` | Jakarta | DTO field | Validation constraint |
| `@Valid` | Jakarta | Parameter | Trigger validation |
| `@Document` | Spring Data ES | Entity | Maps to ES index |
| `@Field` | Spring Data ES | Field | ES field mapping |
| `@SchedulerLock` | ShedLock | Method | Distributed cron lock |
| `@Operation` | Springdoc | Method | Documents API endpoint |
| `@Schema` | Springdoc | Field | Documents a field |
| `@Tag` | Springdoc | Class | Labels a controller |
| `@LogPrint` | This codebase | Method | Auto-log params |
| `@Idempotent` | This codebase | Method | Redis idempotency guard |

---

## Suggested Study Path

> This is not a 2-week curriculum. Given you are a few months into Java/Spring Boot, this is realistic:

| Phase | Weeks | Focus |
|---|---|---|
| 1–6 | Weeks 1–6 | Java basics → REST API → Exceptions → DB → Transactions |
| 7–9 | Weeks 7–9 | Multi-module Maven → MapStruct → AOP |
| 10–12 | Weeks 10–12 | Redis → Guava Cache → Feign |
| 13–15 | Weeks 13–15 | Context propagation → Kafka → Scheduling |
| 16–18 | Weeks 16–18 | Async → Idempotency → Lifecycle |
| 19–21 | Weeks 19–21 | POI → MinIO → OpenSearch |
| 22–25 | Weeks 22–25 | SSE → Jinjava → OpenAPI → Spring Cloud |
| 26–28 | Weeks 26–28 | Docker → CI/CD → Java 21 features |

**Total: 6–7 months of consistent, hands-on work.**

Do not rush. The only way to master a phase is to build it yourself, break it, debug it, and rebuild it.
