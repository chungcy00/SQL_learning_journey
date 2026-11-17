-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoices` (`invoice_id` INT, `account_id` INT, `invoice_date` DATE, `due_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `invoices` VALUES
(1,1,'2026-01-01','2026-01-31',100),
(2,1,'2026-01-02','2026-02-01',200);
CREATE TABLE `payments` (`payment_id` INT, `invoice_id` INT, `payment_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `payments` VALUES
(1,1,'2026-01-10',40),
(2,1,'2026-02-02',60);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT i.invoice_id,i.amount FROM invoices i WHERE NOT EXISTS(SELECT 1 FROM payments p WHERE p.invoice_id=i.invoice_id) ORDER BY i.invoice_id;
