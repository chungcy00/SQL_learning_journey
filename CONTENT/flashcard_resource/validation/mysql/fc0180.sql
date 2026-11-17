-- fc0180：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `month_axis` (`month_key` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `month_axis` VALUES ('2025-01'), ('2025-02'), ('2025-03');
CREATE TABLE `months_missing` (`month_key` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `net_cents` INT) ENGINE=InnoDB;
INSERT INTO `months_missing` VALUES ('2025-01', 10000), ('2025-03', 15000);
WITH m AS (SELECT a.month_key,COALESCE(s.net_cents,0) AS net FROM month_axis a LEFT JOIN months_missing s ON a.month_key=s.month_key) SELECT month_key,net,LAG(net) OVER(ORDER BY month_key) AS previous FROM m ORDER BY month_key;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["month_key", "net", "previous"], "rows": [["2025-01", 10000, null], ["2025-02", 0, 10000], ["2025-03", 15000, 0]]}
