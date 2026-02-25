-- 기존 별도 엔티티 테이블 삭제
DROP TABLE IF EXISTS disliked_food;
DROP TABLE IF EXISTS preference_food_type;
DROP TABLE IF EXISTS preference_taste;

-- food_preferences 에서 allergy_info 컬럼 삭제
ALTER TABLE food_preferences
    DROP COLUMN IF EXISTS allergy_info;

-- @ElementCollection 테이블 생성
CREATE TABLE preferred_food_types (
    preference_id BIGINT      NOT NULL,
    food_type     VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pft_preference FOREIGN KEY (preference_id)
        REFERENCES food_preferences (id) ON DELETE CASCADE
);

CREATE TABLE preferred_tastes (
    preference_id BIGINT      NOT NULL,
    taste         VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pt_preference FOREIGN KEY (preference_id)
        REFERENCES food_preferences (id) ON DELETE CASCADE
);

CREATE TABLE avoided_foods (
    preference_id BIGINT       NOT NULL,
    food_name     VARCHAR(100) NOT NULL,
    CONSTRAINT fk_af_preference FOREIGN KEY (preference_id)
        REFERENCES food_preferences (id) ON DELETE CASCADE
);

CREATE TABLE allergies (
    preference_id BIGINT       NOT NULL,
    allergy_name  VARCHAR(100) NOT NULL,
    CONSTRAINT fk_al_preference FOREIGN KEY (preference_id)
        REFERENCES food_preferences (id) ON DELETE CASCADE
);
