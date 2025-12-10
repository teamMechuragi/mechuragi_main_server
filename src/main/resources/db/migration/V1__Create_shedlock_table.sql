-- ShedLock 테이블 생성
-- 블루그린 배포 시 스케줄러 중복 실행 방지용
--
-- 컬럼 설명:
--   name: 스케줄러 작업 이름 (PK)
--   lock_until: 락 만료 시간
--   locked_at: 락 획득 시간
--   locked_by: 락을 획득한 인스턴스 식별자

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
