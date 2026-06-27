package by.shaaldy.orderservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.shaaldy.orderservice.domain.Order;
import by.shaaldy.orderservice.domain.OrderItem;
import by.shaaldy.orderservice.domain.OrderStatus;
import by.shaaldy.orderservice.domain.OutboxMessage;
import by.shaaldy.orderservice.dto.CreateOrderRequest;
import by.shaaldy.orderservice.dto.OrderResponse;
import by.shaaldy.orderservice.exception.OrderNotFoundException;
import by.shaaldy.orderservice.mapper.OrderMapper;
import by.shaaldy.orderservice.messaging.event.OrderCreatedEvent;
import by.shaaldy.orderservice.repository.OrderRepository;
import by.shaaldy.orderservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
  private final OrderRepository repository;
  private final OrderMapper mapper;
  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    if (request.getItems() == null || request.getItems().isEmpty()) {
      throw new IllegalArgumentException("Order must contain at least one item");
    }
    Order order = buildOrder(request);
    order.setTotalAmount(calculateTotal(order));
    Order saved = repository.saveAndFlush(order);
    log.info(
        "Created order {} for customer {}, total {}",
        saved.getId(),
        saved.getCustomerId(),
        saved.getTotalAmount());

    publishToOutbox(saved.getId(), saved.getTotalAmount());
    return mapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public OrderResponse findById(UUID id) {
    Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    return mapper.toResponse(order);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> list(Pageable pageable) {
    return repository.findAll(pageable).map(mapper::toResponse);
  }

  @Transactional
  public OrderResponse cancel(UUID id) {
    Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    if (!order.getStatus().isCancellable()) {
      throw new IllegalStateException("Cannot cancel order in status " + order.getStatus());
    }

    order.setStatus(OrderStatus.CANCELLED);
    Order save = repository.save(order);
    return mapper.toResponse(save);
  }

  @Transactional
  public void updatePaymentStatus(UUID orderId, boolean success) {
    Order order =
        repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(success ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
    repository.save(order);
    log.info("Updated order {} status to {}", orderId, order.getStatus());
  }

  private Order buildOrder(CreateOrderRequest req) {
    Order order =
        Order.builder()
            .customerId(req.getCustomerId())
            .status(OrderStatus.CREATED)
            .totalAmount(BigDecimal.ZERO)
            .build();

    req.getItems()
        .forEach(
            itemDto -> {
              OrderItem item =
                  OrderItem.builder()
                      .productName(itemDto.getProductName())
                      .price(itemDto.getPrice())
                      .quantity(itemDto.getQuantity())
                      .build();
              order.addItem(item);
            });

    return order;
  }

  private BigDecimal calculateTotal(Order order) {
    return order.getItems().stream()
        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void publishToOutbox(UUID id, BigDecimal total) {
    OrderCreatedEvent event = new OrderCreatedEvent(id, total);
    String toPayload = objectMapper.writeValueAsString(event);
    OutboxMessage message =
        OutboxMessage.builder().topic("order.created").payload(toPayload).build();
    outboxRepository.save(message);
  }
}
