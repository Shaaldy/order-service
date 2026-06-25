package by.shaaldy.orderservice.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, BigDecimal amount) {}
