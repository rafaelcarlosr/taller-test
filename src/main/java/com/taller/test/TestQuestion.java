package com.taller.test;
import java.math.BigDecimal;

public class TestQuestion {
    static void main() {
        PaymentProcessor paymentProcessor = new PaymentProcessor();

        paymentProcessor.addPayment(new Payment("1", BigDecimal.valueOf(100), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("2", BigDecimal.valueOf(200), "BRL", Payment.Status.PENDING));
        paymentProcessor.addPayment(new Payment("3", BigDecimal.valueOf(300), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("4", BigDecimal.valueOf(400), "USD", Payment.Status.PENDING));
        paymentProcessor.addPayment(new Payment("5", BigDecimal.valueOf(500), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("6", BigDecimal.valueOf(600), "USD", Payment.Status.PENDING));
        paymentProcessor.addPayment(new Payment("7", BigDecimal.valueOf(700), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("8", BigDecimal.valueOf(800), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("9", BigDecimal.valueOf(900), "USD", Payment.Status.SUCCESS));
        paymentProcessor.addPayment(new Payment("10", BigDecimal.valueOf(1000), "USD", Payment.Status.SUCCESS));

    }
}
