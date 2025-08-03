package com.example.jibmusil.news;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewsApiResponse {
    
    private String status;
    
    @JsonProperty("totalResults")
    private Integer totalResults;
    
    private List<NewsArticleDto> articles;
}