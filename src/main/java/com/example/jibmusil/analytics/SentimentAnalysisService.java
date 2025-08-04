package com.example.jibmusil.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Spring AI imports - 임시로 주석 처리
// import org.springframework.ai.chat.ChatClient;
// import org.springframework.ai.chat.ChatResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SentimentAnalysisService {

    // private final ChatClient chatClient; // 임시 주석

    private static final String SENTIMENT_ANALYSIS_PROMPT = """
            Analyze the sentiment of the following text and return a score between -1.0 and 1.0:
            - -1.0 = Very Negative
            - -0.5 = Negative  
            - 0.0 = Neutral
            - 0.5 = Positive
            - 1.0 = Very Positive
            
            Please respond with ONLY the numerical score (e.g., 0.3, -0.7, etc.), no other text.
            
            Text to analyze: {text}
            """;

    private static final Pattern SCORE_PATTERN = Pattern.compile("(-?[01](?:\\.[0-9]+)?)");

    @Cacheable(value = "sentiment", key = "#text.hashCode()")
    public Mono<BigDecimal> analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Mono.just(BigDecimal.ZERO);
        }

        return Mono.fromCallable(() -> {
            try {
                log.debug("Analyzing sentiment for text: {}", text.substring(0, Math.min(100, text.length())));
                
                // AI 분석 대신 키워드 기반 분석 사용
                return keywordBasedSentimentAnalysis(text);
                
            } catch (Exception e) {
                log.warn("Failed to analyze sentiment, using neutral score", e);
                return BigDecimal.ZERO;
            }
        });
    }

    public Mono<SentimentResult> analyzeSentimentDetailed(String text) {
        return analyzeSentiment(text)
                .map(score -> {
                    String label = getSentimentLabel(score);
                    double confidence = calculateConfidence(text, score);
                    return new SentimentResult(score, label, confidence);
                });
    }

    private BigDecimal parseSentimentScore(String result, String originalText) {
        Matcher matcher = SCORE_PATTERN.matcher(result);
        
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                score = Math.max(-1.0, Math.min(1.0, score)); // 범위 제한
                return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse sentiment score from AI response: {}", result);
            }
        }
        
        log.warn("AI response did not contain valid sentiment score, falling back to keyword analysis");
        return keywordBasedSentimentAnalysis(originalText);
    }

    private BigDecimal keywordBasedSentimentAnalysis(String text) {
        String lowerText = text.toLowerCase();
        
        String[] positiveWords = {
            "good", "great", "excellent", "amazing", "wonderful", "fantastic", "awesome", 
            "positive", "success", "win", "victory", "achievement", "breakthrough", "progress",
            "love", "like", "enjoy", "happy", "pleased", "satisfied", "excited", "thrilled",
            "best", "better", "improved", "upgrade", "advance", "growth", "opportunity"
        };
        
        String[] negativeWords = {
            "bad", "terrible", "awful", "horrible", "disaster", "crisis", "problem", "issue",
            "fail", "failure", "loss", "defeat", "decline", "crash", "collapse", "emergency",
            "hate", "dislike", "angry", "upset", "disappointed", "frustrated", "concerned",
            "worst", "worse", "decline", "drop", "fall", "threat", "risk", "danger"
        };
        
        String[] neutralWords = {
            "said", "according", "reported", "announced", "stated", "mentioned", "noted",
            "analysis", "study", "research", "data", "statistics", "information", "details"
        };
        
        int positiveCount = countKeywords(lowerText, positiveWords);
        int negativeCount = countKeywords(lowerText, negativeWords);
        int neutralCount = countKeywords(lowerText, neutralWords);
        
        int totalSentimentWords = positiveCount + negativeCount;
        
        if (totalSentimentWords == 0 || neutralCount > totalSentimentWords) {
            return BigDecimal.ZERO;
        }
        
        double score = (double) (positiveCount - negativeCount) / totalSentimentWords;
        score = Math.max(-1.0, Math.min(1.0, score));
        
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
    
    private int countKeywords(String text, String[] keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }
    
    private String getSentimentLabel(BigDecimal score) {
        double value = score.doubleValue();
        
        if (value >= 0.5) return "Very Positive";
        if (value >= 0.1) return "Positive";
        if (value <= -0.5) return "Very Negative";
        if (value <= -0.1) return "Negative";
        return "Neutral";
    }
    
    private double calculateConfidence(String text, BigDecimal score) {
        // 텍스트 길이와 감정 점수의 절댓값을 기반으로 신뢰도 계산
        int textLength = text.length();
        double scoreAbs = Math.abs(score.doubleValue());
        
        double lengthFactor = Math.min(1.0, textLength / 200.0); // 200자 기준으로 정규화
        double scoreFactor = scoreAbs; // 절댓값이 클수록 확신도 높음
        
        return Math.min(0.95, 0.5 + (lengthFactor * 0.3) + (scoreFactor * 0.2));
    }

    public record SentimentResult(
            BigDecimal score,
            String label,
            double confidence
    ) {}
}