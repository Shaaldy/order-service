package by.shaaldy.orderservice.messaging.event;

import java.util.UUID;

public record PaymentProcessedEvent(UUID paymentId, UUID orderId, boolean success) {}
