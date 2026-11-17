-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `raw_data` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
-- 参考解；等价解须从同一初始快照另行运行。
WITH a AS (SELECT CASE WHEN LOWER(TRIM(category))='skin' THEN '个护' WHEN LOWER(TRIM(category))='home' THEN '家居' ELSE '其他' END AS clean_category FROM raw_data) SELECT clean_category,COUNT(*) AS n FROM a GROUP BY clean_category ORDER BY clean_category;
