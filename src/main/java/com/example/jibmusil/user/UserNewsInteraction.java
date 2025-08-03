package com.example.jibmusil.user;

import com.example.jibmusil.news.NewsArticle;
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
@Table(name = "user_news_interactions", indexes = {
    @Index(name = "idx_user_interaction", columnList = "userId,interactionType"),
    @Index(name = "idx_article_interaction", columnList = "newsArticleId,interactionType"),
    @Index(name = "idx_interaction_time", columnList = "interactionTime")
})
public class UserNewsInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "news_article_id", nullable = false)
    private Long newsArticleId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;
    
    @CreationTimestamp
    @Column(name = "interaction_time")
    private LocalDateTime interactionTime;
    
    @Column(name = "reading_time_seconds")
    @Builder.Default
    private Integer readingTimeSeconds = 0;
    
    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_article_id", insertable = false, updatable = false)
    private NewsArticle newsArticle;
    
    public enum InteractionType {
        VIEW,       // 뉴스 조회
        CLICK,      // 뉴스 클릭
        LIKE,       // 좋아요
        SHARE,      // 공유
        SAVE,       // 저장
        DISLIKE     // 싫어요
    }
    
    // 팩토리 메소드
    public static UserNewsInteraction of(Long userId, Long newsArticleId, InteractionType type) {
        return UserNewsInteraction.builder()
                .userId(userId)
                .newsArticleId(newsArticleId)
                .interactionType(type)
                .build();
    }
    
    public static UserNewsInteraction withReadingTime(Long userId, Long newsArticleId, int readingTime) {
        return UserNewsInteraction.builder()
                .userId(userId)
                .newsArticleId(newsArticleId)
                .interactionType(InteractionType.VIEW)
                .readingTimeSeconds(readingTime)
                .build();
    }
}