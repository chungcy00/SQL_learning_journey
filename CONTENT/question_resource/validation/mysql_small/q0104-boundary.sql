-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoices` (`invoice_id` INT, `account_id` INT, `invoice_date` DATE, `due_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `invoices` VALUES
(11,1,'2026-01-01','2026-01-31',150),
(12,1,'2026-01-02','2026-02-15',200),
(13,3,'2026-02-01','2026-03-01',0),
(14,3,'2026-02-07','2026-02-28',80);
CREATE TABLE `payments` (`payment_id` INT, `invoice_id` INT, `payment_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `payments` VALUES
(20,11,'2026-01-31',60),
(21,11,'2026-02-01',10),
(22,12,'2026-02-28',200),
(23,12,'2026-03-01',5);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT i.invoice_id,p.payment_id FROM payments p RIGHT JOIN invoices i ON p.invoice_id=i.invoice_id ORDER BY i.invoice_id,p.payment_id;
