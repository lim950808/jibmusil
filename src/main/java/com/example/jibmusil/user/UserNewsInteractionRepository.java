package com.example.jibmusil.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserNewsInteractionRepository extends JpaRepository<UserNewsInteraction, Long> {
    
    List<UserNewsInteraction> findByUserId(Long userId);
    
    List<UserNewsInteraction> findByUserIdAndInteractionType(Long userId, UserNewsInteraction.InteractionType interactionType);
    
    List<UserNewsInteraction> findByNewsArticleId(Long newsArticleId);
    
    @Query("SELECT ui FROM UserNewsInteraction ui WHERE ui.userId = :userId AND ui.interactionType IN ('LIKE', 'SHARE', 'SAVE') ORDER BY ui.interactionTime DESC")
    List<UserNewsInteraction> findPositiveInteractionsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT ui FROM UserNewsInteraction ui WHERE ui.userId = :userId AND ui.interactionType IN ('LIKE', 'SHARE', 'SAVE') AND ui.interactionTime >= :fromDate ORDER BY ui.interactionTime DESC")
    List<UserNewsInteraction> findRecentPositiveInteractionsByUserId(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate, Pageable pageable);
    
    @Query("SELECT ui FROM UserNewsInteraction ui WHERE ui.userId = :userId ORDER BY ui.interactionTime DESC")
    List<UserNewsInteraction> findByUserIdOrderByInteractionTimeDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(ui) FROM UserNewsInteraction ui WHERE ui.newsArticleId = :articleId AND ui.interactionType = :interactionType")
    Long countByNewsArticleIdAndInteractionType(@Param("articleId") Long articleId, @Param("interactionType") UserNewsInteraction.InteractionType interactionType);
    
    @Query("SELECT COUNT(ui) FROM UserNewsInteraction ui WHERE ui.userId = :userId AND ui.interactionTime >= :fromDate")
    Long countUserInteractionsSince(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);
    
    @Query("SELECT ui.newsArticleId, COUNT(ui) as interactionCount FROM UserNewsInteraction ui WHERE ui.interactionType IN ('LIKE', 'SHARE', 'SAVE') GROUP BY ui.newsArticleId ORDER BY interactionCount DESC")
    List<Object[]> findMostInteractedArticles(Pageable pageable);
    
    @Query("SELECT ui FROM UserNewsInteraction ui WHERE ui.userId = :userId AND ui.newsArticleId = :articleId ORDER BY ui.interactionTime DESC")
    List<UserNewsInteraction> findByUserIdAndNewsArticleId(@Param("userId") Long userId, @Param("articleId") Long articleId);
    
    boolean existsByUserIdAndNewsArticleIdAndInteractionType(Long userId, Long newsArticleId, UserNewsInteraction.InteractionType interactionType);
}