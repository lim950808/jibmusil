package com.example.jibmusil.news;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// import org.springframework.data.elasticsearch.annotations.Document;
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "news_articles", indexes = {
    @Index(name = "idx_published_at", columnList = "publishedAt"),
    @Index(name = "idx_category", columnList = "categoryId"),
    @Index(name = "idx_sentiment", columnList = "sentimentScore"),
    @Index(name = "idx_popularity", columnList = "popularityScore"),
    @Index(name = "idx_url", columnList = "url", unique = true)
})
// @Document(indexName = "news")
public class NewsArticle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Field(type = FieldType.Long)
    private Long id;
    
    @Column(length = 500, nullable = false)
    // @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    // @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Column(columnDefinition = "LONGTEXT")
    // @Field(type = FieldType.Text, analyzer = "standard")
    private String content;
    
    @Column(length = 255)
    // @Field(type = FieldType.Keyword)
    private String author;
    
    @Column(length = 1000, nullable = false, unique = true)
    // @Field(type = FieldType.Keyword)
    private String url;
    
    @Column(name = "url_to_image", length = 1000)
    // @Field(type = FieldType.Keyword)
    @JsonProperty("urlToImage")
    private String urlToImage;
    
    @Column(name = "published_at")
    // @Field(type = FieldType.Date)
    private LocalDateTime publishedAt;
    
    @Column(name = "source_name", length = 255)
    // @Field(type = FieldType.Keyword)
    private String sourceName;
    
    @Column(name = "source_id", length = 255)
    // @Field(type = FieldType.Keyword)
    private String sourceId;
    
    @Column(name = "category_id")
    // @Field(type = FieldType.Long)
    private Long categoryId;
    
    // AI 분석 결과 필드들
    @Column(name = "sentiment_score", precision = 3, scale = 2)
    // @Field(type = FieldType.Float)
    private BigDecimal sentimentScore; // -1.0 ~ 1.0 (부정 ~ 긍정)
    
    @Column(name = "popularity_score", precision = 10, scale = 2)
    // @Field(type = FieldType.Float)
    @Builder.Default
    private BigDecimal popularityScore = BigDecimal.ZERO;
    
    @Column(name = "fact_check_score", precision = 3, scale = 2)
    // @Field(type = FieldType.Float)
    private BigDecimal factCheckScore; // 0.0 ~ 1.0 (낮음 ~ 높음)
    
    @Column(length = 10)
    // @Field(type = FieldType.Keyword)
    @Builder.Default
    private String language = "en";
    
    // @Field(type = FieldType.Keyword)
    private String[] keywords; // 추출된 키워드들
    
    // @Field(type = FieldType.Keyword)
    private String[] entities; // NER로 추출된 엔티티들
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    // @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    // @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
    
    // 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private NewsCategory category;
    
    // 비즈니스 메소드
    public boolean isPositiveSentiment() {
        return sentimentScore != null && sentimentScore.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isHighFactCheck() {
        return factCheckScore != null && factCheckScore.compareTo(new BigDecimal("0.7")) >= 0;
    }
    
    public boolean isPopular() {
        return popularityScore != null && popularityScore.compareTo(new BigDecimal("50.0")) >= 0;
    }
}