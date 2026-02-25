-- ================================================================
-- recommendation_sessions 테이블 Enum 값 마이그레이션
-- V5에서 누락된 기존 한국어 값 → 영문 Enum 상수명으로 변환
-- (V3에서 food_preferences에 적용한 동일 기준)
-- ================================================================

SET SQL_SAFE_UPDATES = 0;

-- diet_status
UPDATE recommendation_sessions SET diet_status = 'NONE'        WHERE diet_status = '해당_없음';
UPDATE recommendation_sessions SET diet_status = 'WEIGHT_LOSS' WHERE diet_status = '다이어트_중';
UPDATE recommendation_sessions SET diet_status = 'BULKING'     WHERE diet_status = '근성장';
UPDATE recommendation_sessions SET diet_status = 'MAINTENANCE' WHERE diet_status = '유지어터';

-- vegan_option
UPDATE recommendation_sessions SET vegan_option = 'NONE'        WHERE vegan_option = '해당없음';
UPDATE recommendation_sessions SET vegan_option = 'VEGAN'       WHERE vegan_option = '비건';
UPDATE recommendation_sessions SET vegan_option = 'VEGETARIAN'  WHERE vegan_option IN ('락토_베지테리언', '락토_오보_베지테리언', '오보_베지테리언');
UPDATE recommendation_sessions SET vegan_option = 'PESCATARIAN' WHERE vegan_option = '페스코_베지테리언';
UPDATE recommendation_sessions SET vegan_option = 'FLEXITARIAN' WHERE vegan_option = '플렉시테리언';
UPDATE recommendation_sessions SET vegan_option = 'NONE'        WHERE vegan_option IN ('폴로_베지테리언', '프루테리언');

-- spice_level
UPDATE recommendation_sessions SET spice_level = 'VERY_MILD' WHERE spice_level = '맵찔이';
UPDATE recommendation_sessions SET spice_level = 'MILD'      WHERE spice_level = '순한맛';
UPDATE recommendation_sessions SET spice_level = 'MEDIUM'    WHERE spice_level = '신라면';
UPDATE recommendation_sessions SET spice_level = 'HOT'       WHERE spice_level = '불닭';
UPDATE recommendation_sessions SET spice_level = 'EXTREME'   WHERE spice_level = '핵불닭';

SET SQL_SAFE_UPDATES = 1;
