package by.shaaldy.orderservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDto {
  @NotBlank private String productName;

  @NotBlank @Positive private Integer quantity;

  @NotNull @Positive private BigDecimal price;
}
