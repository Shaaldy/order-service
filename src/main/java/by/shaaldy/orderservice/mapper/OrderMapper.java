package by.shaaldy.orderservice.mapper;

import org.mapstruct.Mapper;

import by.shaaldy.orderservice.domain.Order;
import by.shaaldy.orderservice.domain.OrderItem;
import by.shaaldy.orderservice.dto.OrderItemDto;
import by.shaaldy.orderservice.dto.OrderResponse;

@Mapper(componentModel = "spring")
public interface OrderMapper {
  OrderResponse toResponse(Order order);

  OrderItem toEntity(OrderItemDto dto);
}
