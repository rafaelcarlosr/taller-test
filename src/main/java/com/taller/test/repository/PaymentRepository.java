package com.taller.test.repository;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Spring Data JPA repository for Payment entities.
 * Provides CRUD operations and custom queries with Redis caching.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Find all payments by status.
     * Cached in Redis for performance.
     */
    @Cacheable(value = "paymentsByStatus", key = "#status")
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find all payments sorted by amount in descending order.
     * Cached in Redis.
     */
    @Cacheable(value = "paymentsSorted")
    List<Payment> findAllByOrderByAmountDesc();

    /**
     * Count payments by status.
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(PaymentStatus status);

    /**
     * Get total amount by status.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(PaymentStatus status);

    /**
     * Get statistics grouped by currency.
     */
    @Query("SELECT p.currency as currency, COUNT(p) as count, SUM(p.amount) as total " +
           "FROM Payment p GROUP BY p.currency")
    List<Map<String, Object>> getStatisticsByCurrency();

    /**
     * Delete all payments and clear cache.
     */
    @Override
    @CacheEvict(value = {"paymentsByStatus", "paymentsSorted", "paymentStatistics"}, allEntries = true)
    void deleteAll();

    /**
     * Save payment and clear relevant caches.
     */
    @Override
    @CacheEvict(value = {"paymentsByStatus", "paymentsSorted", "paymentStatistics"}, allEntries = true)
    <S extends Payment> S save(S entity);
}
