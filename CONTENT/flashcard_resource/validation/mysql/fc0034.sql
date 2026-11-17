-- fc0034：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SELECT NULLIF(5,5) AS a,NULLIF(5,0) AS b,COALESCE(NULL,0,7) AS c;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["a", "b", "c"], "rows": [[null, 5, 0]]}
