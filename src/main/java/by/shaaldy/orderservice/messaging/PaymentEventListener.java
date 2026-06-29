package by.shaaldy.orderservice.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import by.shaaldy.orderservice.messaging.event.payment.PaymentProcessedEvent;
import by.shaaldy.orderservice.messaging.event.refund.RefundProcessedEvent;
import by.shaaldy.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {
  private final OrderService orderService;
  private final ObjectMapper objectMapper;
  @KafkaListener(topics = "payment.processed", groupId = "order-service")
  public void onPaymentProcessed(String payLoad) {
      PaymentProcessedEvent event = objectMapper.readValue(payLoad, PaymentProcessedEvent.class);
      log.info("Received payment.processed for order {}", event.orderId());
    orderService.updatePaymentStatus(event.orderId(), event.success());
  }

  @KafkaListener(topics = "payment.refunded", groupId = "order-service")
  public void onPaymentCancel(String payLoad) {
    RefundProcessedEvent event = objectMapper.readValue(payLoad, RefundProcessedEvent.class);
    log.info("Order {} cancelled (refund confirmed)", event.orderId());
    orderService.updateCancel(event.orderId());
  }
}
