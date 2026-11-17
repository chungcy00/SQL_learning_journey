-- fc0150：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `store_month` (`month_key` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `net_cents` INT) ENGINE=InnoDB;
INSERT INTO `store_month` VALUES ('2025-01', 10000), ('2025-02', 0), ('2025-03', 20000), ('2025-04', 15000);
SELECT month_key,amount_prev FROM (SELECT month_key,LAG(net_cents) OVER(ORDER BY month_key) AS amount_prev FROM store_month) m ORDER BY month_key;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["month_key", "amount_prev"], "rows": [["2025-01", null], ["2025-02", 10000], ["2025-03", 0], ["2025-04", 20000]]}
