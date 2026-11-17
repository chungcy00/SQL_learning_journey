-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `invoices` (`invoice_id` INT, `account_id` INT, `invoice_date` DATE, `due_date` DATE, `amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `invoices` VALUES (1,1,'2026-01-01','2026-01-31',100),
(2,1,'2026-01-02','2026-02-01',200);

CREATE TABLE `payments` (`payment_id` INT, `invoice_id` INT, `payment_date` DATE, `amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `payments` VALUES (1,1,'2026-01-10',40),
(2,1,'2026-02-02',60);

WITH p AS (SELECT invoice_id,SUM(amount) AS paid FROM payments WHERE payment_date<'2026-02-01' GROUP BY invoice_id) SELECT i.invoice_id,i.amount-COALESCE(p.paid,0) AS outstanding FROM invoices i LEFT JOIN p ON i.invoice_id=p.invoice_id WHERE i.invoice_date<'2026-02-01' ORDER BY i.invoice_id;
