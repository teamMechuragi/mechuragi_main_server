-- ================================================================
-- RecommendationSession 엔티티 리팩토링
-- - context, food_types, tastes, avoided_foods, allergies
--   TEXT(콤마 구분) → @ElementCollection 테이블로 분리
-- - dietStatus, veganOption, spiceLevel: String → Enum (컬럼 유지, 크기 보정)
-- - number_of_diners 컬럼 추가
-- ================================================================

-- ----------------------------------------------------------------
-- 1. 새 컬렉션 테이블 생성
-- ----------------------------------------------------------------

CREATE TABLE session_context (
    session_id   BIGINT       NOT NULL,
    context_item VARCHAR(255) NOT NULL,
    CONSTRAINT fk_sc_session FOREIGN KEY (session_id)
        REFERENCES recommendation_sessions (id) ON DELETE CASCADE
);

CREATE TABLE session_preferred_food_types (
    session_id BIGINT      NOT NULL,
    food_type  VARCHAR(100) NOT NULL,
    CONSTRAINT fk_spft_session FOREIGN KEY (session_id)
        REFERENCES recommendation_sessions (id) ON DELETE CASCADE
);

CREATE TABLE session_preferred_tastes (
    session_id BIGINT      NOT NULL,
    taste      VARCHAR(100) NOT NULL,
    CONSTRAINT fk_spt_session FOREIGN KEY (session_id)
        REFERENCES recommendation_sessions (id) ON DELETE CASCADE
);

CREATE TABLE session_avoided_foods (
    session_id BIGINT       NOT NULL,
    food_name  VARCHAR(100) NOT NULL,
    CONSTRAINT fk_saf_session FOREIGN KEY (session_id)
        REFERENCES recommendation_sessions (id) ON DELETE CASCADE
);

CREATE TABLE session_allergies (
    session_id   BIGINT       NOT NULL,
    allergy_name VARCHAR(100) NOT NULL,
    CONSTRAINT fk_sal_session FOREIGN KEY (session_id)
        REFERENCES recommendation_sessions (id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------
-- 2. 데이터 마이그레이션: 콤마 구분 TEXT → 컬렉션 테이블
--    (MySQL 8.0 recursive CTE 사용)
-- ----------------------------------------------------------------

-- 2-1. context
INSERT INTO session_context (session_id, context_item)
WITH RECURSIVE split (session_id, val, rest) AS (
    SELECT id,
           TRIM(SUBSTRING_INDEX(context, ',', 1)),
           IF(LOCATE(',', context) > 0, TRIM(SUBSTRING(context, LOCATE(',', context) + 1)), NULL)
    FROM recommendation_sessions
    WHERE context IS NOT NULL AND context != ''
    UNION ALL
    SELECT session_id,
           TRIM(SUBSTRING_INDEX(rest, ',', 1)),
           IF(LOCATE(',', rest) > 0, TRIM(SUBSTRING(rest, LOCATE(',', rest) + 1)), NULL)
    FROM split
    WHERE rest IS NOT NULL
)
SELECT session_id, val FROM split WHERE val IS NOT NULL AND val != '';

-- 2-2. food_types → session_preferred_food_types
INSERT INTO session_preferred_food_types (session_id, food_type)
WITH RECURSIVE split (session_id, val, rest) AS (
    SELECT id,
           TRIM(SUBSTRING_INDEX(food_types, ',', 1)),
           IF(LOCATE(',', food_types) > 0, TRIM(SUBSTRING(food_types, LOCATE(',', food_types) + 1)), NULL)
    FROM recommendation_sessions
    WHERE food_types IS NOT NULL AND food_types != ''
    UNION ALL
    SELECT session_id,
           TRIM(SUBSTRING_INDEX(rest, ',', 1)),
           IF(LOCATE(',', rest) > 0, TRIM(SUBSTRING(rest, LOCATE(',', rest) + 1)), NULL)
    FROM split
    WHERE rest IS NOT NULL
)
SELECT session_id, val FROM split WHERE val IS NOT NULL AND val != '';

-- 2-3. tastes → session_preferred_tastes
INSERT INTO session_preferred_tastes (session_id, taste)
WITH RECURSIVE split (session_id, val, rest) AS (
    SELECT id,
           TRIM(SUBSTRING_INDEX(tastes, ',', 1)),
           IF(LOCATE(',', tastes) > 0, TRIM(SUBSTRING(tastes, LOCATE(',', tastes) + 1)), NULL)
    FROM recommendation_sessions
    WHERE tastes IS NOT NULL AND tastes != ''
    UNION ALL
    SELECT session_id,
           TRIM(SUBSTRING_INDEX(rest, ',', 1)),
           IF(LOCATE(',', rest) > 0, TRIM(SUBSTRING(rest, LOCATE(',', rest) + 1)), NULL)
    FROM split
    WHERE rest IS NOT NULL
)
SELECT session_id, val FROM split WHERE val IS NOT NULL AND val != '';

-- 2-4. avoided_foods → session_avoided_foods
INSERT INTO session_avoided_foods (session_id, food_name)
WITH RECURSIVE split (session_id, val, rest) AS (
    SELECT id,
           TRIM(SUBSTRING_INDEX(avoided_foods, ',', 1)),
           IF(LOCATE(',', avoided_foods) > 0, TRIM(SUBSTRING(avoided_foods, LOCATE(',', avoided_foods) + 1)), NULL)
    FROM recommendation_sessions
    WHERE avoided_foods IS NOT NULL AND avoided_foods != ''
    UNION ALL
    SELECT session_id,
           TRIM(SUBSTRING_INDEX(rest, ',', 1)),
           IF(LOCATE(',', rest) > 0, TRIM(SUBSTRING(rest, LOCATE(',', rest) + 1)), NULL)
    FROM split
    WHERE rest IS NOT NULL
)
SELECT session_id, val FROM split WHERE val IS NOT NULL AND val != '';

-- 2-5. allergies → session_allergies
INSERT INTO session_allergies (session_id, allergy_name)
WITH RECURSIVE split (session_id, val, rest) AS (
    SELECT id,
           TRIM(SUBSTRING_INDEX(allergies, ',', 1)),
           IF(LOCATE(',', allergies) > 0, TRIM(SUBSTRING(allergies, LOCATE(',', allergies) + 1)), NULL)
    FROM recommendation_sessions
    WHERE allergies IS NOT NULL AND allergies != ''
    UNION ALL
    SELECT session_id,
           TRIM(SUBSTRING_INDEX(rest, ',', 1)),
           IF(LOCATE(',', rest) > 0, TRIM(SUBSTRING(rest, LOCATE(',', rest) + 1)), NULL)
    FROM split
    WHERE rest IS NOT NULL
)
SELECT session_id, val FROM split WHERE val IS NOT NULL AND val != '';

-- ----------------------------------------------------------------
-- 3. Enum 컬럼 크기 보정 (VARCHAR(255) → VARCHAR(20), NOT NULL 해제)
--    recommendation_sessions는 스냅샷이므로 nullable 허용
-- ----------------------------------------------------------------

ALTER TABLE recommendation_sessions
    MODIFY COLUMN diet_status  VARCHAR(20) NULL,
    MODIFY COLUMN vegan_option VARCHAR(20) NULL,
    MODIFY COLUMN spice_level  VARCHAR(20) NULL;

-- ----------------------------------------------------------------
-- 4. number_of_diners 컬럼 추가
-- ----------------------------------------------------------------

ALTER TABLE recommendation_sessions
    ADD COLUMN number_of_diners INT NULL;

-- ----------------------------------------------------------------
-- 5. 구 TEXT 컬럼 삭제
-- ----------------------------------------------------------------

ALTER TABLE recommendation_sessions
    DROP COLUMN context,
    DROP COLUMN food_types,
    DROP COLUMN tastes,
    DROP COLUMN avoided_foods,
    DROP COLUMN allergies;
