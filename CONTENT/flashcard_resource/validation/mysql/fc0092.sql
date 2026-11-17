-- fc0092：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SELECT DATEDIFF('2025-03-01','2025-02-27') AS days;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["days"], "rows": [[2]]}
