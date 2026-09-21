# Tech User Service — Проверка готовности к развёртыванию

> **Дата**: 21 сентября 2026 (обновлено после повторного аудита кода + прогона тестов)
> **Статус**: ✅ **ГОТОВ КАК АВТОНОМНЫЙ СЕРВИС (~90%)** / ⚠️ **К МЕЖСЕРВИСНОЙ ИНТЕГРАЦИИ НЕ ГОТОВ (контракты есть, plumbing — 0%)**
> **Архитектурная спецификация**: tech-store README.md (6 сервисов + Gateway)
> **Текущая реализация**: Java 17, Spring Boot 4.1.1, PostgreSQL, MapStruct, Spring Security + JWT (jjwt 0.11.5), Flyway (V1–V3)
> **Ветка**: `feature-metrics&log` (поверх `f105717`, merge PR #7). ⚠️ **Незакоммиченные изменения**: `observability/` (MetricsConfig, MdcLoggingFilter, OpenApiConfig), `logback-spring.xml`, правки `SecurityConfig`, `JwtAuthenticationFilter`, `AuthServiceImpl`, `application.properties`, `compose.yaml`, `.env.example`, `SecurityConfigTest`, `observability/*Test` — до merge в `master`/`main` production-сборка их не содержит.

---

## 📊 Общая оценка готовности

| Компонент | Статус | Комментарий |
|-----------|--------|-------------|
| Entity-модель | ✅ Готово | `entity/User`, `Address`, `Role`, `Permission`, `RefreshToken` (перенос из `model/` выполнен) |
| Repository-слой | ✅ Готово | 5 репозиториев (`User`, `Address`, `Role`, `Permission`, `RefreshToken`) |
| DTO и Mapper | ✅ Готово | 12+ DTO (`UserRequest/Response`, `UpdateUserRequest`, `AuthRequest/Response/Refresh`, `AddressDTO`, `RoleDTO`, `RoleUpdateDTO`, `PageResponseDTO`, `ApiError`, `PasswordResetNotification`) + 4 MapStruct-маппера |
| Контроллеры | ✅ Тонкие, делегируют в сервисы | `AuthController` (register/login/refresh/logout/me), `UserController` (CRUD + role/active/reset + `@PreAuthorize`), `AddressController` (CRUD + default + `SELF_OR_STAFF`) |
| Service Layer | ✅ Готово | `AuthServiceImpl` (register/authenticate/refresh/logout, ротация `tokenVersion`, `@Timed`), `UserServiceImpl` (merge-семантика `assignRole`, `removeRole` с защитой последней роли, `resetPassword` с временным паролем), `AddressServiceImpl`, `JwtServiceImpl`, `RoleServiceImpl` + `PermissionServiceImpl`, `RoleDataSeeder`, `NotificationService` (порт) + `LoggingNotificationService` (заглушка) |
| Security / JWT | ✅ Готово (базово) | `@EnableMethodSecurity`, `@PreAuthorize` на всех методах, `SecurityConfig` (stateless, entry point 401 JSON + access denied 403 JSON, CORS, actuator-матчеры, дубль-защита URL+аннотации), `JwtAuthenticationFilter` (кладёт `userId` в MDC) |
| Application Config | ✅ Готово | Env-only секреты (`DB_*`, `JWT_SECRET`), `ddl-auto=validate`, `open-in-view=false`, CORS/actuator/логирование через env, `springdoc` группы |
| Обработка ошибок | ✅ Готово | `GlobalExceptionHandler` → единый `ApiError` (`timestamp/status/error/message/path/errors`): 404, 409 (`DuplicateEmail` + `DataIntegrityViolation`), 401/403 JSON, 400 со ВСЕМИ ошибками валидации, 500 без утечек |
| Миграции БД | ✅ Готово | Flyway `V1__init` (7 таблиц, FK `ON DELETE CASCADE`, индексы) + `V2__seed_roles_permissions` (USER/MANAGER/ADMIN + 5 permissions, идемпотентно) + `V3__refresh_token_unique` |
| Тесты | ✅ Готово (базово) | **21 тестовый файл**, зелёные (`UserControllerTest` 9/9, `SecurityConfigTest` 1/1, `BUILD SUCCESS` 21.09.2026): unit (services/security), `@SpringBootTest` + Testcontainers (controllers, repositories, `SecurityIntegrationTest`, `ObservabilityIntegrationTest`, `AuthHappyPathTest`) |
| Docker / Compose | ✅ Готово | Многостадийный `Dockerfile` (Maven → slim JRE + curl для healthcheck), `compose.yaml` (postgres:16 + app, `env_file`, healthcheck `pg_isready` + `/actuator/health`, volume `pgdata`, `depends_on: service_healthy`), `.env.example` |
| Актуатор / Мониторинг | ✅ Готово (на ветке, незакоммичено) | `spring-boot-starter-actuator` + `micrometer-registry-prometheus` в `pom.xml`; `management.*` (exposure/health details/probes/percentiles), `MetricsConfig` (`@Timed`), `MdcLoggingFilter` (`traceId` из `X-Request-Id` + `userId`, эхо заголовка), `logback-spring.xml` (единый формат `[traceId userId]`), `OpenApiConfig` (JWT bearer + группы users/auth/addresses/actuator) |
| Интеграция с другими сервисами | ❌ Не начата | Нет Feign, Eureka/Consul, RabbitMQ/Kafka, Resilience4j, Config Client (grep — 0 совпадений вне этого файла); только in-process `LoggingNotificationService` вместо событий |

---

## ✅ Что уже готово (подтверждено кодом 21.09.2026)

| Компонент | Файлы |
|-----------|-------|
| Контроллеры | `controller/AuthController.java`, `controller/UserController.java:22-76` (`@PreAuthorize`: листинг/role/active/reset/delete — `ADMIN/MANAGER`, чтение/обновление — self-or-staff), `controller/AddressController.java:22-26` (`SELF_OR_STAFF`) |
| Сервисы | `service/impl/AuthServiceImpl.java:62-97` (register: `existsByEmail` → 409, `getOrCreate(USER)` — пустых `roles` больше нет), `service/impl/UserServiceImpl.java:132-195` (merge `assignRole`, `removeRole`, `resetPassword` с `SecureRandom`-паролем + `revokeAllByUserId` + `NotificationService.sendPasswordReset`), `service/impl/RoleServiceImpl.java` (дефолтные permissions = V2), `service/impl/RoleDataSeeder.java`, `service/NotificationService.java` + `service/impl/LoggingNotificationService.java` |
| Security | `security/SecurityConfig.java:32-101` (`@EnableMethodSecurity`, entry points `ApiError` 401/403, CORS из env, actuator: health/info public, остальное `ADMIN/MANAGER`), `security/JwtAuthenticationFilter.java`, `security/CustomUserDetailsServiceImpl.java`, `security/PasswordEncoderImpl.java`, `security/JwtConfig.java` |
| Конфиг | `src/main/resources/application.properties:1-70` — `server.port=${SERVER_PORT:8001}`, datasource env-only, `ddl-auto=validate`, Flyway, JWT env, `management.*`, CORS env, logging MDC-паттерн, springdoc группы |
| Ошибки | `exception/GlobalExceptionHandler.java:36-120` + `dto/error/ApiError.java` + `DuplicateEmailException` (409, больше не `SecurityException`/401) |
| Миграции | `db/migration/V1__init.sql` (users/roles/permissions/user_roles/role_permissions/addresses/refresh_tokens), `V2__seed_roles_permissions.sql`, `V3__refresh_token_unique.sql` |
| Наблюдаемость (ветка) | `observability/MetricsConfig.java`, `observability/MdcLoggingFilter.java`, `observability/OpenApiConfig.java`, `logback-spring.xml` |
| Тесты (21 файл) | `controller/*Test` (User/Address/Auth), `security/SecurityIntegrationTest`, `security/SecurityConfigTest`, `repository/*Test` (Testcontainers), `service/impl/*Test` + `AuthHappyPathTest`, `observability/ObservabilityIntegrationTest`, `exception/ExceptionTest` — прогон 21.09.2026 зелёный |
| Упаковка | `Dockerfile`, `compose.yaml:1-54`, `.env.example:1-31` |
| `pom.xml` | Security, Data JPA, WebMVC, validation, jjwt, springdoc, postgres, lombok, mapstruct, **actuator + flyway + flyway-postgresql + micrometer-prometheus**, testcontainers/security-test/webmvc-test |

---

## 🔌 Готовность к интеграции с другими сервисами (аудит 21.09.2026)

### Что сервис уже отдаёт (контракты готовы к употреблению)

| Потребитель | Что ему нужно от User Service | Статус на нашей стороне |
|-------------|-------------------------------|-------------------------|
| **API Gateway (8000)** | Маршрутизация `/api/auth/**`, `/api/users/**`; валидация JWT; CORS; `X-Request-Id` сквозной; health-пробы для LB | ✅ Готово: Bearer-JWT (`sub=userId` + роль), CORS из env, `MdcLoggingFilter` принимает/возвращает `X-Request-Id`, `GET /actuator/health|info` public, OpenAPI `/v3/api-docs/{users,auth,addresses}`. Нужен только Gateway-маршрут и **общий `JWT_SECRET`** (сейчас dev-дефолт в properties + `.env.example`) |
| **Order Service (8004)** | Проверить существование пользователя + принадлежность `shippingAddressId` (`OrderRequestDTO.shippingAddressId` по README) | ⚠️ Частично: `GET /api/users/{id}` + `GET /api/users/{userId}/addresses` отдают данные, но требуют **пользовательский JWT**; отдельного **service-to-service** доступа (m2m-токен/API-ключ, `GET /internal/users/{id}/exists`) нет — Order не сможет валидировать без токена пользователя |
| **Review Service (8006)** | Валидация автора отзыва (`ReviewResponseDTO.userId`), проверка «покупал ли товар» | ⚠️ Частично: пользователь по id читается, но кросс-проверка «user × order» не спроектирована; события покупок не слушаем (брокера нет) |
| **Notification (будущий)** | Доставка временного пароля при `resetPassword` | ⚠️ Контракт зафиксирован (`NotificationService` + `PasswordResetNotification{userId,email,temporaryPassword}`), но реализация — `LoggingNotificationService` (лог). Событие в RabbitMQ не публикуется |
| Все потребители | Единый формат ошибок для Feign-декодеров | ✅ `ApiError` на всех `4xx/5xx`, включая security entry points (401/403 JSON, а не HTML/пусто) |

### Чего нет (блокеры интеграции, P3)

1. **Service discovery**: нет `spring-cloud-starter-netflix-eureka-client` / Consul — все URL пришлось бы хардкодить. В `pom.xml`/`application.properties` нет `eureka.*`, в коде нет `@EnableDiscoveryClient`.
2. **Синхронные клиенты**: нет OpenFeign (`@EnableFeignClients`, клиенты `Order/Inventory/Payment/Catalog`), нет Resilience4j (circuit breaker/retry/timeout). Внешних вызовов сервис не делает и принимать от сервисов по m2m не умеет.
3. **Асинхронные события**: нет `spring-amqp`/`spring-kafka`, нет `RabbitMQConfig`, нет `UserRegisteredEvent` / `PasswordResetRequestedEvent` / consumer'ов (`OrderCreatedEvent` и т.д. по README). Единственный «event» — in-process вызов заглушки.
4. **Service-to-service auth**: нет концепции m2m (отдельный issuer/роль `SERVICE`, API-ключ, `permitAll` для `/internal/**` с сетевой изоляцией). Текущие `@PreAuthorize` понимают только пользовательский JWT с `entity.User` в principal.
5. **Контрактные тесты**: нет Spring Cloud Contract / Pact; изменение `UserResponseDTO`/`AddressDTO`/`ApiError` не ловится на стороне потребителей.
6. **Инфраструктура tech-store**: другие 5 сервисов, Gateway, `tech-common-*`, root `docker-compose.yml` (Postgres ×4 + Mongo + RabbitMQ + Eureka + 6 сервисов), `kubernetes/` — отсутствуют (в репозитории только `tech-user-service`; `tech-store/` содержит только `README.md`).
7. **Незакоммиченная observability-ветка**: `feature-metrics&log` не смержена — любой потребитель, собирающий `/actuator/prometheus` или `X-Request-Id`, зависит от merge.

---

## 📋 План реализации (обновлённый)

| Приоритет | Задача | Оценка |
|-----------|--------|--------|
| **P0** | Смержить `feature-metrics&log` в `master`/`main` (сейчас production не содержит observability-правки) | 0.5 дня |
| **P3** | Определить стратегию JWT для Gateway/сервисов (общий `JWT_SECRET` из vault vs Gateway-only валидация + `X-User-Id` downstream) и задокументировать в OpenAPI | 0.5–1 день |
| **P3** | Internal API для m2m: `GET /internal/users/{id}/exists`, `GET /internal/users/{userId}/addresses/{addressId}` + service-креды (роль `SERVICE`) | 1–2 дня |
| **P3** | Eureka Client (или Consul) + регистрация, health-пробы для discovery | 1 день |
| **P3** | RabbitMQ: `spring-amqp`, `RabbitMQConfig`, publish `UserRegisteredEvent`/`PasswordResetRequestedEvent`, consumer-заглушки под `Order*`/`Payment*` события README | 3–5 дней |
| **P3** | OpenFeign-клиенты (если User Service будет дёргать других) + Resilience4j + Feign `ErrorDecoder` под `ApiError` | 2–3 дня |
| **P3** | Контрактные тесты (Spring Cloud Contract / Pact) на `UserResponseDTO`/`AddressDTO`/`ApiError` | 2–3 дня |
| **P3** | Остальные 5 сервисов + Gateway + common + root compose + K8s | 3–4 недели |

---

## 🎯 Заключение

**`tech-user-service` как автономный сервис (~90%)**: P0/P1/P2 из прошлой версии файла закрыты и подтверждены кодом и зелёными тестами — ролевой доступ (`@PreAuthorize` + URL-матчеры + 401/403 JSON), Flyway V1–V3 + `validate`, env-конфиг, `ApiError`, Docker/compose с healthcheck, Testcontainers-тесты, actuator/Prometheus/MDC/OpenAPI. Осталось: **смержить `feature-metrics&log`** и держать dev-дефолт `JWT_SECRET` вне prod.

**К интеграции с другими сервисами (~40%: контракты есть, транспорта нет)**: REST-контракты (`/api/auth`, `/api/users`, `/api/users/{id}/addresses`), JWT, `ApiError`, health/info, `X-Request-Id`, OpenAPI-группы — готовы и достаточны, чтобы Gateway и Order/Review начинали проектироваться поверх. Но **plumbing отсутствует полностью**: discovery, Feign, RabbitMQ, m2m-auth, контрактные тесты — **~1.5–2.5 недели** на сам User Service. Полный `tech-store` (5 сервисов + Gateway + брокер + discovery + root compose/K8s) — по-прежнему **~4–6 недель** после добивки.
