package com.example.jibmusil.email;

import com.example.jibmusil.news.NewsArticle;
import com.example.jibmusil.news.NewsRepository;
import com.example.jibmusil.user.EmailSubscription;
import com.example.jibmusil.user.User;
import com.example.jibmusil.user.UserRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final Configuration freemarkerConfig;
    private final UserRepository userRepository;
    private final NewsRepository newsRepository;
    private final EmailSubscriptionRepository emailSubscriptionRepository;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${jibmusil.email.daily-digest-time:08:00}")
    private String dailyDigestTime;
    
    @Async
    public CompletableFuture<Void> sendDailyDigest(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                List<NewsArticle> topNews = newsRepository.findTrendingNews(10);
                
                Map<String, Object> templateModel = new HashMap<>();
                templateModel.put("user", user);
                templateModel.put("articles", topNews);
                templateModel.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
                templateModel.put("digestType", "일일 뉴스 다이제스트");
                
                String htmlContent = processTemplate("daily-digest.ftl", templateModel);
                String subject = String.format("[Jibmusil] %s님의 일일 뉴스 다이제스트", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
                
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                updateEmailSubscriptionLastSent(userId, EmailSubscription.SubscriptionType.DAILY_DIGEST);
                
                log.info("Daily digest sent to user: {}", user.getEmail());
                
            } catch (Exception e) {
                log.error("Failed to send daily digest to user: {}", userId, e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> sendWeeklyDigest(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                // 최근 일주일간의 인기 뉴스
                List<NewsArticle> weeklyTopNews = newsRepository.findRecentNews(
                        LocalDateTime.now().minusDays(7), 
                        PageRequest.of(0, 15)
                );
                
                Map<String, Object> templateModel = new HashMap<>();
                templateModel.put("user", user);
                templateModel.put("articles", weeklyTopNews);
                templateModel.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
                templateModel.put("digestType", "주간 뉴스 요약");
                templateModel.put("weekRange", getWeekRange());
                
                String htmlContent = processTemplate("weekly-digest.ftl", templateModel);
                String subject = String.format("[Jibmusil] %s님의 주간 뉴스 요약", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
                
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                updateEmailSubscriptionLastSent(userId, EmailSubscription.SubscriptionType.WEEKLY_SUMMARY);
                
                log.info("Weekly digest sent to user: {}", user.getEmail());
                
            } catch (Exception e) {
                log.error("Failed to send weekly digest to user: {}", userId, e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> sendBreakingNews(NewsArticle breakingNews) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<EmailSubscription> subscriptions = emailSubscriptionRepository
                        .findActiveSubscriptionsByType(EmailSubscription.SubscriptionType.BREAKING_NEWS);
                
                for (EmailSubscription subscription : subscriptions) {
                    User user = userRepository.findById(subscription.getUserId()).orElse(null);
                    if (user == null) continue;
                    
                    Map<String, Object> templateModel = new HashMap<>();
                    templateModel.put("user", user);
                    templateModel.put("article", breakingNews);
                    templateModel.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")));
                    
                    String htmlContent = processTemplate("breaking-news.ftl", templateModel);
                    String subject = String.format("[Jibmusil 속보] %s", breakingNews.getTitle());
                    
                    sendHtmlEmail(user.getEmail(), subject, htmlContent);
                }
                
                log.info("Breaking news sent to {} subscribers", subscriptions.size());
                
            } catch (Exception e) {
                log.error("Failed to send breaking news: {}", breakingNews.getId(), e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> sendTrendingTopics(Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                // 트렌딩 토픽 및 관련 뉴스
                List<NewsArticle> trendingNews = newsRepository.findTrendingNews(8);
                
                Map<String, Object> templateModel = new HashMap<>();
                templateModel.put("user", user);
                templateModel.put("trendingArticles", trendingNews);
                templateModel.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
                
                String htmlContent = processTemplate("trending-topics.ftl", templateModel);
                String subject = String.format("[Jibmusil] 오늘의 트렌딩 토픽");
                
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                updateEmailSubscriptionLastSent(userId, EmailSubscription.SubscriptionType.TRENDING_TOPICS);
                
                log.info("Trending topics sent to user: {}", user.getEmail());
                
            } catch (Exception e) {
                log.error("Failed to send trending topics to user: {}", userId, e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> sendPersonalizedNewsletter(Long userId, List<NewsArticle> personalizedNews) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                
                Map<String, Object> templateModel = new HashMap<>();
                templateModel.put("user", user);
                templateModel.put("personalizedArticles", personalizedNews);
                templateModel.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
                
                String htmlContent = processTemplate("personalized-newsletter.ftl", templateModel);
                String subject = String.format("[Jibmusil] %s님을 위한 맞춤 뉴스", user.getFirstName() != null ? user.getFirstName() : user.getUsername());
                
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                
                log.info("Personalized newsletter sent to user: {}", user.getEmail());
                
            } catch (Exception e) {
                log.error("Failed to send personalized newsletter to user: {}", userId, e);
            }
        });
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }
    
    private String processTemplate(String templateName, Map<String, Object> model) throws IOException, TemplateException {
        Template template = freemarkerConfig.getTemplate(templateName);
        StringWriter stringWriter = new StringWriter();
        template.process(model, stringWriter);
        return stringWriter.toString();
    }
    
    @Transactional
    private void updateEmailSubscriptionLastSent(Long userId, EmailSubscription.SubscriptionType subscriptionType) {
        emailSubscriptionRepository.findByUserIdAndSubscriptionType(userId, subscriptionType)
                .ifPresent(subscription -> {
                    subscription.updateLastSent();
                    emailSubscriptionRepository.save(subscription);
                });
    }
    
    private String getWeekRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일");
        return weekStart.format(formatter) + " ~ " + now.format(formatter);
    }
    
    // 이메일 구독 관리 메소드들
    @Transactional
    public void subscribeToEmail(Long userId, EmailSubscription.SubscriptionType subscriptionType, EmailSubscription.Frequency frequency) {
        if (emailSubscriptionRepository.existsByUserIdAndSubscriptionType(userId, subscriptionType)) {
            EmailSubscription existing = emailSubscriptionRepository
                    .findByUserIdAndSubscriptionType(userId, subscriptionType).orElse(null);
            if (existing != null) {
                existing.setFrequency(frequency);
                existing.activate();
                emailSubscriptionRepository.save(existing);
            }
        } else {
            EmailSubscription subscription = EmailSubscription.of(userId, subscriptionType, frequency);
            emailSubscriptionRepository.save(subscription);
        }
        
        log.info("User {} subscribed to {} with frequency {}", userId, subscriptionType, frequency);
    }
    
    @Transactional
    public void unsubscribeFromEmail(Long userId, EmailSubscription.SubscriptionType subscriptionType) {
        emailSubscriptionRepository.findByUserIdAndSubscriptionType(userId, subscriptionType)
                .ifPresent(subscription -> {
                    subscription.deactivate();
                    emailSubscriptionRepository.save(subscription);
                    log.info("User {} unsubscribed from {}", userId, subscriptionType);
                });
    }
    
    public List<EmailSubscription> getUserEmailSubscriptions(Long userId) {
        return emailSubscriptionRepository.findByUserId(userId);
    }
    
    public boolean isSubscribed(Long userId, EmailSubscription.SubscriptionType subscriptionType) {
        return emailSubscriptionRepository.findByUserIdAndSubscriptionType(userId, subscriptionType)
                .map(EmailSubscription::getIsActive)
                .orElse(false);
    }
}