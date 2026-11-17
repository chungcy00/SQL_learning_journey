-- fc0165：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoice_mini` (`invoice_id` INT, `billed_cents` INT) ENGINE=InnoDB;
INSERT INTO `invoice_mini` VALUES (1, 10000), (2, 5000), (3, 5000);
CREATE TABLE `pay_mini` (`invoice_id` INT, `payment_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount_cents` INT) ENGINE=InnoDB;
INSERT INTO `pay_mini` VALUES (1, '2025-12-10', 4000), (1, '2026-01-02', 6000), (3, '2025-12-20', 5500);
WITH p AS (SELECT invoice_id,SUM(amount_cents) AS paid FROM pay_mini WHERE payment_date<='2025-12-31' GROUP BY invoice_id) SELECT i.invoice_id,i.billed_cents-COALESCE(p.paid,0) AS balance FROM invoice_mini i LEFT JOIN p ON i.invoice_id=p.invoice_id ORDER BY i.invoice_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["invoice_id", "balance"], "rows": [[1, 6000], [2, 5000], [3, -500]]}
