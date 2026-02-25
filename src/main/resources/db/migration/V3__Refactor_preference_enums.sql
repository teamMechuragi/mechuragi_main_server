-- ================================================================
-- 취향 Enum 컬럼 리팩토링
-- - 한국어 enum 이름 → 영어 enum 이름으로 변경
-- - is_on_diet 컬럼명 → diet_status 로 변경
-- - VARCHAR 길이 확장 (한국어 3자 기준 → 영어 20자 기준)
-- ================================================================

-- 1. is_on_diet 데이터를 diet_status로 복사 후 삭제
--    (Hibernate ddl-auto=update가 diet_status 컬럼을 이미 생성했을 수 있으므로 RENAME 대신 이 방식 사용)
UPDATE food_preferences SET diet_status = is_on_diet WHERE diet_status IS NULL OR diet_status = '';
ALTER TABLE food_preferences DROP COLUMN is_on_diet;

-- 2. 컬럼 크기 확장
ALTER TABLE food_preferences
    MODIFY COLUMN diet_status  VARCHAR(20) NOT NULL DEFAULT 'NONE',
    MODIFY COLUMN vegan_option VARCHAR(20) NOT NULL DEFAULT 'NONE',
    MODIFY COLUMN spice_level  VARCHAR(20) NOT NULL DEFAULT 'MILD';

-- 3. 데이터 마이그레이션 (safe update 모드 일시 해제)
SET SQL_SAFE_UPDATES = 0;

-- diet_status 데이터 마이그레이션
UPDATE food_preferences SET diet_status = 'NONE'        WHERE diet_status = '해당_없음';
UPDATE food_preferences SET diet_status = 'WEIGHT_LOSS' WHERE diet_status = '다이어트_중';

-- 4. vegan_option 데이터 마이그레이션
UPDATE food_preferences SET vegan_option = 'NONE'        WHERE vegan_option = '해당없음';
UPDATE food_preferences SET vegan_option = 'VEGAN'       WHERE vegan_option = '비건';
UPDATE food_preferences SET vegan_option = 'VEGETARIAN'  WHERE vegan_option IN ('락토_베지테리언', '락토_오보_베지테리언', '오보_베지테리언');
UPDATE food_preferences SET vegan_option = 'PESCATARIAN' WHERE vegan_option = '페스코_베지테리언';
UPDATE food_preferences SET vegan_option = 'FLEXITARIAN' WHERE vegan_option = '플렉시테리언';
UPDATE food_preferences SET vegan_option = 'NONE'        WHERE vegan_option IN ('폴로_베지테리언', '프루테리언');

-- 5. spice_level 데이터 마이그레이션
UPDATE food_preferences SET spice_level = 'VERY_MILD' WHERE spice_level = '맵찔이';
UPDATE food_preferences SET spice_level = 'MILD'      WHERE spice_level = '순한맛';
UPDATE food_preferences SET spice_level = 'MEDIUM'    WHERE spice_level = '신라면';
UPDATE food_preferences SET spice_level = 'HOT'       WHERE spice_level = '불닭';
UPDATE food_preferences SET spice_level = 'EXTREME'   WHERE spice_level = '핵불닭';

SET SQL_SAFE_UPDATES = 1;
