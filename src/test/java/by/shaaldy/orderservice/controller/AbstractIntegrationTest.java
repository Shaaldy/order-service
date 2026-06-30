package by.shaaldy.orderservice.controller;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

  static final KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

  static {
    kafka.start();
    postgres.start();
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("outbox.poll-interval", () -> 100);
  }
}
