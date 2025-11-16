#!/bin/bash

# Blue-Green 무중단 배포 스크립트
set -e

# 색상 출력용
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정 변수
PROJECT_NAME="mechuragi"
MAIN_SERVICE_PORT="8080"
GREEN_SERVICE_PORT="8081"
APP_DIRECTORY="/home/ubuntu/app"
NGINX_CONFIG="/etc/nginx/sites-available/main-service"

# 로그 함수
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Docker 네트워크 확인 및 생성
if ! docker network ls | grep -q app-network; then
    echo "app-network 생성 중..."
    docker network create app-network 2>/dev/null || true
fi

# 현재 활성 컨테이너 확인
get_active_container() {
    if docker ps -q -f name=${PROJECT_NAME}-main-blue | grep -q .; then
        echo "blue"
    elif docker ps -q -f name=${PROJECT_NAME}-main-green | grep -q .; then
        echo "green"
    else
        echo "none"
    fi
}

# 헬스체크 함수
health_check() {
    local port=$1
    local max_attempts=30
    local attempt=1

    log "헬스체크 시작 (포트: $port)"

    while [ $attempt -le $max_attempts ]; do
        if curl -f -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            success "헬스체크 성공 (시도: $attempt/$max_attempts)"
            return 0
        fi

        log "헬스체크 대기 중... ($attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done

    error "헬스체크 실패 - 애플리케이션이 정상적으로 시작되지 않았습니다"
}

# Nginx 설정 업데이트
update_nginx_config() {
    local active_port=$1
    local backup_config="${NGINX_CONFIG}.backup.$(date +%Y%m%d_%H%M%S)"

    # Nginx 설정 파일 존재 확인
    if [ ! -f "$NGINX_CONFIG" ]; then
        warn "Nginx 설정 파일을 찾을 수 없습니다: $NGINX_CONFIG"
        warn "가능한 설정 파일 위치를 확인하고 있습니다..."

        # 일반적인 Nginx 설정 파일 위치들 확인
        for potential_config in "/etc/nginx/nginx.conf" "/etc/nginx/sites-available/default" "/etc/nginx/sites-enabled/default"; do
            if [ -f "$potential_config" ]; then
                log "발견된 Nginx 설정 파일: $potential_config"
            fi
        done

        warn "Nginx 설정 업데이트를 건너뜁니다. 수동으로 설정을 확인해주세요."
        return 0
    fi

    log "Nginx 설정 백업: $backup_config"
    sudo cp $NGINX_CONFIG $backup_config

    log "Nginx 설정 업데이트 (활성 포트: $active_port)"
    # upstream backend 블록에서 server 포트 업데이트
    sudo sed -i "/upstream backend/,/^}/ s/server 127.0.0.1:[0-9]\+/server 127.0.0.1:$active_port/" $NGINX_CONFIG

    log "Nginx 설정 테스트"
    sudo nginx -t || error "Nginx 설정이 올바르지 않습니다"

    log "Nginx 리로드"
    sudo nginx -s reload || error "Nginx 리로드에 실패했습니다"

    success "Nginx 설정 업데이트 완료"
}

# 메인 배포 로직
main() {
    log "Blue-Green 무중단 배포 시작"

    cd $APP_DIRECTORY

    # 현재 활성 컨테이너 확인
    current_active=$(get_active_container)
    log "현재 활성 컨테이너: $current_active"

    # 새로 배포할 컨테이너 결정
    if [ "$current_active" = "blue" ] || [ "$current_active" = "none" ]; then
        new_active="green"
        new_port=$GREEN_SERVICE_PORT
        old_container="${PROJECT_NAME}-main-blue"
    else
        new_active="blue"
        new_port=$MAIN_SERVICE_PORT
        old_container="${PROJECT_NAME}-main-green"
    fi

    log "새로 배포할 컨테이너: $new_active (포트: $new_port)"

    # 새 컨테이너 시작
    log "새 컨테이너 시작: ${PROJECT_NAME}-main-$new_active"
    docker run -d \
        --name ${PROJECT_NAME}-main-$new_active \
        --network app-network \
        -p $new_port:8080 \
        -e SPRING_PROFILES_ACTIVE=production \
        -e SPRING_DATASOURCE_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME} \
        -e SPRING_DATASOURCE_USERNAME=${DB_USERNAME} \
        -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
        -e SPRING_REDIS_HOST=${REDIS_HOST} \
        -e SPRING_REDIS_PORT=${REDIS_PORT} \
        -e REDIS_HOST=${REDIS_HOST} \
        -e REDIS_PORT=${REDIS_PORT} \
        -e JWT_SECRET=${JWT_SECRET} \
        -e AWS_REGION=${AWS_REGION} \
        -e S3_BUCKET=${S3_BUCKET} \
        -e SES_FROM_EMAIL=${SES_FROM_EMAIL} \
        -e BEDROCK_AI_HOST=${BEDROCK_AI_HOST} \
        --restart unless-stopped \
        ${DOCKERHUB_USERNAME}/mechuragi-app:latest

    # 헬스체크
    health_check $new_port

    # Nginx 설정 업데이트 (트래픽 전환)
    update_nginx_config $new_port

    # 이전 컨테이너 중지 (존재하는 경우)
    if [ "$current_active" != "none" ]; then
        log "이전 컨테이너 중지: $old_container"
        docker stop $old_container || warn "이전 컨테이너 중지 실패 (이미 중지되었을 수 있음)"
        docker rm $old_container || warn "이전 컨테이너 제거 실패"
    fi

    # 사용하지 않는 이미지 정리
    log "사용하지 않는 Docker 이미지 정리"
    docker image prune -f

    success "Blue-Green 무중단 배포 완료!"
    log "활성 컨테이너: ${PROJECT_NAME}-main-$new_active (포트: $new_port)"
}

# 스크립트 실행
main "$@"