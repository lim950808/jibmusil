package com.example.jibmusil.news;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NewsArticleDto {
    
    private String title;
    private String description;
    private String content;
    private String author;
    private String url;
    
    @JsonProperty("urlToImage")
    private String urlToImage;
    
    @JsonProperty("publishedAt")
    private String publishedAt;
    
    private NewsSourceDto source;
    
    @Data
    public static class NewsSourceDto {
        private String id;
        private String name;
    }
}