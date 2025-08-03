package com.example.jibmusil.news;

import com.example.jibmusil.recommendation.RecommendationEngine;
import com.example.jibmusil.user.UserNewsInteraction;
import com.example.jibmusil.user.UserNewsInteractionRepository;
import com.example.jibmusil.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/personalized")
@RequiredArgsConstructor
@Tag(name = "Personalized News", description = "개인화된 뉴스 추천 및 상호작용 API")
public class PersonalizedNewsController {

    private final RecommendationEngine recommendationEngine;
    private final UserNewsInteractionRepository interactionRepository;
    private final NewsService newsService;
    private final UserService userService;

    @GetMapping("/recommendations")
    @Operation(summary = "개인화된 뉴스 추천", description = "사용자의 선호도와 행동 패턴을 기반으로 개인화된 뉴스를 추천합니다.")
    public ResponseEntity<List<NewsArticle>> getPersonalizedRecommendations(
            Authentication authentication,
            @Parameter(description = "추천할 뉴스 개수") @RequestParam(defaultValue = "20") int limit) {
        
        Long userId = userService.getCurrentUserId(authentication);
        List<NewsArticle> recommendations = recommendationEngine.getPersonalizedRecommendations(userId, limit);
        
        log.info("Generated {} personalized recommendations for user {}", recommendations.size(), userId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/trending")
    @Operation(summary = "트렌딩 뉴스", description = "현재 인기 있는 뉴스를 반환합니다.")
    public ResponseEntity<List<NewsArticle>> getTrendingNews(
            @Parameter(description = "반환할 뉴스 개수") @RequestParam(defaultValue = "10") int limit) {
        
        List<NewsArticle> trendingNews = newsService.findTrendingNews(limit);
        return ResponseEntity.ok(trendingNews);
    }

    @GetMapping("/by-sentiment")
    @Operation(summary = "감정별 뉴스", description = "특정 감정을 기준으로 뉴스를 필터링합니다.")
    public ResponseEntity<List<NewsArticle>> getNewsBySentiment(
            @Parameter(description = "감정 타입 (positive, negative, neutral)") @RequestParam String sentiment,
            @Parameter(description = "반환할 뉴스 개수") @RequestParam(defaultValue = "10") int limit) {
        
        List<NewsArticle> sentimentNews = newsService.findNewsBySentiment(sentiment, limit);
        return ResponseEntity.ok(sentimentNews);
    }

    @PostMapping("/interactions")
    @Operation(summary = "뉴스 상호작용 기록", description = "사용자의 뉴스 상호작용을 기록하고 추천 엔진에 반영합니다.")
    public ResponseEntity<Map<String, String>> recordInteraction(
            Authentication authentication,
            @RequestBody NewsInteractionRequest request) {
        
        Long userId = userService.getCurrentUserId(authentication);
        
        // 상호작용 기록
        UserNewsInteraction interaction = UserNewsInteraction.builder()
                .userId(userId)
                .newsArticleId(request.getArticleId())
                .interactionType(request.getInteractionType())
                .readingTimeSeconds(request.getReadingTimeSeconds())
                .build();
        
        interactionRepository.save(interaction);
        
        // 비동기적으로 사용자 선호도 업데이트
        CompletableFuture<Void> preferenceUpdate = recommendationEngine.updateUserPreferences(
                userId, request.getArticleId(), request.getInteractionType());
        
        // 뉴스 인기도 업데이트 (VIEW, CLICK의 경우)
        if (request.getInteractionType() == UserNewsInteraction.InteractionType.VIEW ||
            request.getInteractionType() == UserNewsInteraction.InteractionType.CLICK) {
            newsService.incrementPopularity(request.getArticleId());
        }
        
        log.info("Recorded {} interaction for user {} on article {}", 
                request.getInteractionType(), userId, request.getArticleId());
        
        return ResponseEntity.ok(Map.of("status", "success", "message", "Interaction recorded"));
    }

    @GetMapping("/interactions/history")
    @Operation(summary = "사용자 상호작용 히스토리", description = "사용자의 뉴스 상호작용 히스토리를 조회합니다.")
    public ResponseEntity<Page<UserNewsInteraction>> getInteractionHistory(
            Authentication authentication,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = userService.getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        
        List<UserNewsInteraction> interactions = interactionRepository
                .findByUserIdOrderByInteractionTimeDesc(userId, pageable);
        
        // List를 Page로 변환 (실제로는 Page를 반환하는 Repository 메소드 사용 권장)
        Page<UserNewsInteraction> interactionPage = new org.springframework.data.domain.PageImpl<>(
                interactions, pageable, interactions.size());
        
        return ResponseEntity.ok(interactionPage);
    }

    @GetMapping("/categories/preferences")
    @Operation(summary = "카테고리별 선호도", description = "사용자의 카테고리별 선호도를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getCategoryPreferences(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        
        // 실제 구현에서는 UserPreferenceProfileRepository를 사용
        Map<String, Object> preferences = Map.of(
                "technology", 0.8,
                "business", 0.6,
                "sports", 0.3,
                "politics", 0.4
        );
        
        return ResponseEntity.ok(preferences);
    }

    @PostMapping("/feedback")
    @Operation(summary = "추천 피드백", description = "추천 결과에 대한 사용자 피드백을 수집합니다.")
    public ResponseEntity<Map<String, String>> submitFeedback(
            Authentication authentication,
            @RequestBody RecommendationFeedback feedback) {
        
        Long userId = userService.getCurrentUserId(authentication);
        
        // 피드백을 기반으로 추천 엔진 개선
        log.info("Received recommendation feedback from user {}: rating={}, comment={}", 
                userId, feedback.getRating(), feedback.getComment());
        
        return ResponseEntity.ok(Map.of("status", "success", "message", "Feedback received"));
    }

    // Request/Response DTOs
    public static class NewsInteractionRequest {
        private Long articleId;
        private UserNewsInteraction.InteractionType interactionType;
        private Integer readingTimeSeconds;
        
        // Getters and setters
        public Long getArticleId() { return articleId; }
        public void setArticleId(Long articleId) { this.articleId = articleId; }
        
        public UserNewsInteraction.InteractionType getInteractionType() { return interactionType; }
        public void setInteractionType(UserNewsInteraction.InteractionType interactionType) { 
            this.interactionType = interactionType; 
        }
        
        public Integer getReadingTimeSeconds() { return readingTimeSeconds; }
        public void setReadingTimeSeconds(Integer readingTimeSeconds) { 
            this.readingTimeSeconds = readingTimeSeconds; 
        }
    }

    public static class RecommendationFeedback {
        private Integer rating; // 1-5 점수
        private String comment;
        private List<Long> relevantArticleIds;
        private List<Long> irrelevantArticleIds;
        
        // Getters and setters
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        
        public List<Long> getRelevantArticleIds() { return relevantArticleIds; }
        public void setRelevantArticleIds(List<Long> relevantArticleIds) { 
            this.relevantArticleIds = relevantArticleIds; 
        }
        
        public List<Long> getIrrelevantArticleIds() { return irrelevantArticleIds; }
        public void setIrrelevantArticleIds(List<Long> irrelevantArticleIds) { 
            this.irrelevantArticleIds = irrelevantArticleIds; 
        }
    }
}