package by.shaaldy.orderservice.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import by.shaaldy.orderservice.domain.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {}
