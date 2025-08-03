package com.example.jibmusil.user;

import com.example.jibmusil.news.NewsArticle;
import com.example.jibmusil.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NewsService newsService;

    @Cacheable(value = "userNews", key = "#username")
    public Mono<List<NewsArticle>> getPersonalizedNews(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        // 선호 주제별 뉴스 병합 (창의: Flux.merge로 병렬 호출)
        return Mono.zip(user.getPreferences().stream()
                                .map(pref -> newsService.fetchNews(pref, null))
                                .collect(Collectors.toList()),
                        results -> List.of(results))  // 합치기
                .map(merged -> merged.stream().flatMap(List::stream).toList());
    }

    // 현재 사용자 뉴스 가져오기 (SecurityContextHolder로 IoC 활용)
    public Mono<List<NewsArticle>> getCurrentUserNews() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getPersonalizedNews(auth.getName());
    }
}