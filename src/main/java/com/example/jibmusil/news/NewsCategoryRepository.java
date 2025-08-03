package com.example.jibmusil.news;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {
    
    Optional<NewsCategory> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM NewsCategory c ORDER BY c.name")
    List<NewsCategory> findAllOrderByName();
    
    @Query("SELECT c.name FROM NewsCategory c ORDER BY c.name")
    List<String> findAllCategoryNames();
}