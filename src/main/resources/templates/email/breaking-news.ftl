<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jibmusil - 속보</title>
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
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
            color: white;
            padding: 20px;
            text-align: center;
            position: relative;
        }
        .breaking-badge {
            background-color: #fff;
            color: #ff6b6b;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
            display: inline-block;
            margin-bottom: 10px;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.05); }
            100% { transform: scale(1); }
        }
        .header h1 {
            margin: 0;
            font-size: 24px;
        }
        .timestamp {
            font-size: 14px;
            opacity: 0.9;
            margin-top: 5px;
        }
        .content {
            padding: 25px;
        }
        .urgent-notice {
            background: linear-gradient(135deg, #ff9ff3 0%, #f368e0 100%);
            color: white;
            padding: 15px;
            border-radius: 10px;
            text-align: center;
            margin-bottom: 20px;
            font-weight: bold;
        }
        .article-container {
            border: 2px solid #ff6b6b;
            border-radius: 10px;
            padding: 20px;
            background-color: #fef7f7;
        }
        .article-title {
            font-size: 22px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 15px;
            line-height: 1.4;
        }
        .article-description {
            color: #444;
            font-size: 16px;
            margin-bottom: 15px;
            line-height: 1.6;
        }
        .article-meta {
            font-size: 14px;
            color: #666;
            margin-bottom: 20px;
            padding-top: 15px;
            border-top: 1px solid #eee;
        }
        .source-info {
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
        }
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
            color: white;
            padding: 15px 30px;
            text-decoration: none;
            border-radius: 25px;
            margin: 20px 0;
            font-weight: bold;
            font-size: 16px;
            text-align: center;
            display: block;
            max-width: 200px;
            margin: 20px auto;
        }
        .related-section {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 2px solid #f1f2f6;
        }
        .related-title {
            font-size: 16px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 15px;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #eee;
        }
        .disclaimer {
            font-size: 12px;
            color: #999;
            margin-top: 15px;
            line-height: 1.4;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="breaking-badge">⚡ 속보</div>
            <h1>📢 긴급 뉴스</h1>
            <div class="timestamp">${currentDate}</div>
        </div>
        
        <div class="content">
            <div class="urgent-notice">
                🚨 중요한 뉴스가 발생했습니다!
            </div>
            
            <div class="article-container">
                <div class="article-title">
                    ${article.title}
                </div>
                
                <#if article.description??>
                <div class="article-description">
                    ${article.description}
                </div>
                </#if>
                
                <div class="article-meta">
                    <div class="source-info">
                        <span>
                            <#if article.sourceName??>
                                📰 ${article.sourceName}
                            </#if>
                        </span>
                        <span>
                            🕐 ${article.publishedAt?string("HH:mm")}
                        </span>
                    </div>
                </div>
                
                <a href="${article.url}" class="cta-button" target="_blank">
                    전체 기사 읽기 →
                </a>
            </div>
            
            <div class="related-section">
                <div class="related-title">📲 실시간 업데이트</div>
                <p style="color: #666; font-size: 14px;">
                    이 뉴스에 대한 추가 정보와 실시간 업데이트를 받아보시려면 
                    Jibmusil 앱을 확인해주세요.
                </p>
                <a href="https://jibmusil.com/breaking" style="color: #ff6b6b; text-decoration: none; font-weight: bold;">
                    실시간 뉴스 센터 방문하기 →
                </a>
            </div>
        </div>
        
        <div class="footer">
            <p style="margin: 0; color: #666; font-size: 14px;">
                중요한 뉴스를 놓치지 않도록 즉시 알려드렸습니다.
            </p>
            <div style="margin-top: 15px;">
                <a href="https://jibmusil.com/unsubscribe?type=breaking" style="color: #667eea; text-decoration: none; margin: 0 10px; font-size: 12px;">속보 알림 해지</a>
                <a href="https://jibmusil.com/preferences" style="color: #667eea; text-decoration: none; margin: 0 10px; font-size: 12px;">알림 설정</a>
            </div>
            <div class="disclaimer">
                본 속보는 AI가 자동으로 감지하여 발송되었습니다. 
                정확한 정보는 원문 기사를 참조해주세요.
            </div>
        </div>
    </div>
</body>
</html>