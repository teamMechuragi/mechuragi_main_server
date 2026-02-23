ALTER TABLE recommended_foods
    DROP COLUMN IF EXISTS ingredients,
    DROP COLUMN IF EXISTS cooking_time,
    DROP COLUMN IF EXISTS difficulty;
