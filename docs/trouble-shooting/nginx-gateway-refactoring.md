# OpenResty Gateway 리팩토링 전후 비교

> 대상 파일: `mechuragi_infra/ansible/playbooks/nginx-gateway.yml`
> `mechuragi_infra/ansible/templates/nginx.conf.j2`

---

## 개요

기존 버전에서 실제 동작하지 않는 문제가 있어 전면 수정.

**핵심 문제**
- `lua-resty-jwt` 의존 라이브러리(`lua-resty-hmac`) 설치 누락 → JWT 검증 런타임 에러
- 모니터링(CloudWatch Agent) 미포함
- `vars_files` 미참조로 변수 관리 분리 안 됨
- `nginx.conf.j2` 내 JWT 검증 로직 미완성

---

## 1. nginx-gateway.yml 변경 사항

### 1-1. vars_files 추가

**Before**
```yaml
- name: Setup Nginx Gateway (OpenResty + NAT + JWT Auth)
  hosts: nginx_gateway
  become: yes
  tasks:
```

**After**
```yaml
- name: Setup Nginx Gateway (OpenResty + NAT + JWT Auth)
  hosts: nginx_gateway
  become: yes
  vars_files:
    - ../group_vars/all/main.yml
  tasks:
```

`jwt_secret`, `project_name`, `main_service_private_ip` 등 민감 변수들을 `group_vars/all/main.yml`에서 중앙 관리하기 위해 추가. 원본에서는 변수 참조 경로가 없어 템플릿 렌더링 시 변수 미정의 오류 발생 가능.

---

### 1-2. lua-resty-hmac 의존성 설치 추가 (핵심 버그 수정)

**Before**
```yaml
- name: Clone lua-resty-jwt repository
  git:
    repo: 'https://github.com/SkyLothar/lua-resty-jwt.git'
    dest: /tmp/lua-resty-jwt
    version: master
    force: yes

- name: Install lua-resty-jwt to OpenResty
  shell: |
    cd /tmp/lua-resty-jwt
    cp -r lib/resty/* /usr/local/openresty/lualib/resty/
  args:
    creates: /usr/local/openresty/lualib/resty/jwt.lua
```

**After**
```yaml
# [추가] lua-resty-hmac 먼저 설치 (lua-resty-jwt의 내부 의존성)
- name: Clone lua-resty-hmac repository (jwt 의존성)
  git:
    repo: 'https://github.com/jkeys089/lua-resty-hmac.git'
    dest: /tmp/lua-resty-hmac
    version: master
    force: yes

- name: Install lua-resty-hmac to OpenResty
  shell: |
    cp /tmp/lua-resty-hmac/lib/resty/hmac.lua /usr/local/openresty/lualib/resty/
  args:
    creates: /usr/local/openresty/lualib/resty/hmac.lua

- name: Clone lua-resty-jwt repository
  git:
    repo: 'https://github.com/SkyLothar/lua-resty-jwt.git'
    dest: /tmp/lua-resty-jwt
    version: master
    force: yes

- name: Install lua-resty-jwt to OpenResty
  shell: |
    cd /tmp/lua-resty-jwt
    cp -r lib/resty/* /usr/local/openresty/lualib/resty/
  args:
    creates: /usr/local/openresty/lualib/resty/jwt.lua
```

`lua-resty-jwt`는 내부에서 `require("resty.hmac")`를 호출함. 원본에서는 jwt만 설치하고 hmac은 설치하지 않아 `/recommend/food` 요청 시 아래 런타임 에러 발생:

```
[error] ...resty/jwt.lua:xx: module 'resty.hmac' not found
```

`lua-resty-hmac`를 먼저 설치하는 순서로 수정하여 해결.

---

### 1-3. CloudWatch Agent 섹션 전체 추가

**Before**
해당 섹션 없음. Nginx 로그가 서버 로컬에만 존재하여 모니터링 불가.

**After**
```yaml
- name: Download CloudWatch Agent
  get_url:
    url: https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
    dest: /tmp/amazon-cloudwatch-agent.deb

- name: Install CloudWatch Agent
  apt:
    deb: /tmp/amazon-cloudwatch-agent.deb

- name: Deploy CloudWatch Agent configuration
  template:
    src: ../templates/cloudwatch-agent-config.json.j2
    dest: /opt/aws/amazon-cloudwatch-agent/etc/config.json

- name: Start CloudWatch Agent
  command: >
    /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl
    -a fetch-config -m ec2 -s
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json

- name: Enable CloudWatch Agent service
  systemd:
    name: amazon-cloudwatch-agent
    enabled: yes
    state: started
```

OpenResty access/error 로그를 CloudWatch Logs로 실시간 전송하기 위해 추가. Terraform `cloudwatch-logs` 모듈에서 생성한 로그 그룹과 연동하여 **로컬 파일(logrotate) → CloudWatch Agent(실시간) → S3(야간 아카이빙)** 3계층 로그 구조 완성.

---

### 1-4. 유지된 항목

| 항목 | 내용 |
|------|------|
| OpenResty 설치 | apt 저장소 추가 → 패키지 설치 |
| systemd 서비스 등록 | JWT_SECRET 환경변수 주입 포함 |
| NAT Instance 설정 | `ip_forward sysctl` + `iptables MASQUERADE` |
| logrotate 설정 | daily, rotate 30, compress |
| S3 로그 백업 cron | 매일 02:00, `s3-log-backup.sh.j2` 템플릿 |

---

## 2. nginx.conf.j2 변경 사항 (전면 재작성)

원본 `nginx.conf.j2`는 JWT 검증 로직이 없거나 미완성 상태. 인프라 구조(upstream IP, 포트, 경로) 변경에 맞게 전면 재작성.

---

### 2-1. JWT Secret 공유 메모리 캐싱 추가

**Before**
환경변수를 Lua 블록에서 매 요청마다 `os.getenv()`로 읽거나 미구현.

**After**
```nginx
lua_shared_dict env_secrets 128k;

init_by_lua_block {
    local secret = os.getenv("JWT_SECRET")
    if secret then
        ngx.shared.env_secrets:set("jwt_secret", secret)
    else
        ngx.log(ngx.ERR, "JWT_SECRET is missing in Environment!")
    end
}
```

`os.getenv()`는 worker process 간 공유가 안 되고 매 요청마다 호출 비용 발생. `lua_shared_dict`에 서버 시작 시 1회 로드하여 전 worker가 공유 메모리로 접근하도록 개선.

---

### 2-2. Blue-Green 배포용 upstream 변수화

**Before**
```nginx
upstream main_server {
    server <하드코딩 IP>:8080;
}
```

**After**
```nginx
upstream main_server {
    server {{ main_service_private_ip }}:{{ main_server_active_port }};
}

upstream ai_server {
    server {{ ai_service_private_ip }}:8082;
}
```

Blue-Green 배포 시 Ansible이 `main_server_active_port` 변수를 8080/8081로 전환하면 nginx.conf를 재렌더링하여 무중단 포트 전환 가능. IP도 Ansible `group_vars`에서 관리하여 환경 변경 시 코드 수정 불필요.

---

### 2-3. /recommend/food JWT 검증 로직 완전 구현

**Before**
JWT 검증 없이 AI 서버로 바로 프록시하거나 미완성 상태.

**After**
```nginx
location /recommend/food {
    access_by_lua_block {
        local jwt = require "resty.jwt"
        local cjson = require "cjson"

        -- Authorization 헤더 추출
        local auth_header = ngx.var.http_authorization
        if not auth_header then
            ngx.status = 401
            ngx.say(cjson.encode({error = "Missing Authorization header"}))
            return ngx.exit(401)
        end

        -- Bearer 토큰 파싱
        local token = auth_header:match("Bearer%s+(.+)")
        if not token then
            ngx.status = 401
            ngx.say(cjson.encode({error = "Invalid Authorization format"}))
            return ngx.exit(401)
        end

        -- 공유 메모리에서 JWT Secret 로드
        local jwt_secret = ngx.shared.env_secrets:get("jwt_secret")
        if not jwt_secret then
            ngx.status = 500
            ngx.say(cjson.encode({error = "Internal server error"}))
            return ngx.exit(500)
        end

        -- JWT 검증 및 처리 시간 측정
        local lua_auth_start = ngx.now()
        local jwt_obj = jwt:verify(jwt_secret, token)
        local lua_auth_ms = math.floor((ngx.now() - lua_auth_start) * 1000)

        if not jwt_obj.verified then
            ngx.status = 401
            ngx.say(cjson.encode({error = "Invalid or expired token"}))
            return ngx.exit(401)
        end

        ngx.log(ngx.WARN, "[성능] Nginx Lua 인증 처리: ", lua_auth_ms, "ms")
    }

    proxy_buffering off;
    proxy_cache off;
    chunked_transfer_encoding on;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    proxy_pass http://ai_server;
    proxy_read_timeout 300s;
}
```

- Spring Security를 거치지 않고 Nginx Lua에서 직접 JWT 검증
- 검증 실패 시 AI 서버에 요청 자체가 도달하지 않아 불필요한 트래픽 차단
- `ngx.now()` 기반 인증 처리 시간 측정 로그 → k6 부하 테스트에서 Lua 오버헤드 측정 근거로 활용
- SSE 스트리밍을 위한 `proxy_buffering off`, 300초 read timeout 적용

---

### 2-4. /api/* 경로 SSE 스트리밍 지원 추가

**Before**
일반 리버스 프록시 설정만 존재. SSE 연결 시 버퍼링으로 인해 실시간 전송 불가.

**After**
```nginx
location /api/ {
    proxy_buffering off;
    proxy_cache off;
    chunked_transfer_encoding on;
    proxy_set_header Connection '';  # SSE 연결 유지 (Upgrade 헤더 제거)
    proxy_http_version 1.1;
    proxy_read_timeout 300s;         # SSE heartbeat 주기 고려 (60초 기준 5배)
}
```

Spring Boot `SseEmitter` 응답이 Nginx 버퍼에 쌓이면 클라이언트에 즉시 전달되지 않음. `proxy_buffering off` + `Connection ''` 조합으로 청크 단위 즉시 전송 보장.

---

### 2-5. OAuth2 경로 HTTPS 헤더 포워딩 추가

**Before**
```nginx
location /oauth2/authorization/ {
    proxy_pass http://main_server;
}
```

**After**
```nginx
location /oauth2/authorization/ {
    proxy_pass http://main_server;
    proxy_set_header X-Forwarded-Proto https;  # 원래 프로토콜이 HTTPS임을 Spring에 전달
    proxy_set_header X-Forwarded-Port 443;     # 원래 포트가 443임을 Spring에 전달
    proxy_redirect off;
}

location = /oauth2/callback {
    return 301 https://{{ domain_name }}$request_uri;  # SPA 경로를 CloudFront로 리다이렉트
}
```

Nginx는 HTTP(80)로 수신하지만 CloudFront가 HTTPS(443)로 받은 요청을 포워딩한 것임. `X-Forwarded-Proto`를 `https`로 설정하지 않으면 Spring Security가 리다이렉트 URL을 `http://`로 생성하여 카카오 OAuth 콜백이 실패함.

---

### 2-6. /api/legacy/* 경로 추가 (성능 비교용)

**Before**
해당 경로 없음.

**After**
```nginx
location /api/legacy/ {
    proxy_pass http://main_server;
    proxy_set_header Authorization $http_authorization;
    proxy_read_timeout 300s;
}
```

k6 부하 테스트에서 두 경로의 성능을 비교하기 위해 추가.

| 경로 | 흐름 |
|------|------|
| **Direct** | Client → Nginx (Lua JWT) → AI Server |
| **Legacy** | Client → Nginx → Main Server (Spring Security) → AI Server |

이 비교를 통해 Direct Path가 avg 56%, p95 82% 응답시간 단축을 수치로 확인하고 채택.

---

## 3. 변경 요약

| 항목 | Before | After |
|------|--------------|-------------|
| vars_files | 없음 | `group_vars/all/main.yml` 참조 |
| lua-resty-hmac 설치 | 없음 (JWT 런타임 에러) | `lua-resty-jwt` 전에 먼저 설치 |
| CloudWatch Agent | 없음 | 설치 + 설정 + 시작 + 검증 전체 구현 |
| JWT Secret 관리 | 미구현 | `lua_shared_dict` 공유 메모리 캐싱 |
| `/recommend/food` JWT 검증 | 없음 또는 미완성 | Bearer 파싱 + 검증 + 401/500 처리 완전 구현 |
| upstream 설정 | 하드코딩 | Ansible 변수화 (Blue-Green 포트 전환) |
| SSE 스트리밍 지원 | 없음 | `proxy_buffering off` + `Connection ''` 적용 |
| OAuth2 HTTPS 헤더 | 없음 | `X-Forwarded-Proto/Port` 주입 |
| `/api/legacy/*` 경로 | 없음 | 성능 비교 테스트용 추가 |
| 인증 처리 시간 측정 | 없음 | `ngx.now()` 기반 ms 단위 로그 |
