-- fc0094：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SELECT ABS(-9) AS a,ROUND(3.146,2) AS r,MOD(17,5) AS m;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["a", "r", "m"], "rows": [[9, 3.15, 2]]}
