package by.shaaldy.orderservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.shaaldy.orderservice.domain.OutboxMessage;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

  List<OutboxMessage> findAllByOrderByCreatedAtAsc();
}
