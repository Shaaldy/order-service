package by.shaaldy.orderservice.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import by.shaaldy.orderservice.messaging.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {
  private static final String TOPIC = "order.created";
  private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

  public void publish(OrderCreatedEvent event) {
    kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    log.info("Published order.created event for order {}", event.orderId());
  }
}
