package com.example.jibmusil.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<NewsArticle, Long> {
    
    boolean existsByUrl(String url);
    
    Page<NewsArticle> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    Page<NewsArticle> findByCategoryIdOrderByPublishedAtDesc(Long categoryId, Pageable pageable);
    
    @Query("SELECT n FROM NewsArticle n WHERE n.categoryId = :categoryId ORDER BY n.popularityScore DESC, n.publishedAt DESC")
    List<NewsArticle> findByCategoryIdOrderByPopularityAndDate(@Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT n FROM NewsArticle n ORDER BY n.popularityScore DESC, n.publishedAt DESC")
    List<NewsArticle> findTrendingNews(Pageable pageable);
    
    default List<NewsArticle> findTrendingNews(int limit) {
        return findTrendingNews(org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    @Query("SELECT n FROM NewsArticle n WHERE " +
           "(:sentiment = 'positive' AND n.sentimentScore > :threshold) OR " +
           "(:sentiment = 'negative' AND n.sentimentScore < :threshold) OR " +
           "(:sentiment = 'neutral' AND n.sentimentScore BETWEEN -0.1 AND 0.1) " +
           "ORDER BY n.publishedAt DESC")
    List<NewsArticle> findBySentimentScore(@Param("threshold") BigDecimal threshold, Pageable pageable);
    
    default List<NewsArticle> findBySentimentScore(BigDecimal threshold, int limit) {
        return findBySentimentScore(threshold, org.springframework.data.domain.PageRequest.of(0, limit));
    }
    
    @Query("SELECT n FROM NewsArticle n WHERE n.language = :language ORDER BY n.publishedAt DESC")
    List<NewsArticle> findByLanguage(@Param("language") String language, Pageable pageable);
    
    @Query("SELECT n FROM NewsArticle n WHERE n.publishedAt >= :fromDate ORDER BY n.publishedAt DESC")
    List<NewsArticle> findRecentNews(@Param("fromDate") java.time.LocalDateTime fromDate, Pageable pageable);
    
    @Modifying
    @Query("UPDATE NewsArticle n SET n.popularityScore = n.popularityScore + 1 WHERE n.id = :articleId")
    void incrementPopularityScore(@Param("articleId") Long articleId);
    
    @Query("SELECT DISTINCT n.sourceName FROM NewsArticle n WHERE n.sourceName IS NOT NULL ORDER BY n.sourceName")
    List<String> findAllSources();
    
    @Query("SELECT COUNT(n) FROM NewsArticle n WHERE n.categoryId = :categoryId")
    Long countByCategory(@Param("categoryId") Long categoryId);
    
    @Query("SELECT n FROM NewsArticle n WHERE n.factCheckScore >= :minScore ORDER BY n.factCheckScore DESC, n.publishedAt DESC")
    List<NewsArticle> findHighFactCheckNews(@Param("minScore") BigDecimal minScore, Pageable pageable);
}