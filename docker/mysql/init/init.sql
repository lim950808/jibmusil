-- 기본 데이터베이스 및 테이블 초기화 스크립트

CREATE DATABASE IF NOT EXISTS jibmusil_db;
USE jibmusil_db;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    preferences JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- 뉴스 카테고리 테이블
CREATE TABLE IF NOT EXISTS news_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 뉴스 기사 테이블
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    content LONGTEXT,
    author VARCHAR(255),
    url VARCHAR(1000) UNIQUE NOT NULL,
    url_to_image VARCHAR(1000),
    published_at TIMESTAMP,
    source_name VARCHAR(255),
    source_id VARCHAR(255),
    category_id BIGINT,
    sentiment_score DECIMAL(3,2),
    popularity_score DECIMAL(10,2) DEFAULT 0,
    fact_check_score DECIMAL(3,2),
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES news_categories(id),
    INDEX idx_published_at (published_at),
    INDEX idx_category (category_id),
    INDEX idx_sentiment (sentiment_score),
    INDEX idx_popularity (popularity_score)
);

-- 사용자 뉴스 상호작용 테이블 (클릭, 좋아요, 공유 등)
CREATE TABLE IF NOT EXISTS user_news_interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    news_article_id BIGINT NOT NULL,
    interaction_type ENUM('VIEW', 'CLICK', 'LIKE', 'SHARE', 'SAVE', 'DISLIKE') NOT NULL,
    interaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reading_time_seconds INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (news_article_id) REFERENCES news_articles(id) ON DELETE CASCADE,
    INDEX idx_user_interaction (user_id, interaction_type),
    INDEX idx_article_interaction (news_article_id, interaction_type),
    INDEX idx_interaction_time (interaction_time)
);

-- 사용자 개인화 프로필 테이블
CREATE TABLE IF NOT EXISTS user_preference_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    preference_score DECIMAL(5,4) DEFAULT 0.5000,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES news_categories(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_category (user_id, category_id)
);

-- 뉴스 트렌드 분석 테이블
CREATE TABLE IF NOT EXISTS news_trends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    trend_score DECIMAL(10,4) NOT NULL,
    mention_count INT DEFAULT 0,
    sentiment_average DECIMAL(3,2),
    time_window_start TIMESTAMP NOT NULL,
    time_window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_keyword (keyword),
    INDEX idx_trend_score (trend_score DESC),
    INDEX idx_time_window (time_window_start, time_window_end)
);

-- 이메일 구독 설정 테이블
CREATE TABLE IF NOT EXISTS email_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_type ENUM('DAILY_DIGEST', 'WEEKLY_SUMMARY', 'BREAKING_NEWS', 'TRENDING_TOPICS') NOT NULL,
    frequency ENUM('IMMEDIATE', 'HOURLY', 'DAILY', 'WEEKLY') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_sent TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_subscription (user_id, subscription_type)
);

-- 기본 카테고리 데이터 삽입
INSERT INTO news_categories (name, description) VALUES 
('Technology', 'Technology and innovation news'),
('Business', 'Business and economy news'),
('Politics', 'Political news and government updates'),
('Sports', 'Sports news and updates'),
('Entertainment', 'Entertainment and celebrity news'),
('Health', 'Health and medical news'),
('Science', 'Scientific discoveries and research'),
('World', 'International news and global events'),
('Opinion', 'Opinion pieces and editorials'),
('Lifestyle', 'Lifestyle and culture news');

-- 관리자 사용자 생성 (패스워드: admin123)
INSERT INTO users (username, email, password, first_name, last_name, preferences) VALUES 
('admin', 'admin@jibmusil.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Admin', 'User', '{"theme": "dark", "language": "ko"}');

-- 샘플 사용자 생성
INSERT INTO users (username, email, password, first_name, last_name, preferences) VALUES 
('testuser', 'test@jibmusil.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test', 'User', '{"theme": "light", "language": "ko"}');