package by.shaaldy.orderservice.domain;

public enum OrderStatus {
  CREATED,
  PAID,
  CONFIRMED,
  CANCELLED,
  FAILED;

  public boolean isCancellable() {
    return this == CREATED || this == PAID;
  }
}
