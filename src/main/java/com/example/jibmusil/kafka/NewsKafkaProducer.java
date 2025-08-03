package com.example.jibmusil.kafka;

import com.example.jibmusil.news.NewsArticle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsKafkaProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String NEWS_PROCESSED_TOPIC = "news-processed";
    private static final String NEWS_VIEW_TOPIC = "news-view";
    private static final String USER_ACTIVITY_TOPIC = "user-activity";
    private static final String SENTIMENT_ANALYSIS_TOPIC = "sentiment-analysis";
    
    public void sendNewsProcessedEvent(NewsArticle article) {
        try {
            NewsProcessedEvent event = NewsProcessedEvent.builder()
                    .articleId(article.getId())
                    .title(article.getTitle())
                    .url(article.getUrl())
                    .categoryId(article.getCategoryId())
                    .sentimentScore(article.getSentimentScore())
                    .popularityScore(article.getPopularityScore())
                    .publishedAt(article.getPublishedAt())
                    .processedAt(LocalDateTime.now())
                    .build();
                    
            sendEvent(NEWS_PROCESSED_TOPIC, article.getId().toString(), event);
            log.debug("Sent news processed event for article ID: {}", article.getId());
            
        } catch (Exception e) {
            log.error("Failed to send news processed event for article ID: {}", article.getId(), e);
        }
    }
    
    public void sendNewsViewEvent(Long articleId) {
        try {
            NewsViewEvent event = NewsViewEvent.builder()
                    .articleId(articleId)
                    .viewedAt(LocalDateTime.now())
                    .build();
                    
            sendEvent(NEWS_VIEW_TOPIC, articleId.toString(), event);
            log.debug("Sent news view event for article ID: {}", articleId);
            
        } catch (Exception e) {
            log.error("Failed to send news view event for article ID: {}", articleId, e);
        }
    }
    
    public void sendUserActivityEvent(Long userId, String activityType, Long articleId) {
        try {
            UserActivityEvent event = UserActivityEvent.builder()
                    .userId(userId)
                    .articleId(articleId)
                    .activityType(activityType)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
            sendEvent(USER_ACTIVITY_TOPIC, userId.toString(), event);
            log.debug("Sent user activity event: user={}, activity={}, article={}", 
                     userId, activityType, articleId);
            
        } catch (Exception e) {
            log.error("Failed to send user activity event", e);
        }
    }
    
    public void sendSentimentAnalysisEvent(Long articleId, String text, Object sentimentResult) {
        try {
            SentimentAnalysisEvent event = SentimentAnalysisEvent.builder()
                    .articleId(articleId)
                    .text(text)
                    .sentimentResult(sentimentResult)
                    .analyzedAt(LocalDateTime.now())
                    .build();
                    
            sendEvent(SENTIMENT_ANALYSIS_TOPIC, articleId.toString(), event);
            log.debug("Sent sentiment analysis event for article ID: {}", articleId);
            
        } catch (Exception e) {
            log.error("Failed to send sentiment analysis event for article ID: {}", articleId, e);
        }
    }
    
    private void sendEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send event to topic {} with key {}", topic, key, throwable);
            } else {
                log.debug("Successfully sent event to topic {} with key {} at offset {}",
                         topic, key, result.getRecordMetadata().offset());
            }
        });
    }
    
    // Event DTOs
    @lombok.Builder
    @lombok.Data
    public static class NewsProcessedEvent {
        private Long articleId;
        private String title;
        private String url;
        private Long categoryId;
        private Object sentimentScore;
        private Object popularityScore;
        private LocalDateTime publishedAt;
        private LocalDateTime processedAt;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class NewsViewEvent {
        private Long articleId;
        private LocalDateTime viewedAt;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class UserActivityEvent {
        private Long userId;
        private Long articleId;
        private String activityType;
        private LocalDateTime timestamp;
    }
    
    @lombok.Builder
    @lombok.Data
    public static class SentimentAnalysisEvent {
        private Long articleId;
        private String text;
        private Object sentimentResult;
        private LocalDateTime analyzedAt;
    }
}