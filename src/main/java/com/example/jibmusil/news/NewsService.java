package com.example.jibmusil.news;

import com.example.jibmusil.analytics.SentimentAnalysisService;
import com.example.jibmusil.kafka.NewsKafkaProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
// import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
// import org.springframework.data.elasticsearch.core.SearchHit;
// import org.springframework.data.elasticsearch.core.SearchHits;
// import org.springframework.data.elasticsearch.core.query.Query;
// import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    private final NewsRepository newsRepository;
    // private final NewsElasticsearchRepository newsElasticsearchRepository;
    private final NewsCategoryRepository categoryRepository;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final NewsKafkaProducer kafkaProducer;
    // private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @Value("${newsapi.base-url}")
    private String newsApiBaseUrl;

    @Value("${newsapi.key}")
    private String newsApiKey;

    @Value("${jibmusil.news.batch-size:100}")
    private int batchSize;

    @Cacheable(value = "news", key = "#query + ':' + #category + ':' + #language")
    public Mono<NewsApiResponse> fetchNewsFromApi(String query, String category, String language) {
        log.info("Fetching news from API: query={}, category={}, language={}", query, category, language);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("newsapi.org")
                        .path("/v2/everything")
                        .queryParam("q", query)
                        .queryParam("category", category)
                        .queryParam("language", language)
                        .queryParam("sortBy", "publishedAt")
                        .queryParam("pageSize", batchSize)
                        .queryParam("apiKey", newsApiKey)
                        .build())
                .retrieve()
                .bodyToMono(NewsApiResponse.class)
                .doOnSuccess(response -> log.info("Successfully fetched {} articles from NewsAPI", 
                    response != null ? response.getTotalResults() : 0))
                .doOnError(error -> log.error("Error fetching news from API", error));
    }

    @Transactional
    @Async
    public CompletableFuture<Void> processAndSaveNews(String query, String category, String language) {
        return fetchNewsFromApi(query, category, language)
                .flatMapMany(response -> Flux.fromIterable(response.getArticles()))
                .filter(this::isValidArticle)
                .map(this::convertToEntity)
                .flatMap(this::enrichWithAiAnalysis)
                .collectList()
                .doOnNext(this::saveArticlesBatch)
                .doOnNext(articles -> articles.forEach(kafkaProducer::sendNewsProcessedEvent))
                .doOnSuccess(articles -> log.info("Processed and saved {} articles", articles.size()))
                .doOnError(error -> log.error("Error processing news", error))
                .then()
                .toFuture();
    }

    private boolean isValidArticle(NewsArticleDto dto) {
        return dto.getTitle() != null && 
               dto.getUrl() != null && 
               !dto.getTitle().isEmpty() && 
               !dto.getUrl().isEmpty() &&
               !newsRepository.existsByUrl(dto.getUrl());
    }

    private NewsArticle convertToEntity(NewsArticleDto dto) {
        NewsCategory category = findOrCreateCategory(extractCategory(dto));
        
        return NewsArticle.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .content(dto.getContent())
                .author(dto.getAuthor())
                .url(dto.getUrl())
                .urlToImage(dto.getUrlToImage())
                .publishedAt(parsePublishedDate(dto.getPublishedAt()))
                .sourceName(dto.getSource() != null ? dto.getSource().getName() : null)
                .sourceId(dto.getSource() != null ? dto.getSource().getId() : null)
                .categoryId(category.getId())
                .language(detectLanguage(dto.getTitle(), dto.getDescription()))
                .build();
    }

    private Mono<NewsArticle> enrichWithAiAnalysis(NewsArticle article) {
        return sentimentAnalysisService.analyzeSentiment(article.getTitle() + " " + article.getDescription())
                .map(sentiment -> {
                    article.setSentimentScore(sentiment);
                    article.setPopularityScore(calculatePopularityScore(article));
                    article.setFactCheckScore(calculateFactCheckScore(article));
                    article.setKeywords(extractKeywords(article));
                    article.setEntities(extractEntities(article));
                    return article;
                });
    }

    @Transactional
    public void saveArticlesBatch(List<NewsArticle> articles) {
        try {
            List<NewsArticle> savedArticles = newsRepository.saveAll(articles);
            // newsElasticsearchRepository.saveAll(savedArticles);
            log.info("Saved batch of {} articles to database and Elasticsearch", articles.size());
        } catch (Exception e) {
            log.error("Error saving articles batch", e);
            throw e;
        }
    }

    public Page<NewsArticle> searchNews(String query, String category, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return findNewsByCategory(category, pageable);
        }

        // JPA Repository를 사용한 검색 (Elasticsearch 대신)
        if (category != null) {
            Long categoryId = getCategoryId(category);
            return newsRepository.findByCategoryIdOrderByPublishedAtDesc(categoryId, pageable);
        } else {
            return newsRepository.findByTitleContainingOrDescriptionContaining(query, query, pageable);
        }
    }

    public Page<NewsArticle> findNewsByCategory(String category, Pageable pageable) {
        if (category != null) {
            Long categoryId = getCategoryId(category);
            return newsRepository.findByCategoryIdOrderByPublishedAtDesc(categoryId, pageable);
        }
        return newsRepository.findAllByOrderByPublishedAtDesc(pageable);
    }

    public List<NewsArticle> findTrendingNews(int limit) {
        return newsRepository.findTrendingNews(limit);
    }

    public List<NewsArticle> findNewsBySentiment(String sentiment, int limit) {
        BigDecimal threshold = switch (sentiment.toLowerCase()) {
            case "positive" -> new BigDecimal("0.1");
            case "negative" -> new BigDecimal("-0.1");
            default -> BigDecimal.ZERO;
        };

        return newsRepository.findBySentimentScore(threshold, limit);
    }

    public Optional<NewsArticle> findNewsById(Long id) {
        return newsRepository.findById(id);
    }

    @Transactional
    public void incrementPopularity(Long articleId) {
        newsRepository.incrementPopularityScore(articleId);
        kafkaProducer.sendNewsViewEvent(articleId);
    }

    private NewsCategory findOrCreateCategory(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    NewsCategory newCategory = NewsCategory.builder()
                            .name(categoryName)
                            .description("Auto-generated category for " + categoryName)
                            .build();
                    return categoryRepository.save(newCategory);
                });
    }

    private String extractCategory(NewsArticleDto dto) {
        String content = (dto.getTitle() + " " + dto.getDescription()).toLowerCase();
        
        if (content.contains("technology") || content.contains("tech") || content.contains("ai")) {
            return "Technology";
        } else if (content.contains("business") || content.contains("economy") || content.contains("finance")) {
            return "Business";
        } else if (content.contains("politics") || content.contains("government") || content.contains("election")) {
            return "Politics";
        } else if (content.contains("sports") || content.contains("football") || content.contains("basketball")) {
            return "Sports";
        } else if (content.contains("health") || content.contains("medical") || content.contains("covid")) {
            return "Health";
        } else {
            return "General";
        }
    }

    private LocalDateTime parsePublishedDate(String publishedAt) {
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse published date: {}, using current time", publishedAt);
            return LocalDateTime.now();
        }
    }

    private String detectLanguage(String title, String description) {
        String text = (title + " " + description).toLowerCase();
        
        if (text.matches(".*[가-힣].*")) {
            return "ko";
        } else if (text.matches(".*[ひらがなカタカナ].*")) {
            return "ja";
        } else if (text.matches(".*[\\u4e00-\\u9fff].*")) {
            return "zh";
        } else {
            return "en";
        }
    }

    private BigDecimal calculatePopularityScore(NewsArticle article) {
        BigDecimal score = BigDecimal.ZERO;
        
        if (article.getTitle() != null && article.getTitle().length() > 50) {
            score = score.add(new BigDecimal("10"));
        }
        
        if (article.getUrlToImage() != null) {
            score = score.add(new BigDecimal("5"));
        }
        
        if (article.getAuthor() != null) {
            score = score.add(new BigDecimal("5"));
        }
        
        return score;
    }

    private BigDecimal calculateFactCheckScore(NewsArticle article) {
        BigDecimal score = new BigDecimal("0.5");
        
        String content = (article.getTitle() + " " + article.getDescription()).toLowerCase();
        
        String[] reliableIndicators = {"study", "research", "university", "official", "confirmed"};
        String[] unreliableIndicators = {"rumor", "allegedly", "unconfirmed", "breaking"};
        
        for (String indicator : reliableIndicators) {
            if (content.contains(indicator)) {
                score = score.add(new BigDecimal("0.1"));
            }
        }
        
        for (String indicator : unreliableIndicators) {
            if (content.contains(indicator)) {
                score = score.subtract(new BigDecimal("0.1"));
            }
        }
        
        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private String[] extractKeywords(NewsArticle article) {
        String text = (article.getTitle() + " " + article.getDescription()).toLowerCase();
        
        List<String> keywords = new ArrayList<>();
        String[] commonKeywords = {"ai", "technology", "business", "politics", "health", "sports", "economy"};
        
        for (String keyword : commonKeywords) {
            if (text.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        
        return keywords.toArray(new String[0]);
    }

    private String[] extractEntities(NewsArticle article) {
        String text = article.getTitle() + " " + article.getDescription();
        
        List<String> entities = new ArrayList<>();
        
        if (text.matches(".*\\b[A-Z][a-z]+ [A-Z][a-z]+\\b.*")) {
            entities.add("PERSON");
        }
        
        if (text.matches(".*\\b(Inc|Corp|Ltd|Company)\\b.*")) {
            entities.add("ORGANIZATION");
        }
        
        return entities.toArray(new String[0]);
    }

    private Long getCategoryId(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .map(NewsCategory::getId)
                .orElse(null);
    }
    
    // UserService에서 호출하는 메소드 추가
    public Mono<NewsApiResponse> fetchNews(String query, String category) {
        return fetchNewsFromApi(query, category, "en");
    }
}