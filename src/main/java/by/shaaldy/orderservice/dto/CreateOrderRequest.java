package by.shaaldy.orderservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderRequest {
  @NotBlank private String customerId;

  @NotEmpty @Valid private List<OrderItemDto> items;
}
