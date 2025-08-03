package com.example.jibmusil.user;

import com.example.jibmusil.news.NewsCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_preference_profiles", 
       uniqueConstraints = @UniqueConstraint(name = "unique_user_category", columnNames = {"userId", "categoryId"}))
public class UserPreferenceProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "preference_score", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal preferenceScore = new BigDecimal("0.5000"); // 0.0000 ~ 1.0000
    
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private NewsCategory category;
    
    // 비즈니스 메소드
    public void increasePreference(BigDecimal amount) {
        this.preferenceScore = this.preferenceScore.add(amount);
        if (this.preferenceScore.compareTo(BigDecimal.ONE) > 0) {
            this.preferenceScore = BigDecimal.ONE;
        }
    }
    
    public void decreasePreference(BigDecimal amount) {
        this.preferenceScore = this.preferenceScore.subtract(amount);
        if (this.preferenceScore.compareTo(BigDecimal.ZERO) < 0) {
            this.preferenceScore = BigDecimal.ZERO;
        }
    }
    
    public boolean isHighPreference() {
        return preferenceScore.compareTo(new BigDecimal("0.7")) >= 0;
    }
    
    public boolean isLowPreference() {
        return preferenceScore.compareTo(new BigDecimal("0.3")) <= 0;
    }
    
    // 팩토리 메소드
    public static UserPreferenceProfile of(Long userId, Long categoryId) {
        return UserPreferenceProfile.builder()
                .userId(userId)
                .categoryId(categoryId)
                .build();
    }
    
    public static UserPreferenceProfile withScore(Long userId, Long categoryId, BigDecimal score) {
        return UserPreferenceProfile.builder()
                .userId(userId)
                .categoryId(categoryId)
                .preferenceScore(score)
                .build();
    }
}