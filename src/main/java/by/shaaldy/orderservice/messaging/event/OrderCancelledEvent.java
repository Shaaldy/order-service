package by.shaaldy.orderservice.messaging.event;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId) {
}
