-- fc0125：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SELECT CAST('0042' AS SIGNED) AS n;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["n"], "rows": [[42]]}
