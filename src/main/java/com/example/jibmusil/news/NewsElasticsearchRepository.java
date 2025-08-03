package com.example.jibmusil.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsElasticsearchRepository extends ElasticsearchRepository<NewsArticle, Long> {
    
    Page<NewsArticle> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^2\", \"description\", \"content\"]}}]}}")
    Page<NewsArticle> findByMultiMatch(String query, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"publishedAt\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]}}")
    Page<NewsArticle> findByPublishedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"term\": {\"categoryId\": \"?0\"}}]}}")
    Page<NewsArticle> findByCategoryId(Long categoryId, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"sentimentScore\": {\"gte\": \"?0\"}}}]}}")
    Page<NewsArticle> findBySentimentScoreGreaterThan(Double score, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"popularityScore\": {\"gte\": \"?0\"}}}]}}")
    List<NewsArticle> findByPopularityScoreGreaterThan(Double score);
    
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"keywords\": [\"?0\"]}}]}}")
    Page<NewsArticle> findByKeywordsContaining(String keyword, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"term\": {\"language\": \"?0\"}}]}}")
    Page<NewsArticle> findByLanguage(String language, Pageable pageable);
    
    @Query("{\"aggregations\": {\"trending_keywords\": {\"terms\": {\"field\": \"keywords\", \"size\": \"?0\"}}}}")
    List<String> findTrendingKeywords(int size);
}