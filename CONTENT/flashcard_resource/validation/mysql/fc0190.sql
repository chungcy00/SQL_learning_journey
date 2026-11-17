-- fc0190：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `cost_series` (`month_key` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `net_cents` INT, `expense_cents` INT) ENGINE=InnoDB;
INSERT INTO `cost_series` VALUES ('2025-01', 10000, 1000), ('2025-02', 1000, 500), ('2025-03', 10000, 2000), ('2025-04', 10000, 3000);
SELECT month_key,CASE WHEN COUNT(*) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)=3 THEN ROUND(100.0*SUM(expense_cents) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)/NULLIF(SUM(net_cents) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW),0),2) END AS pct FROM cost_series ORDER BY month_key;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["month_key", "pct"], "rows": [["2025-01", null], ["2025-02", null], ["2025-03", 16.67], ["2025-04", 26.19]]}
