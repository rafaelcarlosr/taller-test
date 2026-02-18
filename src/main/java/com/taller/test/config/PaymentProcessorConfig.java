package com.taller.test.config;

import com.taller.test.processor.PaymentProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for PaymentProcessor.
 * Makes PaymentProcessor available as a singleton Spring bean.
 */
@Configuration
public class PaymentProcessorConfig {

    @Bean
    public PaymentProcessor paymentProcessor() {
        return new PaymentProcessor();
    }
}
