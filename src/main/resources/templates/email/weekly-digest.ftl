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
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
            color: white;
            padding: 25px;
            text-align: center;
        }
        .header h1 {
            margin: 0;
            font-size: 28px;
        }
        .week-range {
            margin-top: 5px;
            font-size: 14px;
            opacity: 0.9;
        }
        .content {
            padding: 25px;
        }
        .greeting {
            font-size: 16px;
            margin-bottom: 25px;
            color: #333;
            text-align: center;
        }
        .section {
            margin-bottom: 30px;
        }
        .section-title {
            font-size: 20px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 15px;
            padding-bottom: 8px;
            border-bottom: 2px solid #4facfe;
        }
        .article {
            border-left: 3px solid #4facfe;
            padding: 15px;
            margin-bottom: 15px;
            background-color: #f8f9fa;
            border-radius: 0 8px 8px 0;
        }
        .article-title {
            font-size: 16px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 8px;
            text-decoration: none;
            display: block;
        }
        .article-title:hover {
            color: #4facfe;
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
        .stats-section {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 10px;
            margin: 20px 0;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
            gap: 15px;
            margin-top: 15px;
        }
        .stat-item {
            text-align: center;
        }
        .stat-number {
            font-size: 24px;
            font-weight: bold;
            display: block;
        }
        .stat-label {
            font-size: 12px;
            opacity: 0.8;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            border-top: 1px solid #eee;
        }
        .cta-button {
            display: inline-block;
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
            color: white;
            padding: 15px 30px;
            text-decoration: none;
            border-radius: 25px;
            margin: 20px 0;
            font-weight: bold;
            font-size: 16px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 주간 뉴스 요약</h1>
            <div class="week-range">${weekRange!'지난 주'}</div>
            <p>${currentDate}</p>
        </div>
        
        <div class="content">
            <div class="greeting">
                ${user.firstName!''}${user.username}님, 한 주간 수고하셨습니다! 🎉<br>
                지난 주의 주요 뉴스를 정리해서 전해드립니다.
            </div>
            
            <div class="stats-section">
                <h3 style="margin-top: 0;">이번 주 뉴스 통계</h3>
                <div class="stats-grid">
                    <div class="stat-item">
                        <span class="stat-number">${articles?size}</span>
                        <span class="stat-label">주요 뉴스</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-number">${(articles?size * 0.3)?round}</span>
                        <span class="stat-label">긍정적 뉴스</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-number">7</span>
                        <span class="stat-label">카테고리</span>
                    </div>
                </div>
            </div>
            
            <#if articles?has_content>
                <div class="section">
                    <div class="section-title">🔥 이번 주 주요 뉴스</div>
                    <#list articles as article>
                    <div class="article">
                        <a href="${article.url}" class="article-title" target="_blank">
                            ${article.title}
                        </a>
                        <#if article.description??>
                        <div class="article-description">
                            ${article.description?truncate(200, "...")}
                        </div>
                        </#if>
                        <div class="article-meta">
                            <span>
                                <#if article.sourceName??>
                                    ${article.sourceName} • 
                                </#if>
                                ${article.publishedAt?string("MM월 dd일")}
                            </span>
                            <#if article.popularityScore??>
                                <span>인기도: ${article.popularityScore?round}</span>
                            </#if>
                        </div>
                    </div>
                    </#list>
                </div>
            <#else>
                <p>이번 주는 주요 뉴스가 없었습니다. 😴</p>
            </#if>
            
            <div style="text-align: center;">
                <a href="https://jibmusil.com/weekly-analysis" class="cta-button">
                    상세한 주간 분석 보기
                </a>
            </div>
        </div>
        
        <div class="footer">
            <p style="margin: 0; color: #666; font-size: 14px;">
                다음 주도 알찬 뉴스로 찾아뵙겠습니다! 💪
            </p>
            <div style="margin-top: 15px;">
                <a href="https://jibmusil.com/unsubscribe?type=weekly" style="color: #667eea; text-decoration: none; margin: 0 10px;">구독 해지</a>
                <a href="https://jibmusil.com/preferences" style="color: #667eea; text-decoration: none; margin: 0 10px;">설정 변경</a>
            </div>
        </div>
    </div>
</body>
</html>