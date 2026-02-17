package com.taller.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PaymentProcessor {

    private List<Payment> payments = new ArrayList<>();

    public void addPayment(Payment payment){
        payments.add(payment);
    }

    public List<Payment> retrieveAllPayments() {
        return payments;
    }

    public List<Payment> retrievePaymentsByStatus(Payment.Status status) {
        return payments.stream().filter(payment -> payment.status() == status).toList();
    }

    public Stream<Payment> retrievePaymentsByStatusStream(Payment.Status status) {
        return payments.stream().filter(payment -> payment.status() == status);
    }

    public BigDecimal totalNumberOfPayments() {
        return payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalAmountOfSucessfulPayments() {
        return retrievePaymentsByStatusStream(Payment.Status.SUCCESS).map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averageAmountOfSucessfulPayments() {
        return totalAmountOfSucessfulPayments().divide(BigDecimal.valueOf(payments.size()));
    }
}
