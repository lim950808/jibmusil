package com.example.jibmusil.news;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news")
    public Mono<List<NewsArticle>> getNews(@RequestParam String query, @RequestParam(required = false) String category) {
        return newsService.fetchNews(query, category)
                .map(response -> response.getArticles().stream()
                        .map(this::convertDtoToEntity)
                        .toList());
    }
    
    private NewsArticle convertDtoToEntity(NewsArticleDto dto) {
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