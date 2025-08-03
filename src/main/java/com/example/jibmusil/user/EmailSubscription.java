package com.example.jibmusil.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_subscriptions",
       uniqueConstraints = @UniqueConstraint(name = "unique_user_subscription", columnNames = {"userId", "subscriptionType"}))
public class EmailSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private SubscriptionType subscriptionType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;
    
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_sent")
    private LocalDateTime lastSent;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    public enum SubscriptionType {
        DAILY_DIGEST,      // 일일 뉴스 요약
        WEEKLY_SUMMARY,    // 주간 뉴스 요약
        BREAKING_NEWS,     // 속보
        TRENDING_TOPICS    // 트렌딩 토픽
    }
    
    public enum Frequency {
        IMMEDIATE,  // 즉시
        HOURLY,     // 매시간
        DAILY,      // 매일
        WEEKLY      // 매주
    }
    
    // 비즈니스 메소드
    public void activate() {
        this.isActive = true;
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public void updateLastSent() {
        this.lastSent = LocalDateTime.now();
    }
    
    public boolean shouldSendEmail() {
        if (!isActive) {
            return false;
        }
        
        if (lastSent == null) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case IMMEDIATE -> true;
            case HOURLY -> lastSent.isBefore(now.minusHours(1));
            case DAILY -> lastSent.isBefore(now.minusDays(1));
            case WEEKLY -> lastSent.isBefore(now.minusWeeks(1));
        };
    }
    
    // 팩토리 메소드
    public static EmailSubscription of(Long userId, SubscriptionType type, Frequency frequency) {
        return EmailSubscription.builder()
                .userId(userId)
                .subscriptionType(type)
                .frequency(frequency)
                .build();
    }
}