package com.taller.test;

import java.math.BigDecimal;
import java.util.Objects;

public record Payment(String id, BigDecimal amount, String currency, Status status) {
    public enum Status {PENDING, SUCCESS, FAILED};

}
//public class Payment {
//    private String id;
//    private BigDecimal amount;
//    private String currency;
//    private Status status;
//
//    public enum Status {PENDING, SUCCESS, FAILED};
//
//    public Status getStatus() {
//        return status;
//    }
//
//    public void setStatus(Status status) {
//        this.status = status;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getCurrency() {
//        return currency;
//    }
//
//    public void setCurrency(String currency) {
//        this.currency = currency;
//    }
//
//    public BigDecimal getAmount() {
//        return amount;
//    }
//
//    public void setAmount(BigDecimal amount) {
//        this.amount = amount;
//    }
//
//    @Override
//    public String toString() {
//        return "Payment{" +
//                "status=" + status +
//                ", currency='" + currency + '\'' +
//                ", amount=" + amount +
//                ", id='" + id + '\'' +
//                '}';
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) return false;
//        Payment payment = (Payment) o;
//        return Objects.equals(id, payment.id) && Objects.equals(amount, payment.amount) && Objects.equals(currency, payment.currency) && status == payment.status;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, amount, currency, status);
//    }
//}