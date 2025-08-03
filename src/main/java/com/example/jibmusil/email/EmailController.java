package com.example.jibmusil.email;

import com.example.jibmusil.user.EmailSubscription;
import com.example.jibmusil.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email Subscription", description = "이메일 구독 관리 API")
public class EmailController {
    
    private final EmailService emailService;
    private final UserService userService;
    
    @GetMapping("/subscriptions")
    @Operation(summary = "이메일 구독 목록 조회", description = "사용자의 모든 이메일 구독 정보를 조회합니다.")
    public ResponseEntity<List<EmailSubscription>> getSubscriptions(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        List<EmailSubscription> subscriptions = emailService.getUserEmailSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }
    
    @PostMapping("/subscribe")
    @Operation(summary = "이메일 구독", description = "특정 유형의 이메일 구독을 시작합니다.")
    public ResponseEntity<Map<String, String>> subscribe(
            Authentication authentication,
            @RequestBody SubscriptionRequest request) {
        
        Long userId = userService.getCurrentUserId(authentication);
        
        try {
            emailService.subscribeToEmail(userId, request.getSubscriptionType(), request.getFrequency());
            
            log.info("User {} subscribed to {} with frequency {}", 
                    userId, request.getSubscriptionType(), request.getFrequency());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Successfully subscribed to " + request.getSubscriptionType()
            ));
            
        } catch (Exception e) {
            log.error("Failed to subscribe user {} to {}", userId, request.getSubscriptionType(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to subscribe: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/unsubscribe")
    @Operation(summary = "이메일 구독 해지", description = "특정 유형의 이메일 구독을 해지합니다.")
    public ResponseEntity<Map<String, String>> unsubscribe(
            Authentication authentication,
            @RequestBody UnsubscribeRequest request) {
        
        Long userId = userService.getCurrentUserId(authentication);
        
        try {
            emailService.unsubscribeFromEmail(userId, request.getSubscriptionType());
            
            log.info("User {} unsubscribed from {}", userId, request.getSubscriptionType());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Successfully unsubscribed from " + request.getSubscriptionType()
            ));
            
        } catch (Exception e) {
            log.error("Failed to unsubscribe user {} from {}", userId, request.getSubscriptionType(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to unsubscribe: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/subscription/status")
    @Operation(summary = "구독 상태 확인", description = "특정 이메일 유형의 구독 상태를 확인합니다.")
    public ResponseEntity<Map<String, Boolean>> getSubscriptionStatus(
            Authentication authentication,
            @Parameter(description = "구독 유형") @RequestParam EmailSubscription.SubscriptionType subscriptionType) {
        
        Long userId = userService.getCurrentUserId(authentication);
        boolean isSubscribed = emailService.isSubscribed(userId, subscriptionType);
        
        return ResponseEntity.ok(Map.of("subscribed", isSubscribed));
    }
    
    @PostMapping("/test/daily-digest")
    @Operation(summary = "일일 다이제스트 테스트 발송", description = "테스트용 일일 다이제스트 이메일을 즉시 발송합니다.")
    public ResponseEntity<Map<String, String>> sendTestDailyDigest(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        
        try {
            emailService.sendDailyDigest(userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test daily digest sent successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to send test daily digest to user {}", userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to send test email: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/test/weekly-digest")
    @Operation(summary = "주간 다이제스트 테스트 발송", description = "테스트용 주간 다이제스트 이메일을 즉시 발송합니다.")
    public ResponseEntity<Map<String, String>> sendTestWeeklyDigest(Authentication authentication) {
        Long userId = userService.getCurrentUserId(authentication);
        
        try {
            emailService.sendWeeklyDigest(userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test weekly digest sent successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to send test weekly digest to user {}", userId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to send test email: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/subscription-types")
    @Operation(summary = "구독 유형 목록", description = "사용 가능한 모든 이메일 구독 유형을 반환합니다.")
    public ResponseEntity<Map<String, Object>> getSubscriptionTypes() {
        Map<String, Object> subscriptionTypes = Map.of(
            "DAILY_DIGEST", Map.of(
                "name", "일일 뉴스 다이제스트",
                "description", "매일 주요 뉴스를 요약해서 보내드립니다",
                "availableFrequencies", List.of("DAILY")
            ),
            "WEEKLY_SUMMARY", Map.of(
                "name", "주간 뉴스 요약",
                "description", "일주일간의 주요 뉴스를 정리해서 보내드립니다",
                "availableFrequencies", List.of("WEEKLY")
            ),
            "BREAKING_NEWS", Map.of(
                "name", "속보 알림",
                "description", "긴급한 뉴스가 발생하면 즉시 알려드립니다",
                "availableFrequencies", List.of("IMMEDIATE")
            ),
            "TRENDING_TOPICS", Map.of(
                "name", "트렌딩 토픽",
                "description", "현재 화제가 되고 있는 뉴스를 알려드립니다",
                "availableFrequencies", List.of("HOURLY", "DAILY")
            )
        );
        
        return ResponseEntity.ok(subscriptionTypes);
    }
    
    // Request DTOs
    public static class SubscriptionRequest {
        private EmailSubscription.SubscriptionType subscriptionType;
        private EmailSubscription.Frequency frequency;
        
        public EmailSubscription.SubscriptionType getSubscriptionType() {
            return subscriptionType;
        }
        
        public void setSubscriptionType(EmailSubscription.SubscriptionType subscriptionType) {
            this.subscriptionType = subscriptionType;
        }
        
        public EmailSubscription.Frequency getFrequency() {
            return frequency;
        }
        
        public void setFrequency(EmailSubscription.Frequency frequency) {
            this.frequency = frequency;
        }
    }
    
    public static class UnsubscribeRequest {
        private EmailSubscription.SubscriptionType subscriptionType;
        
        public EmailSubscription.SubscriptionType getSubscriptionType() {
            return subscriptionType;
        }
        
        public void setSubscriptionType(EmailSubscription.SubscriptionType subscriptionType) {
            this.subscriptionType = subscriptionType;
        }
    }
}