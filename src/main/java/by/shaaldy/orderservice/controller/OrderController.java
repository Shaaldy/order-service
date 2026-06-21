package by.shaaldy.orderservice.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import by.shaaldy.orderservice.dto.CreateOrderRequest;
import by.shaaldy.orderservice.dto.OrderResponse;
import by.shaaldy.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderResponse> create(
      @Valid @RequestBody CreateOrderRequest request, UriComponentsBuilder uriBuilder) {

    OrderResponse created = orderService.create(request);

    URI location = uriBuilder.path("/api/orders/{id}").buildAndExpand(created.getId()).toUri();

    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/{id}")
  public OrderResponse getById(@PathVariable UUID id) {
    return orderService.getById(id);
  }

  @GetMapping
  public Page<OrderResponse> list(Pageable pageable) {
    return orderService.list(pageable);
  }

  @PostMapping("/{id}/cancel")
  public OrderResponse cancel(@PathVariable UUID id) {
    return orderService.cancel(id);
  }
}
