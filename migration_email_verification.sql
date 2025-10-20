-- email_verifications 테이블 수정
-- member_id 컬럼을 email 컬럼으로 변경

-- 1. 기존 데이터 백업 (선택 사항)
-- CREATE TABLE email_verifications_backup AS SELECT * FROM email_verifications;

-- 2. 기존 제약 조건 삭제
ALTER TABLE email_verifications DROP INDEX IF EXISTS UK_member_id;

-- 3. member_id 컬럼 삭제
ALTER TABLE email_verifications DROP COLUMN member_id;

-- 4. email 컬럼 추가
ALTER TABLE email_verifications ADD COLUMN email VARCHAR(100) NOT NULL;

-- 5. email에 UNIQUE 제약 조건 추가
ALTER TABLE email_verifications ADD UNIQUE KEY UK_email (email);

-- 6. 기존 데이터 모두 삭제 (구조가 바뀌었으므로)
TRUNCATE TABLE email_verifications;
