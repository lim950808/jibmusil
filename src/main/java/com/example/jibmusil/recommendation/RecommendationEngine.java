package com.example.jibmusil.recommendation;

import com.example.jibmusil.news.NewsArticle;
import com.example.jibmusil.news.NewsRepository;
import com.example.jibmusil.user.User;
import com.example.jibmusil.user.UserNewsInteraction;
import com.example.jibmusil.user.UserPreferenceProfile;
import com.example.jibmusil.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationEngine {
    
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;
    private final UserNewsInteractionRepository interactionRepository;
    private final UserPreferenceProfileRepository preferenceRepository;
    
    @Cacheable(value = "recommendations", key = "#userId + ':' + #limit")
    public List<NewsArticle> getPersonalizedRecommendations(Long userId, int limit) {
        log.info("Generating personalized recommendations for user {}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, returning trending news", userId);
            return newsRepository.findTrendingNews(limit);
        }
        
        User user = userOpt.get();
        
        // 1. 사용자 선호도 프로필 기반 추천
        List<NewsArticle> preferenceBasedNews = getPreferenceBasedRecommendations(user, limit);
        
        // 2. 유사한 사용자 기반 협업 필터링
        List<NewsArticle> collaborativeNews = getCollaborativeFilteringRecommendations(user, limit);
        
        // 3. 콘텐츠 기반 필터링
        List<NewsArticle> contentBasedNews = getContentBasedRecommendations(user, limit);
        
        // 4. 하이브리드 추천 (가중치 조합)
        return combineRecommendations(preferenceBasedNews, collaborativeNews, contentBasedNews, limit);
    }
    
    private List<NewsArticle> getPreferenceBasedRecommendations(User user, int limit) {
        List<UserPreferenceProfile> preferences = preferenceRepository.findByUserIdOrderByPreferenceScoreDesc(user.getId());
        
        if (preferences.isEmpty()) {
            return newsRepository.findTrendingNews(limit);
        }
        
        List<NewsArticle> recommendations = new ArrayList<>();
        
        for (UserPreferenceProfile preference : preferences) {
            if (preference.isHighPreference()) {
                int categoryLimit = Math.max(1, (int) (limit * preference.getPreferenceScore().doubleValue()));
                List<NewsArticle> categoryNews = newsRepository.findByCategoryIdOrderByPopularityAndDate(
                    preference.getCategoryId(), 
                    PageRequest.of(0, categoryLimit)
                );
                recommendations.addAll(categoryNews);
            }
        }
        
        return recommendations.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<NewsArticle> getCollaborativeFilteringRecommendations(User user, int limit) {
        // 유사한 사용자 찾기
        List<User> similarUsers = findSimilarUsers(user, 10);
        
        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<Long> userInteractedArticles = getUserInteractedArticleIds(user.getId());
        List<NewsArticle> recommendations = new ArrayList<>();
        
        for (User similarUser : similarUsers) {
            List<UserNewsInteraction> interactions = interactionRepository
                    .findPositiveInteractionsByUserId(similarUser.getId(), PageRequest.of(0, 20));
            
            for (UserNewsInteraction interaction : interactions) {
                if (!userInteractedArticles.contains(interaction.getNewsArticleId())) {
                    newsRepository.findById(interaction.getNewsArticleId())
                            .ifPresent(recommendations::add);
                }
            }
        }
        
        return recommendations.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<NewsArticle> getContentBasedRecommendations(User user, int limit) {
        // 사용자가 최근에 상호작용한 뉴스의 키워드와 유사한 뉴스 찾기
        List<UserNewsInteraction> recentInteractions = interactionRepository
                .findRecentPositiveInteractionsByUserId(user.getId(), LocalDateTime.now().minusDays(7), PageRequest.of(0, 10));
        
        if (recentInteractions.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> userKeywords = new HashSet<>();
        Set<Long> interactedArticleIds = new HashSet<>();
        
        for (UserNewsInteraction interaction : recentInteractions) {
            interactedArticleIds.add(interaction.getNewsArticleId());
            newsRepository.findById(interaction.getNewsArticleId()).ifPresent(article -> {
                if (article.getKeywords() != null) {
                    userKeywords.addAll(Arrays.asList(article.getKeywords()));
                }
            });
        }
        
        // 유사한 키워드를 가진 뉴스 찾기 (Elasticsearch 활용)
        List<NewsArticle> recommendations = new ArrayList<>();
        for (String keyword : userKeywords) {
            // 여기서는 간단한 구현으로 대체, 실제로는 Elasticsearch의 More Like This 쿼리 사용
            List<NewsArticle> keywordNews = newsRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 5))
                    .getContent()
                    .stream()
                    .filter(article -> !interactedArticleIds.contains(article.getId()))
                    .filter(article -> article.getKeywords() != null && 
                                     Arrays.asList(article.getKeywords()).contains(keyword))
                    .collect(Collectors.toList());
            recommendations.addAll(keywordNews);
        }
        
        return recommendations.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<NewsArticle> combineRecommendations(
            List<NewsArticle> preferenceBasedNews,
            List<NewsArticle> collaborativeNews,
            List<NewsArticle> contentBasedNews,
            int limit) {
        
        Map<Long, RecommendationScore> scoreMap = new HashMap<>();
        
        // 선호도 기반 (가중치 0.5)
        addToScoreMap(scoreMap, preferenceBasedNews, 0.5);
        
        // 협업 필터링 (가중치 0.3)
        addToScoreMap(scoreMap, collaborativeNews, 0.3);
        
        // 콘텐츠 기반 (가중치 0.2)
        addToScoreMap(scoreMap, contentBasedNews, 0.2);
        
        // 점수순으로 정렬하여 반환
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, RecommendationScore>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> entry.getValue().getArticle())
                .collect(Collectors.toList());
    }
    
    private void addToScoreMap(Map<Long, RecommendationScore> scoreMap, List<NewsArticle> articles, double weight) {
        for (int i = 0; i < articles.size(); i++) {
            NewsArticle article = articles.get(i);
            double positionScore = 1.0 - (double) i / articles.size(); // 위치가 높을수록 높은 점수
            double totalScore = positionScore * weight;
            
            scoreMap.merge(article.getId(), 
                new RecommendationScore(article, totalScore),
                (existing, replacement) -> new RecommendationScore(
                    existing.getArticle(), 
                    existing.getScore() + replacement.getScore()
                ));
        }
    }
    
    private List<User> findSimilarUsers(User user, int limit) {
        // 사용자 간 코사인 유사도 계산
        List<UserPreferenceProfile> userPreferences = preferenceRepository.findByUserId(user.getId());
        if (userPreferences.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<Long, Double> userVector = userPreferences.stream()
                .collect(Collectors.toMap(
                    UserPreferenceProfile::getCategoryId,
                    pref -> pref.getPreferenceScore().doubleValue()
                ));
        
        List<User> allUsers = userRepository.findAllActiveUsersExcept(user.getId());
        List<UserSimilarity> similarities = new ArrayList<>();
        
        for (User otherUser : allUsers) {
            List<UserPreferenceProfile> otherPreferences = preferenceRepository.findByUserId(otherUser.getId());
            Map<Long, Double> otherVector = otherPreferences.stream()
                    .collect(Collectors.toMap(
                        UserPreferenceProfile::getCategoryId,
                        pref -> pref.getPreferenceScore().doubleValue()
                    ));
            
            double similarity = calculateCosineSimilarity(userVector, otherVector);
            if (similarity > 0.1) { // 최소 유사도 임계값
                similarities.add(new UserSimilarity(otherUser, similarity));
            }
        }
        
        return similarities.stream()
                .sorted(Comparator.comparingDouble(UserSimilarity::getSimilarity).reversed())
                .limit(limit)
                .map(UserSimilarity::getUser)
                .collect(Collectors.toList());
    }
    
    private double calculateCosineSimilarity(Map<Long, Double> vector1, Map<Long, Double> vector2) {
        Set<Long> commonKeys = new HashSet<>(vector1.keySet());
        commonKeys.retainAll(vector2.keySet());
        
        if (commonKeys.isEmpty()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (Long key : commonKeys) {
            double val1 = vector1.get(key);
            double val2 = vector2.get(key);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private Set<Long> getUserInteractedArticleIds(Long userId) {
        return interactionRepository.findByUserId(userId).stream()
                .map(UserNewsInteraction::getNewsArticleId)
                .collect(Collectors.toSet());
    }
    
    @Async
    @Transactional
    public CompletableFuture<Void> updateUserPreferences(Long userId, Long articleId, UserNewsInteraction.InteractionType interactionType) {
        return CompletableFuture.runAsync(() -> {
            try {
                Optional<NewsArticle> articleOpt = newsRepository.findById(articleId);
                if (articleOpt.isEmpty()) {
                    log.warn("Article {} not found for preference update", articleId);
                    return;
                }
                
                NewsArticle article = articleOpt.get();
                Long categoryId = article.getCategoryId();
                
                if (categoryId == null) {
                    log.warn("Article {} has no category for preference update", articleId);
                    return;
                }
                
                UserPreferenceProfile preference = preferenceRepository
                        .findByUserIdAndCategoryId(userId, categoryId)
                        .orElseGet(() -> UserPreferenceProfile.of(userId, categoryId));
                
                // 상호작용 타입에 따른 선호도 점수 조정
                BigDecimal adjustment = getPreferenceAdjustment(interactionType);
                
                if (adjustment.compareTo(BigDecimal.ZERO) > 0) {
                    preference.increasePreference(adjustment);
                } else {
                    preference.decreasePreference(adjustment.abs());
                }
                
                preferenceRepository.save(preference);
                log.debug("Updated preference for user {} category {} with adjustment {}", 
                         userId, categoryId, adjustment);
                
            } catch (Exception e) {
                log.error("Error updating user preferences for user {} article {}", userId, articleId, e);
            }
        });
    }
    
    private BigDecimal getPreferenceAdjustment(UserNewsInteraction.InteractionType interactionType) {
        return switch (interactionType) {
            case VIEW -> new BigDecimal("0.01");
            case CLICK -> new BigDecimal("0.05");
            case LIKE -> new BigDecimal("0.10");
            case SHARE -> new BigDecimal("0.15");
            case SAVE -> new BigDecimal("0.20");
            case DISLIKE -> new BigDecimal("-0.10");
        };
    }
    
    // Inner classes
    private static class RecommendationScore implements Comparable<RecommendationScore> {
        private final NewsArticle article;
        private final double score;
        
        public RecommendationScore(NewsArticle article, double score) {
            this.article = article;
            this.score = score;
        }
        
        public NewsArticle getArticle() { return article; }
        public double getScore() { return score; }
        
        @Override
        public int compareTo(RecommendationScore other) {
            return Double.compare(this.score, other.score);
        }
    }
    
    private static class UserSimilarity {
        private final User user;
        private final double similarity;
        
        public UserSimilarity(User user, double similarity) {
            this.user = user;
            this.similarity = similarity;
        }
        
        public User getUser() { return user; }
        public double getSimilarity() { return similarity; }
    }
}