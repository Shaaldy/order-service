package by.shaaldy.orderservice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import by.shaaldy.orderservice.domain.OrderStatus;
import by.shaaldy.orderservice.dto.OrderResponse;
import by.shaaldy.orderservice.repository.OrderRepository;

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OrderControllerIT {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private RestTestClient restTestClient;

  @Autowired private OrderRepository orderRepository;

  @BeforeEach
  void clean() {
    orderRepository.deleteAll();
  }

  @Test
  void createOrder_return201() {
    var item = Map.of("productName", "pizza", "price", 20000, "quantity", 5);
    var request = Map.of("customerId", "alice", "items", List.of(item));

    restTestClient
        .post()
        .uri("/api/orders")
        .body(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .exists("Location")
        .expectBody(OrderResponse.class)
        .value(
            body -> {
              assertThat(body.getId()).isNotNull();
              assertThat(body.getItems()).hasSize(1);
              assertThat(body.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
              assertThat(body.getStatus()).isEqualTo(OrderStatus.CREATED);
            });
  }

  @Test
  void createThenGet_return200() {
    var item = Map.of("productName", "pizza", "price", 20000, "quantity", 5);
    var request = Map.of("customerId", "alice", "items", List.of(item));

    OrderResponse created =
        restTestClient
            .post()
            .uri("/api/orders")
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(OrderResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(created.getId()).isNotNull();
    restTestClient
        .get()
        .uri("/api/orders/" + created.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(OrderResponse.class)
        .value(
            body -> {
              assertThat(body.getId()).isEqualTo(created.getId());
              assertThat(body.getTotalAmount()).isEqualByComparingTo(created.getTotalAmount());
              assertThat(body.getCreatedAt()).isNotNull();
              assertThat(body.getStatus()).isEqualTo(created.getStatus());
              assertThat(body.getCustomerId()).isEqualTo(created.getCustomerId());
            });
  }

  @Test
  void getMissingOrder_return404() {
    UUID id = UUID.randomUUID();
    restTestClient.get().uri("/api/orders/" + id).exchange().expectStatus().isNotFound();
  }

  @Test
  void createWithEmptyItems_return400() {
    var request = Map.of("customerId", "alice", "items", List.of());
    restTestClient.post().uri("/api/orders").body(request).exchange().expectStatus().isBadRequest();
  }

  @Test
  void cancelTwice_return409() {
    var item = Map.of("productName", "pizza", "price", 20000, "quantity", 5);
    var request = Map.of("customerId", "alice", "items", List.of(item));
    OrderResponse created =
        restTestClient
            .post()
            .uri("/api/orders")
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(OrderResponse.class)
            .returnResult()
            .getResponseBody();

    OrderResponse cancel =
        restTestClient
            .post()
            .uri("/api/orders/" + created.getId() + "/cancel")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(OrderResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(cancel.getId()).isEqualTo(created.getId());
    assertThat(cancel.getStatus()).isEqualTo(OrderStatus.CANCELLED);

    restTestClient
        .post()
        .uri("/api/orders/" + created.getId() + "/cancel")
        .exchange()
        .expectStatus()
        .isEqualTo(409);
  }

  @Test
  void list_return200() {
    createOrder("alice", "pizza", 20000, 1);
    createOrder("bob", "burger", 15000, 2);
    createOrder("carol", "salad", 8000, 3);

    restTestClient
        .get()
        .uri("/api/orders?page=0&size=2")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content")
        .isArray()
        .jsonPath("$.content.length()")
        .isEqualTo(2)
        .jsonPath("$.totalElements")
        .isEqualTo(3)
        .jsonPath("$.totalPages")
        .isEqualTo(2);
  }

  private void createOrder(String customerId, String productName, int price, int quantity) {
    var item = Map.of("productName", productName, "price", price, "quantity", quantity);
    var request = Map.of("customerId", customerId, "items", List.of(item));
    restTestClient.post().uri("/api/orders").body(request).exchange().expectStatus().isCreated();
  }
}
