package by.shaaldy.orderservice.messaging.event.refund;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId) {}
