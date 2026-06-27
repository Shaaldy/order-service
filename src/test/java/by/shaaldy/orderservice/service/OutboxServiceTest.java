package by.shaaldy.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import by.shaaldy.orderservice.domain.OutboxMessage;
import by.shaaldy.orderservice.repository.OutboxRepository;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {
  @Mock private OutboxRepository outboxRepository;
  @Mock private KafkaTemplate<String, String> kafkaTemplate;

  @InjectMocks private OutboxService outboxService;

  @SuppressWarnings("unchecked")
  @Test
  void publish_sendsMessagesAndDeletesThem() {
    OutboxMessage message =
        OutboxMessage.builder()
            .id(UUID.randomUUID())
            .topic("order.created")
            .payload("orderId: 209138189jiuj12, totalAmount: 100000")
            .build();
    when(outboxRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(message));
    when(kafkaTemplate.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);

    outboxService.publish();

    verify(kafkaTemplate).send(message.getTopic(), message.getPayload());
    verify(outboxRepository).deleteAllById(captor.capture());
    assertThat(captor.getValue()).containsExactly(message.getId());
  }

  @Test
  void publish_whenKafkaFails_doesNotDelete() {
    OutboxMessage message =
        OutboxMessage.builder()
            .id(UUID.randomUUID())
            .topic("order.created")
            .payload("orderId: 209138189jiuj12, totalAmount: 100000")
            .build();
    when(outboxRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(message));
    when(kafkaTemplate.send(any(), any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

    outboxService.publish();

    verify(kafkaTemplate).send(message.getTopic(), message.getPayload());
    verify(outboxRepository, never()).deleteAllById(any());
  }
}
