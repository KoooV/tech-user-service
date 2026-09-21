# Tech User Service — Проверка готовности к развёртыванию

> **Дата**: 21 сентября 2026  
> **Статус**: ⚠️ **НЕ ГОТОВ** к production-развёртыванию  
> **Архитектурная спецификация**: tech-store README.md  
> **Текущая реализация**: Java 17, Spring Boot 4.1.1, PostgreSQL, MapStruct

---

## 📊 Общая оценка готовности

| Компонент | Статус | Критичность |
|-----------|--------|-------------|
| Entity-модель | ✅ Готово | — |
| Repository-слоя | ✅ Готово | — |
| DTO и Mapper | ✅ Готово (частично) | Низкая |
| Контроллеры | ⚠️ Скелет готов | Средняя |
| **Service Layer** | ❌ **Отсутствует** | **Критическая** |
| **Security / JWT** | ❌ **Полностью нереализовано** | **Критическая** |
| **Application Config** | ❌ **Пустой** | **Критическая** |
| Обработка ошибок | ❌ Отсутствует | Высокая |
| Тесты | ❌ Базовые только | Высокая |
| Docker / Compose | ⚠️ Минимально | Средняя |
| Интеграция с другими сервисами | ❌ Не начата | Высокая |
| Актюаторы / Мониторинг | ❌ Не настроены | Средняя |
| Миграции БД | ❌ Отсутствуют | Высокая |

---

## ❌ Что НЕ готово (отсутствует до полной реализации)

### 1. 🔐 Service Layer (КРИТИЧЕСКАЯ)

**Проблема**: Полное отсутствие слоя сервисов. Вся бизнес-логика дублирована в контроллерах.

**Что нужно создать**:
- `UserService` — регистрация, получение профилей, обновление, управление ролями
- `AuthService` — вход, refresh токен, logout, валидация
- `AddressService` — CRUD адресов, управление дефолтным адресом
- `RoleService` — управление ролями и правами
- `PermissionService` — управление пермишенами

**Влияние**: Без service layer невозможно:
- Тестировать бизнес-логику отдельно от контроллеров
- Реализовать транзакционность (`@Transactional`)
- Поддерживать DDD и разделение ответственности
- Добавлять кэширование, retry, circuit breaker

---

### 2. 🔐 Security Configuration (КРИТИЧЕСКАЯ)

**Проблема**: Spring Security зависим в `pom.xml`, но **ни одного класса конфигурации нет**.

**Что нужно создать**:
- `SecurityConfig` — `SecurityFilterChain` bean, настройка авторизации
- `JwtAuthenticationFilter` — фильтр для извлечения и валидации JWT
- `JwtUtil` — генерация/валидация токенов
- `CustomUserDetailsService` — загрузка пользователя из БД
- `PasswordEncoder` bean — BCrypt для хеширования паролей
- `AuthenticationManager` конфигурация

**Текущее состояние**: `AuthController.login()` возвращает **хардкодированные строки** `"access-token"`, `"refresh-token"` вместо реальных JWT.

**Влияние**: Аутентификация **полностью неработоспособна**. Никакой защищённой авторизации не существует.

---

### 3. 📝 Application Configuration (КРИТИЧЕСКАЯ)

**Проблема**: `application.properties` содержит только `spring.application.name=tech-user-service`.

**Что нужно добавить**:
```properties
# DataSource
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=
spring.jpa.show-sql=

# JWT
jwt.secret=
jwt.expiration=
jwt.refresh-expiration=

# Server
server.port=8001

# Actuator
management.endpoints.web.exposure.include=
management.endpoint.health.show-details=

# OpenAPI
springdoc.api-docs.path=
springdoc.swagger-ui.path=

# CORS
```

**Влияние**: Приложение **не может подключиться к базе данных**, не знает свой порт, не имеет JWT-настроек.

---

### 4. 🚨 Global Exception Handler (ВЫСОКАЯ)

**Проблема**: Нет `@ControllerAdvice` или любого обработчика ошибок.

**Что нужно создать**:
- `GlobalExceptionHandler` — перехват `EntityNotFoundException`, `ConstraintViolationException`, `MethodArgumentNotValidException` и т.д.
- `ApiError` DTO — структура ответов об ошибках
- Кастомные исключения: `UserNotFoundException`, `DuplicateEmailException`, `InvalidCredentialsException`, `AuthorizationException`

**Влияние**: Ошибки возвращаются в неструктурированном виде, нет единого формата ответов.

---

### 5. 🧪 Тесты (ВЫСОКАЯ)

**Проблема**: Только 3 файла, проверяющих загрузку Spring-контекста. Нет реальных тестов.

**Что нужно создать**:
- `UserServiceTest` — юнит-тесты сервисов
- `AuthControllerTest` — интеграционные тесты эндпоинтов
- `UserControllerTest` — тесты CRUD
- `AddressControllerTest` — тесты адресов
- `SecurityTest` — тесты авторизации
- `RepositoryTest` — тесты репозиториев (с Testcontainers)
- `AuthServiceTest` — тесты аутентификации

**Влияние**: Невозможно убедиться в корректности работы, нет regression coverage.

---

### 6. 🐳 Docker Compose (СРЕДНЯЯ)

**Проблема**: `compose.yaml` содержит только базовый PostgreSQL без переменных окружения и зависимостей.

**Что нужно добавить**:
- Переменные окружения для подключения Spring Boot к БД
- Health checks для PostgreSQL
- Network configuration
- Зависимости для запуска всех 6 сервисов (по README: Eureka, RabbitMQ, MongoDB, API Gateway)
- Volumes для persistency данных
- Определение всех сервисов (tech-user-service, tech-catalog-service и т.д.)

**Влияние**: Нельзя поднять полную систему из одного `docker-compose up -d`.

---

### 7. 🔄 Интеграция с другими сервисами (ВЫСОКАЯ)

**Проблема**: Не реализовано ни одного межсервисного взаимодействия.

**Что нужно создать**:
- **OpenFeign клиенты** для вызова других сервисов:
  - `CatalogClient` — получение информации о товарах
  - `OrderClient` — создание/получение заказов
  - `PaymentClient` — создание платежей
  - `InventoryClient` — резервирование остатков
- **RabbitMQ конфигурация**:
  - `RabbitMQConfig` — exchange, queue, binding declarations
  - `OrderCreatedEventListener` — прослушивание событий заказов
- **Eureka Client** — регистрация сервиса в Service Discovery

**Влияние**: Сервис полностью изолирован и не участвует в микросервисной архитектуре.

---

### 8. 📦 Database Migrations (ВЫСОКАЯ)

**Проблема**: Нет Flyway или Liquibase. Схема БД создаётся автоматически (или вообще не создаётся).

**Что нужно создать**:
- Flyway/Liquibase скрипты для инициализации схемы
- Seed data для ролей и пермишенов (USER, MANAGER, ADMIN)
- Миграции для production-окружений

**Влияние**: Нет способа контролировать версии схемы БД, невозможно разворачивать в production.

---

### 9. 📊 Actuator и Мониторинг (СРЕДНЯЯ)

**Проблема**: Зависимости присутствуют в `pom.xml`, но ни одного настроенного эндпоинта.

**Что нужно добавить**:
- `management.endpoints.web.exposure.include=health,info,metrics`
- Health indicators для БД и RabbitMQ
- Prometheus метрики
- Логирование (SLF4J + Logback конфигурация)

---

### 10. ✅ DTO — частичная готовность

**Что готово**: Большинство DTO созданы (`UserRequestDTO`, `UserResponseDTO`, `AuthRequestDTO`, `AuthResponseDTO`, `AddressDTO`, `RoleDTO`, `RoleUpdateDTO`, `UpdateUserRequestDTO`, `AuthRefreshRequestDTO`, `PageResponseDTO`)

**Что не готово**:
- `AuthRefreshRequestDTO` существует, но нет логики обработки refresh-токена
- Нет `PhoneDTO` (хотя телефон упомянут в `UserRequestDTO`)
- Нет `PermissionDTO` для полноценной работы с permissions

---

### 11. 🏗️ Контроллеры — скелет (СРЕДНЯЯ)

Контроллеры написаны, но содержат баги и неполноценную логику:

**Проблемы в `UserController`**:
- `updateUser()` вручную заполняет поля вместо использования `UserMapper.updateFromDto()`
- `assignRole()` заменяет все роли `Set.of(role)` (теряются существующие)
- `getAllUsers()` вручную конструирует `PageResponseDTO` вместо маппера
- Нет `@Transactional` на write-операциях

**Проблемы в `AuthController`**:
- `login()` не аутентифицирует через `AuthenticationManager`
- `resetPassword()` — пустой stub, возвращает OK без логики
- Нет реальной генерации JWT

---

## ✅ Что уже готово

| Компонент | Описание |
|-----------|----------|
| Entity-модель | `User`, `Address`, `Role`, `Permission` — корректные JPA-сущности |
| Repository | Все 4 JpaRepository с кастомными query-методами |
| DTO | 10 DTO-классов с Jakarta Validation аннотациями |
| MapStruct Mapper | 4 маппер-интерфейса с сгенерированными реализациями |
| Контроллеры | 3 контроллера с эндпоинтами (скелет) |
| `pom.xml` | Все зависимости описаны (Spring Security, Data JPA, OpenAPI, Actuator, и т.д.) |
| `compose.yaml` | Минимальная PostgreSQL конфигурация |
| Структура проекта | Корректная Maven-структура с пакетами |

---

## 📋 План реализации (приоритеты)

| Приоритет | Задача | Оценка |
|-----------|--------|--------|
| **P0** | Создать `application.properties` с конфигурацией БД, JWT, портов | 1 день |
| **P0** | Реализовать `SecurityConfig` + `JwtUtil` + `JwtAuthenticationFilter` + `CustomUserDetailsService` | 2-3 дня |
| **P0** | Создать `PasswordEncoder` bean | 1 час |
| **P0** | Создать `AuthService` с реальной аутентификацией и генерацией JWT | 2 дня |
| **P0** | Создать `UserService`, `AddressService`, `RoleService` | 2-3 дня |
| **P1** | `GlobalExceptionHandler` + кастомные исключения | 1 день |
| **P1** | Фикс багов в контроллерах (mapper usage, транзакции) | 1 день |
| **P1** | Flyway миграции + seed data | 1 день |
| **P2** | `GlobalExceptionHandler` + `ApiError` DTO | — |
| **P2** | Тесты (юнит + интеграционные) | 3-5 дней |
| **P2** | `compose.yaml` — полная конфигурация всех сервисов | 2 дня |
| **P3** | OpenFeign клиенты для межсервисного взаимодействия | 3-5 дней |
| **P3** | RabbitMQ конфигурация и event listeners | 3-5 дней |
| **P3** | Eureka Client регистрация | 1 день |
| **P3** | Actuator, метрики, логирование | 1 день |

---

## 🎯 Заключение

**Текущий tech-user-service** — это скелет с сущностями и репозиториями. **Ни один эндпоинт не работает корректно** без реализации Service Layer и Security Configuration. Без JWT-аутентификации весь функционал регистрации/авторизации неработоспособен. Без конфигурации БД приложение вообще не запустится.

**Минимальный набор для запуска**: Service Layer + Security Config + Application Properties + PasswordEncoder = **~7-10 дней** разработки.

**Полная реализация** (включая интеграцию, тесты, мониторинг): **~3-4 недели**.
