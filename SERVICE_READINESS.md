# Tech User Service — Проверка готовности к развёртыванию

> **Дата**: 21 сентября 2026 (обновлено после аудита кода)
> **Статус**: ⚠️ **ЧАСТИЧНО ГОТОВ** — ядро реализовано, до production не готов
> **Архитектурная спецификация**: tech-store README.md (6 сервисов + Gateway)
> **Текущая реализация**: Java 17, Spring Boot 4.1.1, PostgreSQL, MapStruct, Spring Security + JWT (jjwt 0.11.5)
> **Примечание**: предыдущая версия этого файла утверждала отсутствие Service Layer / Security / Config — это устарело. Ветки `feature-auth`, `feature-service` уже смержены в `master` (коммиты `d29acaa`, `396e42f`, `4178ac5`).

---

## 📊 Общая оценка готовности

| Компонент | Статус | Комментарий |
|-----------|--------|-------------|
| Entity-модель | ✅ Готово | `User`, `Address`, `Role`, `Permission`, `RefreshToken` |
| Repository-слой | ✅ Готово | 5 репозиториев (`User`, `Address`, `Role`, `Permission`, `RefreshToken`) |
| DTO и Mapper | ✅ Готово (почти) | 10 DTO + 4 MapStruct-маппера; нет `PermissionDTO` |
| Контроллеры | ✅ Тонкие, делегируют в сервисы | `AuthController`, `UserController`, `AddressController` |
| **Service Layer** | ✅ **Готово (базово)** | `AuthServiceImpl`, `UserServiceImpl`, `AddressServiceImpl`, `JwtServiceImpl` с `@Transactional` |
| **Security / JWT** | ✅ Базово / ⚠️ Неполно | Фильтр + `SecurityConfig` есть, но нет ролевой авторизации, CORS, entry points |
| **Application Config** | ⚠️ Минимально | Порт, datasource, jwt, springdoc есть; нет env, actuator, CORS, логирования |
| Обработка ошибок | ⚠️ Частично | `GlobalExceptionHandler` есть, но 4 хендлера, нет `ApiError`, неверные коды |
| Тесты | ⚠️ Только unit/mock | 13 тестовых файлов, нет интеграционных `@SpringBootTest` на контроллеры/репо |
| Docker / Compose | ⚠️ Минимально | Только `postgres:latest` без healthcheck/volumes/app-сервиса, нет `Dockerfile` |
| Интеграция с другими сервисами | ❌ Не начата | Нет Feign, Eureka, RabbitMQ/Kafka, Resilience4j (grep — 0 совпадений) |
| Актуатор / Мониторинг | ❌ Не настроен | В `pom.xml` только `actuator-test` + `prometheus`, нет `spring-boot-starter-actuator` и exposure |
| Миграции БД | ❌ Отсутствуют | Нет Flyway/Liquibase, `ddl-auto=update`, нет сида ролей |

---

## ✅ Что уже готово (подтверждено кодом)

| Компонент | Файлы |
|-----------|-------|
| Контроллеры (тонкие) | `controller/AuthController.java` (register/login/refresh/logout/me), `controller/UserController.java` (CRUD + role/active/reset), `controller/AddressController.java` (CRUD + default) |
| Сервисы | `service/impl/AuthServiceImpl.java` (register/authenticate/refresh/logout, ротация `tokenVersion`), `service/impl/UserServiceImpl.java`, `service/impl/AddressServiceImpl.java` (default-адрес, проверка принадлежности), `service/impl/JwtServiceImpl.java` |
| Security | `security/SecurityConfig.java` (stateless, `/api/auth/**` permitAll), `security/JwtAuthenticationFilter.java`, `security/CustomUserDetailsServiceImpl.java`, `security/PasswordEncoderImpl.java`, `security/JwtConfig.java`, `AuthenticationManager` bean |
| Конфиг | `src/main/resources/application.properties` — `server.port=8001`, datasource `mydatabase`, `jwt.secret`, `jwt.*-expiration-ms`, springdoc |
| Ошибки | `exception/GlobalExceptionHandler.java` (`UserNotFound`, `AddressNotFound`, `Security/BadCredentials`, `MethodArgumentNotValid`), `UserNotFoundException`, `AddressNotFoundException`, `SecurityException` |
| DTO/MapStruct | `UserRequest/ResponseDTO`, `UpdateUserRequestDTO`, `AuthRequest/ResponseDTO`, `AuthRefreshRequestDTO`, `AddressDTO`, `RoleDTO`, `RoleUpdateDTO`, `PageResponseDTO`; мапперы `UserMapper`, `AuthMapper`, `AddressMapper`, `RoleMapper` |
| Тесты (13 файлов) | `service/impl/*Test` (User/Auth/Address/Jwt), `security/*Test`, `controller/AuthControllerTest` (mockito `standaloneSetup`), `exception/ExceptionTest` |
| `pom.xml` | Security, Data JPA, WebMVC, validation, jjwt, springdoc, postgres, lombok, mapstruct, testcontainers |
| `compose.yaml` | Минимальный Postgres (`mydatabase/myuser/secret`, порт 5432) |

---

## ❌ Что осталось до полной реализации

### P0 — Безопасность и корректность (блокирует production)

1. **Ролевая авторизация отсутствует**
   - Нет `@EnableMethodSecurity`, нет `@PreAuthorize` — любой `authenticated` может вызывать `PUT /api/users/{id}/role`, `PUT .../active`, `DELETE` (см. `UserController.java:47-55`, `SecurityConfig.java:30-35`).
   - Нужно: разделить `USER` (только свой профиль/адреса) vs `MANAGER`/`ADMIN` (назначение ролей, блокировка, листинг).
2. **`assignRole` затирает роли** — `UserServiceImpl.java:124` `Set.of(role)`. Нужно merge/add-remove + отдельный `RoleService`/`PermissionService` (сейчас их нет).
3. **Миграции + сиды отсутствуют** — `ddl-auto=update`, Flyway/Liquibase нет. `AuthServiceImpl.java:71-74`: если роли `USER` нет в БД, пользователь сохранится с пустым `roles`. Нужно: `V1__init.sql` + `V2__seed_roles.sql` (`USER, MANAGER, ADMIN` + permissions), в prod `ddl-auto=validate`.
4. **Конфиг не production-ready** — секреты захардкожены в `application.properties:5-11`. Нужно: env-переменные (`${DB_URL}`, `${DB_USER}`, `${JWT_SECRET}`), `management.*`, CORS, уровни логирования, `spring.jpa.open-in-view=false`.

### P1 — Контракты ошибок и мелочи логики

5. **`GlobalExceptionHandler.java:17-47` неполный**: `DuplicateEmail` бросается как `SecurityException` → `401` вместо `409`; нет `AccessDeniedException`/`AuthenticationException` (403/401 JSON), `ConstraintViolationException`, `EntityNotFound` generic; возвращает `Map` вместо `ApiError` DTO; показывает только первую ошибку валидации.
6. **`resetPassword` — заглушка** — `UserServiceImpl.java:142-149` только отзывает refresh-токены. Нет генерации/отправки пароля (зона notification-сервиса — зафиксировать контракт).
7. **`RefreshToken` в `model/`, а не `entity/`** — перенести для консистентности; проверить каскады при `deleteUser` (удалятся ли токены/адреса).
8. **`SecurityConfig`**: нет `AuthenticationEntryPoint`/`AccessDeniedHandler`, нет CORS (`corsCustomizer`), actuator-матчеры не описаны.

### P1 — Тесты и упаковка

9. **Тесты только mock**: `AuthControllerTest.java:44` — `standaloneSetup`, без Spring-контекста. Нет: `UserControllerTest`, `AddressControllerTest`, `SecurityTest` с реальным JWT (401/403/refresh/reuse), `@DataJpaTest`/Testcontainers репозиторных тестов (зависимости уже есть), `@SpringBootTest` happy-path register→login→me→refresh→logout.
10. **Docker**: `compose.yaml` — нет `healthcheck`, `volumes`, сервиса приложения, `.env`. Нет `Dockerfile`/Jib. До полного `tech-store` нужен root `docker-compose.yml` (Postgres x4 + Mongo + RabbitMQ + Eureka + Gateway + 6 сервисов).

### P2 — Наблюдаемость

11. **Actuator/метрики/логи**: в `pom.xml` нет `spring-boot-starter-actuator` (только `actuator-test`), в properties нет `management.endpoints.web.exposure.include=health,info,metrics,prometheus`, нет `health.show-details`, logback-конфига, OpenAPI-группировки, трассировки (`userId` в MDC).

### P3 — Межсервисная интеграция (0%, весь tech-store)

12. **Ничего не начато** (подтверждено grep + `pom.xml`): нет OpenFeign-клиентов (`Order/Inventory/Payment/Catalog`), нет `spring-cloud-starter-netflix-eureka-client`, нет `spring-amqp`/`spring-kafka` (`RabbitMQConfig`, `OrderCreatedEvent` и т.д.), нет Resilience4j, Config Client.
13. **Другие сервисы отсутствуют полностью**: `tech-catalog-service` (8002/Mongo), `tech-inventory-service` (8003), `tech-order-service` (8004), `tech-payment-service` (8005), `tech-review-service` (8006), `tech-api-gateway` (8000), `tech-common-*`, `kubernetes/`. В репозитории есть только `tech-user-service`; `tech-store/` содержит только `README.md`.

---

## 📋 План реализации (обновлённый)

| Приоритет | Задача | Оценка |
|-----------|--------|--------|
| **P0** | `@EnableMethodSecurity` + `@PreAuthorize` на роли/актив/удаление + тесты 403 | 1 день |
| **P0** | Flyway + `V1__init` + `V2__seed_roles` + `ddl-auto=validate` | 1 день |
| **P0** | Env-конфиг (`DB_*`, `JWT_SECRET`), CORS, entry points 401/403 | 1 день |
| **P0** | Фикс `assignRole` (merge вместо `Set.of`) + `RoleService` | 0.5 дня |
| **P1** | `ApiError` DTO + коды (`409` duplicate, `403`, `400` полный список) | 1 день |
| **P1** | `Dockerfile` + `compose.yaml` (healthcheck, volume, app-сервис) | 0.5 дня |
| **P1** | Интеграционные тесты (MockMvc+jwt, Testcontainers для репозиториев) + `User/AddressControllerTest` | 3-5 дней |
| **P2** | `spring-boot-starter-actuator` + `management.*` + Prometheus + logback/MDC | 1 день |
| **P3** | Eureka Client + Feign + RabbitMQ events + Resilience4j | 1-2 недели |
| **P3** | Остальные 5 сервисов + Gateway + common + root compose + K8s | 3-4 недели |

---

## 🎯 Заключение

**`tech-user-service` (~70%)**: ядро (сервисы, JWT-аутентификация, refresh-ротация, CRUD адресов, базовые тесты) работает. До production-минимума остались: ролевой доступ, Flyway-сиды, env/CORS/401-403, `ApiError`-коды, Docker-упаковка — **~1–1.5 недели**. Полная готовность с интеграционными тестами и наблюдаемостью — **~2–3 недели**.

**Весь `tech-store` (~15%)**: реализован только User Service. Без 5 сервисов, Gateway, общих модулей, брокера и discovery система не собирается — **~4–6 недель** после добивки User Service.
