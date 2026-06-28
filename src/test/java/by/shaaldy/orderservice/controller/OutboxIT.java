package by.shaaldy.orderservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import by.shaaldy.orderservice.dto.CreateOrderRequest;
import by.shaaldy.orderservice.dto.OrderItemDto;
import by.shaaldy.orderservice.dto.OrderResponse;
import by.shaaldy.orderservice.repository.OrderRepository;
import by.shaaldy.orderservice.repository.OutboxRepository;
import by.shaaldy.orderservice.service.OrderService;

public class OutboxIT extends AbstractIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OutboxRepository outboxRepository;

  @BeforeEach
  void clean() {
    orderRepository.deleteAll();
    outboxRepository.deleteAll();
  }

  private Consumer<String, String> createConsumer() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID()); // уникальная группа
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // читать с начала топика
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    Consumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(List.of("order.created"));
    return consumer;
  }

  @Test
  void outbox_publishesEventAndClearsOutbox() {
    // 1. Arrange — собрать запрос
    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .customerId("testCustomer")
            .items(
                List.of(
                    OrderItemDto.builder()
                        .productName("p1")
                        .price(BigDecimal.valueOf(20000))
                        .quantity(10)
                        .build()))
            .build();

    // 2. Act — создать заказ (запись в БД + outbox в транзакции)
    OrderResponse response = orderService.create(request);
    UUID orderId = response.getId(); // понадобится для фильтрации события

    // 3. Assert — две проверки:

    //   (а) outbox опустел (почтальон унёс) — АСИНХРОННО → Awaitility
    await().atMost(Duration.ofSeconds(15)).until(() -> outboxRepository.count() == 0);

    //   (б) событие реально в топике — прочитать консьюмером
    try (Consumer<String, String> consumer = createConsumer()) {
      ConsumerRecords<String, String> records =
          KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

      // найти ИМЕННО наше событие по orderId (фильтр, не getSingleRecord)
      String payload =
          StreamSupport.stream(records.spliterator(), false)
              .map(ConsumerRecord::value)
              .filter(v -> v.contains(orderId.toString()))
              .findFirst()
              .orElseThrow();

      assertThat(payload).contains(orderId.toString());
    }
  }
}
