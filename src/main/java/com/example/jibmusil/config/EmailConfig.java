package com.example.jibmusil.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@org.springframework.context.annotation.Configuration
@EnableAsync
@EnableScheduling
public class EmailConfig {
    
    @Bean
    @Primary
    public Configuration freemarkerConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);
        
        // 템플릿 로더 설정
        configuration.setClassForTemplateLoading(this.getClass(), "/templates/email/");
        
        // 기본 인코딩 설정
        configuration.setDefaultEncoding("UTF-8");
        
        // 템플릿 예외 핸들러 설정
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
        // 로그 템플릿 예외 설정
        configuration.setLogTemplateExceptions(false);
        
        // 래핑되지 않은 예외 설정
        configuration.setWrapUncheckedExceptions(true);
        
        // SQL 날짜 및 시간 타입 설정
        configuration.setSQLDateAndTimeTimeZone(java.util.TimeZone.getDefault());
        
        return configuration;
    }
}