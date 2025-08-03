package com.example.jibmusil.analytics;

import com.example.jibmusil.news.NewsRepository;
import com.example.jibmusil.user.UserNewsInteractionRepository;
import com.example.jibmusil.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "분석 및 대시보드 API")
public class AnalyticsController {
    
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final UserNewsInteractionRepository interactionRepository;
    
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 통계", description = "전체 시스템의 주요 통계를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            // 기본 통계
            Long totalUsers = userRepository.countActiveUsers();
            Long totalArticles = newsRepository.count();
            Long totalInteractions = interactionRepository.count();
            
            // 최근 24시간 통계
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            Long recentInteractions = interactionRepository.countUserInteractionsSince(1L, yesterday);
            
            // 카테고리별 뉴스 수
            Map<String, Long> categoryStats = getCategoryStatistics();
            
            // 감정 분석 통계
            Map<String, Object> sentimentStats = getSentimentStatistics();
            
            // 인기 뉴스
            List<Object> topNews = newsRepository.findTrendingNews(5)
                    .stream()
                    .map(article -> Map.of(
                            "id", article.getId(),
                            "title", article.getTitle(),
                            "popularityScore", article.getPopularityScore() != null ? article.getPopularityScore() : BigDecimal.ZERO,
                            "sentimentScore", article.getSentimentScore() != null ? article.getSentimentScore() : BigDecimal.ZERO
                    ))
                    .toList();
            
            dashboard.put("overview", Map.of(
                    "totalUsers", totalUsers,
                    "totalArticles", totalArticles,
                    "totalInteractions", totalInteractions,
                    "dailyInteractions", recentInteractions
            ));
            
            dashboard.put("categoryStats", categoryStats);
            dashboard.put("sentimentStats", sentimentStats);
            dashboard.put("topNews", topNews);
            dashboard.put("lastUpdated", LocalDateTime.now());
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("Error generating dashboard statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate dashboard statistics"));
        }
    }
    
    @GetMapping("/trends")
    @Operation(summary = "트렌드 분석", description = "뉴스 트렌드 및 키워드 분석 결과를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getTrendAnalysis() {
        Map<String, Object> trends = new HashMap<>();
        
        try {
            // 시간별 뉴스 발행 추이 (Mock 데이터)
            List<Map<String, Object>> hourlyTrends = generateHourlyTrends();
            
            // 인기 키워드 (Mock 데이터)
            List<Map<String, Object>> popularKeywords = List.of(
                    Map.of("keyword", "AI", "count", 145, "trend", "up"),
                    Map.of("keyword", "경제", "count", 132, "trend", "stable"),
                    Map.of("keyword", "기술", "count", 98, "trend", "up"),
                    Map.of("keyword", "정치", "count", 87, "trend", "down"),
                    Map.of("keyword", "스포츠", "count", 76, "trend", "stable")
            );
            
            // 감정 트렌드
            List<Map<String, Object>> sentimentTrends = generateSentimentTrends();
            
            trends.put("hourlyTrends", hourlyTrends);
            trends.put("popularKeywords", popularKeywords);
            trends.put("sentimentTrends", sentimentTrends);
            trends.put("generatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            log.error("Error generating trend analysis", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate trend analysis"));
        }
    }
    
    @GetMapping("/user-engagement")
    @Operation(summary = "사용자 참여도 분석", description = "사용자 참여도 및 상호작용 패턴을 분석합니다.")
    public ResponseEntity<Map<String, Object>> getUserEngagementStats() {
        Map<String, Object> engagement = new HashMap<>();
        
        try {
            // 가장 많이 상호작용한 기사들
            List<Object[]> mostInteracted = interactionRepository.findMostInteractedArticles(PageRequest.of(0, 10));
            
            List<Map<String, Object>> topArticles = mostInteracted.stream()
                    .map(row -> Map.of(
                            "articleId", row[0],
                            "interactionCount", row[1]
                    ))
                    .toList();
            
            // 사용자 활동 패턴 (Mock 데이터)
            Map<String, Object> activityPattern = Map.of(
                    "peakHours", List.of(9, 12, 18, 21),
                    "averageReadingTime", 127, // seconds
                    "mostActiveDay", "Tuesday",
                    "engagementRate", 0.68
            );
            
            // 상호작용 유형별 통계
            Map<String, Object> interactionTypes = Map.of(
                    "views", 1250,
                    "likes", 340,
                    "shares", 89,
                    "saves", 156,
                    "clicks", 890
            );
            
            engagement.put("topArticles", topArticles);
            engagement.put("activityPattern", activityPattern);
            engagement.put("interactionTypes", interactionTypes);
            engagement.put("generatedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(engagement);
            
        } catch (Exception e) {
            log.error("Error generating user engagement statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate engagement statistics"));
        }
    }
    
    @GetMapping("/sentiment-analysis")
    @Operation(summary = "감정 분석 결과", description = "뉴스 감정 분석 결과 및 통계를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getSentimentAnalysis() {
        Map<String, Object> sentimentData = new HashMap<>();
        
        try {
            // 전체 감정 분포
            Map<String, Object> distribution = Map.of(
                    "positive", 42.3,
                    "neutral", 38.7,
                    "negative", 19.0
            );
            
            // 카테고리별 감정 분석
            Map<String, Map<String, Double>> categorysentiment = Map.of(
                    "Technology", Map.of("positive", 55.2, "neutral", 32.1, "negative", 12.7),
                    "Business", Map.of("positive", 38.9, "neutral", 45.3, "negative", 15.8),
                    "Politics", Map.of("positive", 25.4, "neutral", 41.2, "negative", 33.4),
                    "Sports", Map.of("positive", 68.7, "neutral", 28.1, "negative", 3.2),
                    "Health", Map.of("positive", 41.5, "neutral", 35.8, "negative", 22.7)
            );
            
            // 시간별 감정 변화
            List<Map<String, Object>> sentimentTimeline = generateSentimentTimeline();
            
            sentimentData.put("distribution", distribution);
            sentimentData.put("categoryBreakdown", categorysentiment);
            sentimentData.put("timeline", sentimentTimeline);
            sentimentData.put("analysisDate", LocalDateTime.now());
            
            return ResponseEntity.ok(sentimentData);
            
        } catch (Exception e) {
            log.error("Error generating sentiment analysis", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate sentiment analysis"));
        }
    }
    
    private Map<String, Long> getCategoryStatistics() {
        // 실제 구현에서는 Repository를 통해 데이터를 조회
        return Map.of(
                "Technology", 245L,
                "Business", 189L,
                "Politics", 156L,
                "Sports", 134L,
                "Health", 98L,
                "Entertainment", 87L,
                "Science", 76L
        );
    }
    
    private Map<String, Object> getSentimentStatistics() {
        return Map.of(
                "averageSentiment", 0.15,
                "positiveRatio", 0.42,
                "negativeRatio", 0.19,
                "neutralRatio", 0.39,
                "totalAnalyzed", 985L
        );
    }
    
    private List<Map<String, Object>> generateHourlyTrends() {
        // Mock 데이터 - 실제로는 데이터베이스에서 조회
        return List.of(
                Map.of("hour", 0, "count", 12),
                Map.of("hour", 1, "count", 8),
                Map.of("hour", 2, "count", 5),
                Map.of("hour", 3, "count", 3),
                Map.of("hour", 4, "count", 7),
                Map.of("hour", 5, "count", 15),
                Map.of("hour", 6, "count", 28),
                Map.of("hour", 7, "count", 45),
                Map.of("hour", 8, "count", 67),
                Map.of("hour", 9, "count", 89),
                Map.of("hour", 10, "count", 78),
                Map.of("hour", 11, "count", 82),
                Map.of("hour", 12, "count", 95),
                Map.of("hour", 13, "count", 71),
                Map.of("hour", 14, "count", 68),
                Map.of("hour", 15, "count", 74),
                Map.of("hour", 16, "count", 69),
                Map.of("hour", 17, "count", 72),
                Map.of("hour", 18, "count", 88),
                Map.of("hour", 19, "count", 91),
                Map.of("hour", 20, "count", 76),
                Map.of("hour", 21, "count", 65),
                Map.of("hour", 22, "count", 42),
                Map.of("hour", 23, "count", 28)
        );
    }
    
    private List<Map<String, Object>> generateSentimentTrends() {
        // Mock 데이터 - 지난 7일간의 감정 트렌드
        return List.of(
                Map.of("date", "2024-01-01", "positive", 0.45, "neutral", 0.38, "negative", 0.17),
                Map.of("date", "2024-01-02", "positive", 0.42, "neutral", 0.39, "negative", 0.19),
                Map.of("date", "2024-01-03", "positive", 0.48, "neutral", 0.35, "negative", 0.17),
                Map.of("date", "2024-01-04", "positive", 0.41, "neutral", 0.41, "negative", 0.18),
                Map.of("date", "2024-01-05", "positive", 0.39, "neutral", 0.42, "negative", 0.19),
                Map.of("date", "2024-01-06", "positive", 0.44, "neutral", 0.37, "negative", 0.19),
                Map.of("date", "2024-01-07", "positive", 0.43, "neutral", 0.38, "negative", 0.19)
        );
    }
    
    private List<Map<String, Object>> generateSentimentTimeline() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                Map.of("timestamp", now.minusHours(6), "sentiment", 0.23),
                Map.of("timestamp", now.minusHours(5), "sentiment", 0.18),
                Map.of("timestamp", now.minusHours(4), "sentiment", 0.31),
                Map.of("timestamp", now.minusHours(3), "sentiment", 0.15),
                Map.of("timestamp", now.minusHours(2), "sentiment", 0.27),
                Map.of("timestamp", now.minusHours(1), "sentiment", 0.22),
                Map.of("timestamp", now, "sentiment", 0.19)
        );
    }
}