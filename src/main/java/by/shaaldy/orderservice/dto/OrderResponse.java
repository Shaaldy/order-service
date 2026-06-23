package by.shaaldy.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import by.shaaldy.orderservice.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

  private UUID id;
  private String customerId;
  private OrderStatus status;
  private BigDecimal totalAmount;
  private List<OrderItemDto> items;
  private Instant createdAt;
}
