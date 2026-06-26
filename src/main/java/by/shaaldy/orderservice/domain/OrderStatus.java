package by.shaaldy.orderservice.domain;

public enum OrderStatus {
  CREATED,
  PAID,
  CONFIRMED,
  CANCELLED,
  PAYMENT_FAILED;

  public boolean isCancellable() {
    return this == CREATED || this == PAID;
  }
}
