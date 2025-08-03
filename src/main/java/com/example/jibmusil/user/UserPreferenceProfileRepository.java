package com.example.jibmusil.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceProfileRepository extends JpaRepository<UserPreferenceProfile, Long> {
    
    List<UserPreferenceProfile> findByUserId(Long userId);
    
    @Query("SELECT upp FROM UserPreferenceProfile upp WHERE upp.userId = :userId ORDER BY upp.preferenceScore DESC")
    List<UserPreferenceProfile> findByUserIdOrderByPreferenceScoreDesc(@Param("userId") Long userId);
    
    Optional<UserPreferenceProfile> findByUserIdAndCategoryId(Long userId, Long categoryId);
    
    @Query("SELECT upp FROM UserPreferenceProfile upp WHERE upp.userId = :userId AND upp.preferenceScore >= :minScore ORDER BY upp.preferenceScore DESC")
    List<UserPreferenceProfile> findHighPreferencesByUserId(@Param("userId") Long userId, @Param("minScore") BigDecimal minScore);
    
    @Query("SELECT upp FROM UserPreferenceProfile upp WHERE upp.categoryId = :categoryId ORDER BY upp.preferenceScore DESC")
    List<UserPreferenceProfile> findByCategoryIdOrderByPreferenceScoreDesc(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(upp) FROM UserPreferenceProfile upp WHERE upp.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(upp.preferenceScore) FROM UserPreferenceProfile upp WHERE upp.userId = :userId")
    BigDecimal getAveragePreferenceScoreByUserId(@Param("userId") Long userId);
    
    @Query("SELECT upp.categoryId, AVG(upp.preferenceScore) FROM UserPreferenceProfile upp GROUP BY upp.categoryId ORDER BY AVG(upp.preferenceScore) DESC")
    List<Object[]> getCategoryPopularityScores();
    
    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);
    
    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}