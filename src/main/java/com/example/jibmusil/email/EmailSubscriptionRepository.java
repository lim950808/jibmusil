package com.example.jibmusil.email;

import com.example.jibmusil.user.EmailSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSubscriptionRepository extends JpaRepository<EmailSubscription, Long> {
    
    List<EmailSubscription> findByUserId(Long userId);
    
    Optional<EmailSubscription> findByUserIdAndSubscriptionType(Long userId, EmailSubscription.SubscriptionType subscriptionType);
    
    boolean existsByUserIdAndSubscriptionType(Long userId, EmailSubscription.SubscriptionType subscriptionType);
    
    @Query("SELECT es FROM EmailSubscription es WHERE es.subscriptionType = :subscriptionType AND es.isActive = true")
    List<EmailSubscription> findActiveSubscriptionsByType(@Param("subscriptionType") EmailSubscription.SubscriptionType subscriptionType);
    
    @Query("SELECT es FROM EmailSubscription es WHERE es.isActive = true AND es.frequency = :frequency")
    List<EmailSubscription> findActiveSubscriptionsByFrequency(@Param("frequency") EmailSubscription.Frequency frequency);
    
    @Query("SELECT es FROM EmailSubscription es WHERE es.isActive = true AND " +
           "((es.frequency = 'HOURLY' AND (es.lastSent IS NULL OR es.lastSent < :oneHourAgo)) OR " +
           "(es.frequency = 'DAILY' AND (es.lastSent IS NULL OR es.lastSent < :oneDayAgo)) OR " +
           "(es.frequency = 'WEEKLY' AND (es.lastSent IS NULL OR es.lastSent < :oneWeekAgo)))")
    List<EmailSubscription> findSubscriptionsDueForSending(
            @Param("oneHourAgo") LocalDateTime oneHourAgo,
            @Param("oneDayAgo") LocalDateTime oneDayAgo,
            @Param("oneWeekAgo") LocalDateTime oneWeekAgo);
    
    @Query("SELECT COUNT(es) FROM EmailSubscription es WHERE es.subscriptionType = :subscriptionType AND es.isActive = true")
    Long countActiveSubscriptionsByType(@Param("subscriptionType") EmailSubscription.SubscriptionType subscriptionType);
    
    @Query("SELECT COUNT(es) FROM EmailSubscription es WHERE es.userId = :userId AND es.isActive = true")
    Long countActiveSubscriptionsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT es FROM EmailSubscription es WHERE es.userId = :userId AND es.isActive = true")
    List<EmailSubscription> findActiveSubscriptionsByUserId(@Param("userId") Long userId);
}