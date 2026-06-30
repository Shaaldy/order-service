# order-service

REST-сервис приёма и управления заказами. Входная точка системы: принимает заказы по HTTP, транзакционно публикует события через Outbox и обновляет статус заказа по результатам оплаты и возврата.

Часть проекта **food-order** (event-driven микросервисы на Spring Boot 4.1 / Java 21). Системная картина, архитектурная диаграмма и инструкция по запуску всего стека — в корневом репозитории: **[food-order-infra](https://github.com/Shaaldy/food-order-infra)**.

---

## Роль сервиса

- Принимает заказы по REST (`/api/orders`), валидирует, сохраняет со статусом `CREATED`.
- В **той же транзакции** пишет `OrderCreatedEvent` в таблицу `outbox` — фоновый relay затем публикует его в Kafka (`order.created`).
- Слушает `payment.processed` → переводит заказ в `PAID` / `PAYMENT_FAILED`.
- Управляет отменой через state machine; на отмену оплаченного заказа публикует `OrderCancelledEvent` (`order.cancelled`), запуская сагу возврата.
- Слушает `payment.refunded` → закрывает заказ в `CANCELLED`.

Прямых вызовов Payment Service нет — только события.

---

## Внутреннее устройство

```
by.shaaldy.orderservice
├── domain        Order, OrderItem, OrderStatus
├── repository    OrderRepository, OutboxRepository
├── service       OrderService (создание, отмена через state machine,
│                  обновление статуса по событиям оплаты/возврата)
│                  OutboxService (@Scheduled relay: outbox → Kafka)
├── web           OrderController (REST), DTO, обработка ошибок
└── messaging     OrderEventPublisher (запись в outbox)
    │             PaymentEventListener (consume payment.processed / payment.refunded)
    └── event     OrderCreatedEvent, OrderCancelledEvent,
                  PaymentProcessedEvent, PaymentRefundedEvent
```

**Ключевые моменты этого сервиса:**

- **Transactional Outbox.** Запись заказа и запись события в `outbox` — в одной транзакции БД. Relay (`@Scheduled(fixedDelay)`) вычитывает outbox и публикует в Kafka, гарантируя at-least-once. Это закрывает dual-write: заказ не может оказаться сохранённым без события (и наоборот).
- **Отмена как state machine.** `OrderService.cancel()` решает на пять веток, что делать в зависимости от текущего статуса: отмена до оплаты просто закрывает заказ, отмена `PAID` — публикует событие и ждёт компенсирующий возврат.
- **String-сериализация.** Событие публикуется как JSON-строка (`StringSerializer`); listener сам делает `objectMapper.readValue(...)` на нужный тип под конкретный топик (Jackson 3). Почему так, а не `JsonSerializer` — см. корневой README (эволюция решения).
- **`saveAndFlush` для timestamp.** При создании используется `saveAndFlush`, чтобы `@CreationTimestamp` успел проставиться до маппинга в ответ; в DTO мапится сохранённая сущность, а не входная.

---

## API

Базовый путь: `/api/orders`

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/api/orders` | Создать заказ → `201 Created` + `Location` + тело (статус `CREATED`) |
| `GET` | `/api/orders/{id}` | Получить заказ по id |
| `GET` | `/api/orders` | Список заказов (постранично) |
| `POST` | `/api/orders/{id}/cancel` | Отменить заказ (запускает сагу возврата, если был `PAID`) |

Статусы: `CREATED → PAID | PAYMENT_FAILED → CANCELLED`.

---

## Тестирование

- **Юниты** (`OrderServiceTest`): все ветки создания и отмены, проверка через `ArgumentCaptor`, `BigDecimal` через `isEqualByComparingTo`, идемпотентность через `verify(..., never())`.
- **Интеграционные** (`*IT`, Testcontainers): PostgreSQL + Kafka в контейнерах, проверка REST-границы (валидация, коды ответов) и асинхронного пути end-to-end. Async-проверки — через `Awaitility` (временно́е окно, не снимок).

```bash
./mvnw verify    # юниты (Surefire) + интеграционные (Failsafe)
```

CI: GitHub Actions гоняет `./mvnw verify` на каждый push в `main` и pull request.

---

## Стек

Java 21 · Spring Boot 4.1 · Spring for Apache Kafka · JPA/Hibernate · PostgreSQL · Flyway · Micrometer (Actuator/Prometheus, трейсинг через OpenTelemetry) · Maven · Lombok · Testcontainers · Awaitility · Spotless.

Запуск сервиса отдельно требует поднятой инфраструктуры (Kafka + PostgreSQL). Проще запускать весь стек через [food-order-infra](https://github.com/Shaaldy/food-order-infra).
