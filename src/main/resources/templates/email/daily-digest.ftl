<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jibmusil - ${digestType}</title>
    <style>
        body {
            font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
            line-height: 1.6;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 24px;
        }
        .content {
            padding: 20px;
        }
        .greeting {
            font-size: 16px;
            margin-bottom: 20px;
            color: #333;
        }
        .article {
            border-bottom: 1px solid #eee;
            padding: 15px 0;
            margin-bottom: 15px;
        }
        .article:last-child {
            border-bottom: none;
        }
        .article-title {
            font-size: 18px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 8px;
            text-decoration: none;
        }
        .article-title:hover {
            color: #667eea;
        }
        .article-description {
            color: #666;
            font-size: 14px;
            margin-bottom: 8px;
        }
        .article-meta {
            font-size: 12px;
            color: #999;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .sentiment-badge {
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: bold;
        }
        .sentiment-positive {
            background-color: #e8f5e8;
            color: #4caf50;
        }
        .sentiment-negative {
            background-color: #ffebee;
            color: #f44336;
        }
        .sentiment-neutral {
            background-color: #f5f5f5;
            color: #757575;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #eee;
        }
        .footer-links {
            margin-top: 10px;
        }
        .footer-links a {
            color: #667eea;
            text-decoration: none;
            margin: 0 10px;
            font-size: 14px;
        }
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 12px 24px;
            text-decoration: none;
            border-radius: 25px;
            margin: 20px 0;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📰 Jibmusil</h1>
            <p>${digestType} • ${currentDate}</p>
        </div>
        
        <div class="content">
            <div class="greeting">
                안녕하세요, ${user.firstName!''}${user.username}님! 👋<br>
                오늘의 주요 뉴스를 엄선해서 전해드립니다.
            </div>
            
            <#if articles?has_content>
                <#list articles as article>
                <div class="article">
                    <a href="${article.url}" class="article-title" target="_blank">
                        ${article.title}
                    </a>
                    <#if article.description??>
                    <div class="article-description">
                        ${article.description?truncate(150, "...")}
                    </div>
                    </#if>
                    <div class="article-meta">
                        <span>
                            <#if article.sourceName??>
                                ${article.sourceName} • 
                            </#if>
                            ${article.publishedAt?string("MM월 dd일 HH:mm")}
                        </span>
                        <#if article.sentimentScore??>
                            <#assign sentimentValue = article.sentimentScore?number>
                            <#if sentimentValue gt 0.1>
                                <span class="sentiment-badge sentiment-positive">긍정적</span>
                            <#elseif sentimentValue lt -0.1>
                                <span class="sentiment-badge sentiment-negative">부정적</span>
                            <#else>
                                <span class="sentiment-badge sentiment-neutral">중립적</span>
                            </#if>
                        </#if>
                    </div>
                </div>
                </#list>
            <#else>
                <p>오늘은 새로운 뉴스가 없습니다. 😴</p>
            </#if>
            
            <div style="text-align: center;">
                <a href="https://jibmusil.com/personalized" class="cta-button">
                    더 많은 맞춤 뉴스 보기
                </a>
            </div>
        </div>
        
        <div class="footer">
            <p style="margin: 0; color: #666; font-size: 14px;">
                이 이메일이 도움이 되셨나요? 피드백을 남겨주세요!
            </p>
            <div class="footer-links">
                <a href="https://jibmusil.com/unsubscribe?type=daily">구독 해지</a>
                <a href="https://jibmusil.com/preferences">설정 변경</a>
                <a href="https://jibmusil.com/contact">문의하기</a>
            </div>
            <p style="margin-top: 15px; color: #999; font-size: 12px;">
                © 2024 Jibmusil. All rights reserved.
            </p>
        </div>
    </div>
</body>
</html>