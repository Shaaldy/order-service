package by.shaaldy.orderservice.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import by.shaaldy.orderservice.messaging.event.PaymentProcessedEvent;
import by.shaaldy.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {
  private final OrderService orderService;

  @KafkaListener(topics = "payment.processed", groupId = "order-service")
  public void onPaymentProcessed(PaymentProcessedEvent event) {
    log.info("Received payment.processed for order {}", event.orderId());
    orderService.updatePaymentStatus(event.orderId(), event.success());
  }
}
