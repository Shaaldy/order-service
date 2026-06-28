package by.shaaldy.orderservice.messaging.event.payment;

import java.util.UUID;

public record PaymentProcessedEvent(UUID paymentId, UUID orderId, boolean success) {}
