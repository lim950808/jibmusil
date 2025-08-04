package com.example.jibmusil.user;

import com.example.jibmusil.news.NewsArticle;
import com.example.jibmusil.news.NewsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NewsService newsService;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "userNews", key = "#username")
    public Mono<List<NewsArticle>> getPersonalizedNews(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        List<String> preferences = getUserPreferences(user);
        
        if (preferences.isEmpty()) {
            return Mono.just(new ArrayList<>());
        }
        
        // 선호 주제별 뉴스 병합 (창의: Flux.merge로 병렬 호출)
        return Flux.fromIterable(preferences)
                .flatMap(pref -> newsService.fetchNewsFromApi(pref, null, "en")
                    .map(response -> response.getArticles())
                    .flux()
                    .flatMap(Flux::fromIterable))
                .map(dto -> convertDtoToEntity(dto))
                .collectList();
    }

    // 현재 사용자 뉴스 가져오기 (SecurityContextHolder로 IoC 활용)
    public Mono<List<NewsArticle>> getCurrentUserNews() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getPersonalizedNews(auth.getName());
    }
    
    // 현재 사용자 ID 가져오기
    public Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElse(null);
    }
    
    // 사용자 선호도 파싱
    private List<String> getUserPreferences(User user) {
        if (user.getPreferences() == null) {
            return List.of("technology", "business"); // 기본 선호도
        }
        
        try {
            return objectMapper.readValue(user.getPreferences(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse user preferences for user {}: {}", user.getUsername(), e.getMessage());
            return List.of("technology", "business"); // 기본 선호도
        }
    }
    
    // DTO를 Entity로 변환하는 간단한 메소드
    private NewsArticle convertDtoToEntity(com.example.jibmusil.news.NewsArticleDto dto) {
        return NewsArticle.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .content(dto.getContent())
                .author(dto.getAuthor())
                .url(dto.getUrl())
                .urlToImage(dto.getUrlToImage())
                .sourceName(dto.getSource() != null ? dto.getSource().getName() : null)
                .sourceId(dto.getSource() != null ? dto.getSource().getId() : null)
                .build();
    }
}