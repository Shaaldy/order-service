package by.shaaldy.orderservice.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import by.shaaldy.orderservice.domain.Order;
import by.shaaldy.orderservice.domain.OrderStatus;
import by.shaaldy.orderservice.domain.OutboxMessage;
import by.shaaldy.orderservice.dto.CreateOrderRequest;
import by.shaaldy.orderservice.dto.OrderItemDto;
import by.shaaldy.orderservice.exception.OrderNotFoundException;
import by.shaaldy.orderservice.mapper.OrderMapper;
import by.shaaldy.orderservice.repository.OrderRepository;
import by.shaaldy.orderservice.repository.OutboxRepository;
import jakarta.validation.Valid;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private OrderMapper orderMapper;
  @Mock private OutboxRepository outboxRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private OrderService orderService;

  static Stream<Arguments> orderItemsProvide() {
    return Stream.of(
        Arguments.of(
            List.of(itemDto("p1", BigDecimal.valueOf(20000), 5)), BigDecimal.valueOf(100000)),
        Arguments.of(
            List.of(
                itemDto("p1", BigDecimal.valueOf(40000), 5),
                itemDto("p2", BigDecimal.valueOf(20000), 1)),
            BigDecimal.valueOf(220000)),
        Arguments.of(
            List.of(
                itemDto("p1", BigDecimal.valueOf(0.3), 30000),
                itemDto("p2", BigDecimal.valueOf(10000), 2),
                itemDto("p3", BigDecimal.valueOf(1), 300),
                itemDto("p4", BigDecimal.valueOf(220), 51)),
            BigDecimal.valueOf(40520)));
  }

  static OrderItemDto itemDto(String name, BigDecimal price, int qty) {
    return OrderItemDto.builder().productName(name).price(price).quantity(qty).build();
  }

  @ParameterizedTest
  @MethodSource("orderItemsProvide")
  void create_withMultipleItems_calculatesTotalAmount(
      List<@Valid OrderItemDto> items, BigDecimal totalAmount) {
    CreateOrderRequest cro =
        CreateOrderRequest.builder().customerId("testCustomer").items(items).build();
    when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
    orderService.create(cro);
    verify(orderRepository).saveAndFlush(captor.capture());
    verify(outboxRepository).save(any(OutboxMessage.class));

    Order saved = captor.getValue();

    assertThat(saved.getTotalAmount()).isEqualByComparingTo(totalAmount);
    assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
  }

  @Test
  void create_writesOutboxMessage() {
    CreateOrderRequest request =
        CreateOrderRequest.builder()
            .customerId("testCustomer")
            .items(List.of(itemDto("p1", BigDecimal.valueOf(100), 2)))
            .build();
    when(orderRepository.saveAndFlush(any(Order.class)))
        .thenAnswer(
            inv -> {
              Order o = inv.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);

    orderService.create(request);

    verify(orderRepository).saveAndFlush(orderCaptor.capture());
    verify(outboxRepository).save(outboxCaptor.capture());

    UUID orderId = orderCaptor.getValue().getId();
    OutboxMessage outbox = outboxCaptor.getValue();

    assertThat(outbox.getTopic()).isEqualTo("order.created");
    assertThat(outbox.getPayload()).contains(orderId.toString());
  }

  @Test
  void create_withEmptyItems_throwIllegalArgument() {

    List<@Valid OrderItemDto> orderItemDtos = new ArrayList<>();
    CreateOrderRequest cro =
        CreateOrderRequest.builder().customerId("testCustomer").items(orderItemDtos).build();
    assertThatThrownBy(() -> orderService.create(cro))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Order must contain at least one item");
    verify(orderRepository, never()).saveAndFlush(any(Order.class));
    verify(orderMapper, never()).toResponse(any());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrderStatus.class,
      names = {"PAID"})
  void cancel_fromCancellableStatus_setsCancelled(OrderStatus status) {
    UUID orderId = UUID.randomUUID();
    Order order = Order.builder().id(orderId).status(status).build();
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    orderService.cancel(order.getId());
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLING);
  }

  @ParameterizedTest
  @EnumSource(
      value = OrderStatus.class,
      names = {"PAYMENT_FAILED"})
  void cancel_fromNonCancellableStatus_throwIllegalState(OrderStatus status) {
    UUID orderId = UUID.randomUUID();

    Order order = Order.builder().id(orderId).status(status).build();
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    assertThatThrownBy(() -> orderService.cancel(order.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot cancel order in status " + status);
    verify(orderMapper, never()).toResponse(any());
  }

  @Test
  void cancel_whenOrderNotFound_throwOrderNotFound() {
    UUID orderId = UUID.randomUUID();
    Order order = Order.builder().id(orderId).status(OrderStatus.CREATED).build();
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.cancel(order.getId()))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found: " + orderId);
    verify(orderMapper, never()).toResponse(any());
  }

  @Test
  void find_whenOrderNotFound_throwOrderNotFound() {
    UUID orderId = UUID.randomUUID();
    when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> orderService.findById(orderId))
        .isInstanceOf(OrderNotFoundException.class)
        .hasMessage("Order not found: " + orderId);
    verify(orderMapper, never()).toResponse(any());
  }
}
