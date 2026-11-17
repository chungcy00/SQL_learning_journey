-- fc0109：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<3) SELECT x FROM n ORDER BY x;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["x"], "rows": [[1], [2], [3]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
