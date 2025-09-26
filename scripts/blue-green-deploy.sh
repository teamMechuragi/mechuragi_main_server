#!/bin/bash

# Blue-Green 무중단 배포 스크립트
set -e

# 색상 출력용
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

# 현재 활성 컨테이너 확인
get_active_container() {
    if docker ps -q -f name=mechuragi-main-blue | grep -q .; then
        echo "blue"
    elif docker ps -q -f name=mechuragi-main-green | grep -q .; then
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

    log "헬스체크 시작: localhost:$port"

    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            success "헬스체크 성공 (attempt $attempt/$max_attempts)"
            return 0
        fi

        log "헬스체크 시도 $attempt/$max_attempts..."
        sleep 10
        attempt=$((attempt + 1))
    done

    error "헬스체크 실패: $max_attempts번 시도 후 실패"
    return 1
}

# Nginx 설정 전환
switch_nginx_config() {
    local target=$1
    local nginx_config="/etc/nginx/conf.d/default.conf"

    log "Nginx 설정을 $target으로 전환 중..."

    # Docker exec로 nginx 컨테이너 내부에서 설정 변경
    if [ "$target" == "green" ]; then
        docker exec mechuragi-nginx sed -i 's/server spring-app-blue:8080/# server spring-app-blue:8080/g' $nginx_config
        docker exec mechuragi-nginx sed -i 's/# server spring-app-green:8080/server spring-app-green:8080/g' $nginx_config
    else
        docker exec mechuragi-nginx sed -i 's/server spring-app-green:8080/# server spring-app-green:8080/g' $nginx_config
        docker exec mechuragi-nginx sed -i 's/# server spring-app-blue:8080/server spring-app-blue:8080/g' $nginx_config
    fi

    # Nginx 설정 리로드
    docker exec mechuragi-nginx nginx -s reload
    success "Nginx 설정이 $target으로 전환되었습니다"
}

# 메인 배포 로직
main() {
    log "=== Blue-Green 무중단 배포 시작 ==="

    # 현재 활성 컨테이너 확인
    current=$(get_active_container)
    log "현재 활성 컨테이너: $current"

    # 다음 배포 타겟 결정
    if [ "$current" == "blue" ] || [ "$current" == "none" ]; then
        target="green"
        target_port=8081
        old="blue"
    else
        target="blue"
        target_port=8080
        old="green"
    fi

    log "배포 타겟: $target (포트: $target_port)"

    # 새 컨테이너 시작
    log "새 컨테이너 시작 중..."
    docker-compose -f docker-compose.blue-green.yml up -d spring-app-$target

    # 헬스체크
    if health_check $target_port; then
        # Nginx 트래픽 전환
        switch_nginx_config $target

        # 잠시 대기 (트래픽 전환 완료 대기)
        log "트래픽 전환 안정화 대기 중..."
        sleep 5

        # 이전 컨테이너 정리 (현재가 none이 아닌 경우)
        if [ "$current" != "none" ]; then
            log "이전 컨테이너($old) 정리 중..."
            docker-compose -f docker-compose.blue-green.yml stop spring-app-$old
            docker-compose -f docker-compose.blue-green.yml rm -f spring-app-$old
            success "이전 컨테이너($old) 정리 완료"
        fi

        # 사용하지 않는 이미지 정리
        log "사용하지 않는 Docker 이미지 정리 중..."
        docker image prune -f

        success "=== Blue-Green 배포 성공 ==="
        success "활성 서비스: $target (포트: $target_port)"

    else
        error "헬스체크 실패로 인한 배포 중단"
        log "새 컨테이너($target) 정리 중..."
        docker-compose -f docker-compose.blue-green.yml stop spring-app-$target
        docker-compose -f docker-compose.blue-green.yml rm -f spring-app-$target
        exit 1
    fi
}

# 스크립트 실행
main "$@"