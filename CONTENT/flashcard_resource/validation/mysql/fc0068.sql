-- fc0068：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
SELECT CASE WHEN 85>=60 THEN '及格' WHEN 85>=80 THEN '良好' ELSE '未及格' END AS label;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["label"], "rows": [["及格"]]}
