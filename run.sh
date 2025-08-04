#!/bin/bash

# Jibmusil 뉴스 큐레이션 플랫폼 실행 스크립트

echo "🚀 Jibmusil 뉴스 큐레이션 플랫폼을 시작합니다..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
print_step() {
    echo -e "${BLUE}📍 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Docker 및 Docker Compose 설치 확인
check_prerequisites() {
    print_step "필수 프로그램 확인 중..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker가 설치되지 않았습니다. Docker를 먼저 설치해주세요."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose가 설치되지 않았습니다. Docker Compose를 먼저 설치해주세요."
        exit 1
    fi
    
    if ! command -v java &> /dev/null; then
        print_error "Java가 설치되지 않았습니다. Java 21을 먼저 설치해주세요."
        exit 1
    fi
    
    print_success "모든 필수 프로그램이 설치되어 있습니다."
}

# 인프라 서비스 시작
start_infrastructure() {
    print_step "인프라 서비스 시작 중..."
    
    # Docker Compose로 인프라 서비스들 시작
    docker-compose up -d mysql redis elasticsearch kibana prometheus grafana kafka zookeeper
    
    if [ $? -eq 0 ]; then
        print_success "인프라 서비스가 성공적으로 시작되었습니다."
    else
        print_error "인프라 서비스 시작에 실패했습니다."
        exit 1
    fi
    
    print_step "서비스들이 준비될 때까지 대기 중..."
    sleep 30
}

# 애플리케이션 빌드
build_application() {
    print_step "Spring Boot 애플리케이션 빌드 중..."
    
    ./gradlew clean build -x test
    
    if [ $? -eq 0 ]; then
        print_success "애플리케이션 빌드가 완료되었습니다."
    else
        print_error "애플리케이션 빌드에 실패했습니다."
        exit 1
    fi
}

# 애플리케이션 시작
start_application() {
    print_step "Spring Boot 애플리케이션 시작 중..."
    
    export SPRING_PROFILES_ACTIVE=local
    export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/jibmusil_db?useSSL=false&serverTimezone=UTC
    export SPRING_DATASOURCE_USERNAME=root
    export SPRING_DATASOURCE_PASSWORD=password
    export SPRING_DATA_REDIS_HOST=localhost
    export SPRING_DATA_REDIS_PORT=6379
    export ELASTICSEARCH_HOST=localhost
    export ELASTICSEARCH_PORT=9200
    export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
    
    java -jar build/libs/jibmusil-0.0.1-SNAPSHOT.jar
}

# 헬스 체크
health_check() {
    print_step "서비스 상태 확인 중..."
    
    # 애플리케이션 헬스 체크
    for i in {1..30}; do
        if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
            print_success "애플리케이션이 정상적으로 실행되고 있습니다."
            break
        else
            echo "대기 중... ($i/30)"
            sleep 2
        fi
    done
}

# 서비스 정보 출력
print_service_info() {
    echo -e "\n${GREEN}🎉 Jibmusil 플랫폼이 성공적으로 시작되었습니다!${NC}\n"
    echo -e "${BLUE}📊 서비스 접속 정보:${NC}"
    echo "  • 메인 애플리케이션: http://localhost:8080"
    echo "  • API 문서 (Swagger): http://localhost:8080/swagger-ui.html"
    echo "  • 대시보드: http://localhost:8080/dashboard.html"
    echo "  • 액추에이터: http://localhost:8080/actuator"
    echo ""
    echo -e "${BLUE}🛠️  관리 도구:${NC}"
    echo "  • Grafana (모니터링): http://localhost:3000 (admin/admin)"
    echo "  • Kibana (로그 분석): http://localhost:5601"
    echo "  • Prometheus: http://localhost:9090"
    echo "  • Airflow: http://localhost:8080"
    echo ""
    echo -e "${YELLOW}🔧 유용한 명령어:${NC}"
    echo "  • 로그 확인: docker-compose logs -f jibmusil-app"
    echo "  • 서비스 중지: docker-compose down"
    echo "  • 데이터베이스 접속: docker exec -it jibmusil-mysql mysql -u root -p"
    echo "  • 로컬 DB 접속: mysql -h localhost -P 3307 -u root -p"
    echo ""
}

# 메인 실행 흐름
main() {
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════╗"
    echo "║                    JIBMUSIL                          ║"
    echo "║          뉴스 큐레이션 & 트렌드 분석 플랫폼              ║"
    echo "╚══════════════════════════════════════════════════════╝"
    echo -e "${NC}\n"
    
    check_prerequisites
    start_infrastructure
    build_application
    
    print_step "백그라운드에서 애플리케이션을 시작합니다..."
    start_application &
    APP_PID=$!
    
    health_check
    print_service_info
    
    echo -e "${YELLOW}애플리케이션을 중지하려면 Ctrl+C를 누르세요.${NC}"
    
    # 신호 핸들러 설정
    trap "echo -e '\n${YELLOW}애플리케이션을 종료합니다...${NC}'; kill $APP_PID; docker-compose down; exit 0" INT TERM
    
    # 애플리케이션이 실행되는 동안 대기
    wait $APP_PID
}

# 스크립트 실행
main "$@"