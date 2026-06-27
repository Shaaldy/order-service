package by.shaaldy.orderservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import by.shaaldy.orderservice.domain.OutboxMessage;
import by.shaaldy.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
  private final OutboxRepository outboxRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Scheduled(fixedDelay = 5000)
  public void publish() {
    List<OutboxMessage> messages = outboxRepository.findAllByOrderByCreatedAtAsc();
    List<UUID> sentIds = new ArrayList<>();
    for (OutboxMessage m : messages) {
      try {
        kafkaTemplate.send(m.getTopic(), m.getPayload()).get();
        sentIds.add(m.getId());
      } catch (Exception e) {
        log.error("Failed to publish outbox message {}, will retry", m.getId(), e);
      }
    }
    if (!sentIds.isEmpty()) {
      outboxRepository.deleteAllById(sentIds);
    }
  }
}
