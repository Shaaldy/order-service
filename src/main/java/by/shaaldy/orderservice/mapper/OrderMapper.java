package by.shaaldy.orderservice.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import by.shaaldy.orderservice.domain.Order;
import by.shaaldy.orderservice.dto.OrderItemDto;
import by.shaaldy.orderservice.dto.OrderResponse;

@Component
public class OrderMapper {

  public OrderResponse toResponse(Order order) {
    List<OrderItemDto> items =
        order.getItems().stream()
            .map(
                item ->
                    OrderItemDto.builder()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
            .toList();

    return OrderResponse.builder()
        .id(order.getId())
        .customerId(order.getCustomerId())
        .status(order.getStatus())
        .totalAmount(order.getTotalAmount())
        .items(items)
        .createdAt(order.getCreatedAt())
        .build();
  }
}
