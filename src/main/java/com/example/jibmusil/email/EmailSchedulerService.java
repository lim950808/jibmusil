package com.example.jibmusil.email;

import com.example.jibmusil.news.NewsArticle;
import com.example.jibmusil.news.NewsRepository;
import com.example.jibmusil.recommendation.RecommendationEngine;
import com.example.jibmusil.user.EmailSubscription;
import com.example.jibmusil.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailSchedulerService {
    
    private final EmailService emailService;
    private final EmailSubscriptionRepository emailSubscriptionRepository;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final RecommendationEngine recommendationEngine;
    
    // 매일 오전 8시에 일일 다이제스트 발송
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyDigests() {
        log.info("Starting daily digest email sending job");
        
        List<EmailSubscription> dailySubscriptions = emailSubscriptionRepository
                .findActiveSubscriptionsByType(EmailSubscription.SubscriptionType.DAILY_DIGEST);
        
        for (EmailSubscription subscription : dailySubscriptions) {
            if (subscription.shouldSendEmail()) {
                try {
                    emailService.sendDailyDigest(subscription.getUserId());
                    log.debug("Daily digest scheduled for user: {}", subscription.getUserId());
                } catch (Exception e) {
                    log.error("Failed to send daily digest to user: {}", subscription.getUserId(), e);
                }
            }
        }
        
        log.info("Daily digest job completed. Processed {} subscriptions", dailySubscriptions.size());
    }
    
    // 매주 월요일 오전 9시에 주간 요약 발송
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyDigests() {
        log.info("Starting weekly digest email sending job");
        
        List<EmailSubscription> weeklySubscriptions = emailSubscriptionRepository
                .findActiveSubscriptionsByType(EmailSubscription.SubscriptionType.WEEKLY_SUMMARY);
        
        for (EmailSubscription subscription : weeklySubscriptions) {
            if (subscription.shouldSendEmail()) {
                try {
                    emailService.sendWeeklyDigest(subscription.getUserId());
                    log.debug("Weekly digest scheduled for user: {}", subscription.getUserId());
                } catch (Exception e) {
                    log.error("Failed to send weekly digest to user: {}", subscription.getUserId(), e);
                }
            }
        }
        
        log.info("Weekly digest job completed. Processed {} subscriptions", weeklySubscriptions.size());
    }
    
    // 매시간마다 트렌딩 토픽 확인 및 발송
    @Scheduled(cron = "0 0 * * * ?")
    public void sendTrendingTopics() {
        log.info("Starting trending topics email sending job");
        
        List<EmailSubscription> trendingSubscriptions = emailSubscriptionRepository
                .findActiveSubscriptionsByType(EmailSubscription.SubscriptionType.TRENDING_TOPICS);
        
        // 최근 1시간 내에 인기도가 급상승한 뉴스가 있는지 확인
        List<NewsArticle> hotTrendingNews = newsRepository.findTrendingNews(3);
        
        if (!hotTrendingNews.isEmpty()) {
            for (EmailSubscription subscription : trendingSubscriptions) {
                if (subscription.shouldSendEmail() && 
                    subscription.getFrequency() == EmailSubscription.Frequency.HOURLY) {
                    try {
                        emailService.sendTrendingTopics(subscription.getUserId());
                        log.debug("Trending topics scheduled for user: {}", subscription.getUserId());
                    } catch (Exception e) {
                        log.error("Failed to send trending topics to user: {}", subscription.getUserId(), e);
                    }
                }
            }
        }
        
        log.info("Trending topics job completed. Found {} trending news", hotTrendingNews.size());
    }
    
    // 5분마다 속보 확인 및 발송
    @Scheduled(fixedRate = 300000) // 5분마다
    public void checkForBreakingNews() {
        log.debug("Checking for breaking news");
        
        // 최근 10분 내에 생성된 뉴스 중 인기도가 높은 것들을 속보로 간주
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<NewsArticle> recentNews = newsRepository.findRecentNews(tenMinutesAgo, PageRequest.of(0, 5));
        
        for (NewsArticle article : recentNews) {
            // 인기도 점수가 높거나 특정 키워드가 포함된 경우 속보로 판단
            if (isBreakingNews(article)) {
                try {
                    emailService.sendBreakingNews(article);
                    log.info("Breaking news sent for article: {}", article.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send breaking news for article: {}", article.getId(), e);
                }
            }
        }
    }
    
    // 개인화된 뉴스레터 발송 (매일 오후 6시)
    @Scheduled(cron = "0 0 18 * * ?")
    public void sendPersonalizedNewsletters() {
        log.info("Starting personalized newsletter sending job");
        
        List<Long> activeUserIds = userRepository.findAllActiveUsers()
                .stream()
                .map(user -> user.getId())
                .toList();
        
        for (Long userId : activeUserIds) {
            try {
                // 사용자별 개인화된 추천 뉴스 생성
                List<NewsArticle> personalizedNews = recommendationEngine.getPersonalizedRecommendations(userId, 8);
                
                if (!personalizedNews.isEmpty()) {
                    emailService.sendPersonalizedNewsletter(userId, personalizedNews);
                    log.debug("Personalized newsletter scheduled for user: {}", userId);
                }
                
            } catch (Exception e) {
                log.error("Failed to send personalized newsletter to user: {}", userId, e);
            }
        }
        
        log.info("Personalized newsletter job completed for {} users", activeUserIds.size());
    }
    
    // 이메일 발송 통계 및 정리 작업 (매일 자정)
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupAndGenerateStats() {
        log.info("Starting email cleanup and statistics generation");
        
        try {
            // 비활성 구독 정리
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            List<EmailSubscription> oldInactiveSubscriptions = emailSubscriptionRepository
                    .findAll()
                    .stream()
                    .filter(sub -> !sub.getIsActive() && 
                                 sub.getCreatedAt().isBefore(threeMonthsAgo))
                    .toList();
            
            if (!oldInactiveSubscriptions.isEmpty()) {
                emailSubscriptionRepository.deleteAll(oldInactiveSubscriptions);
                log.info("Cleaned up {} old inactive email subscriptions", oldInactiveSubscriptions.size());
            }
            
            // 발송 통계 로깅
            logEmailStatistics();
            
        } catch (Exception e) {
            log.error("Error during email cleanup and statistics generation", e);
        }
    }
    
    private boolean isBreakingNews(NewsArticle article) {
        // 속보 판단 로직
        if (article.getPopularityScore() != null && 
            article.getPopularityScore().compareTo(new BigDecimal("100")) >= 0) {
            return true;
        }
        
        // 특정 키워드가 포함된 경우
        String title = article.getTitle().toLowerCase();
        String[] breakingKeywords = {"긴급", "속보", "breaking", "urgent", "alert", "emergency"};
        
        for (String keyword : breakingKeywords) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void logEmailStatistics() {
        try {
            Long totalActiveSubscriptions = emailSubscriptionRepository.count();
            Long dailyDigestCount = emailSubscriptionRepository
                    .countActiveSubscriptionsByType(EmailSubscription.SubscriptionType.DAILY_DIGEST);
            Long weeklyDigestCount = emailSubscriptionRepository
                    .countActiveSubscriptionsByType(EmailSubscription.SubscriptionType.WEEKLY_SUMMARY);
            Long breakingNewsCount = emailSubscriptionRepository
                    .countActiveSubscriptionsByType(EmailSubscription.SubscriptionType.BREAKING_NEWS);
            Long trendingTopicsCount = emailSubscriptionRepository
                    .countActiveSubscriptionsByType(EmailSubscription.SubscriptionType.TRENDING_TOPICS);
            
            log.info("Email Subscription Statistics - Total: {}, Daily: {}, Weekly: {}, Breaking: {}, Trending: {}",
                    totalActiveSubscriptions, dailyDigestCount, weeklyDigestCount, breakingNewsCount, trendingTopicsCount);
                    
        } catch (Exception e) {
            log.error("Error generating email statistics", e);
        }
    }
}