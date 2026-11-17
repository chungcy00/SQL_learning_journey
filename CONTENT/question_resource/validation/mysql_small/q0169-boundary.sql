-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoices` (`invoice_id` INT, `account_id` INT, `invoice_date` DATE, `due_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `invoices` VALUES
(11,1,'2026-01-01','2026-01-31',150),
(12,1,'2026-01-02','2026-02-15',200),
(13,3,'2026-02-01','2026-03-01',0),
(14,3,'2026-02-07','2026-02-28',80);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT invoice_id,DATEDIFF(due_date,invoice_date) AS days_allowed FROM invoices ORDER BY invoice_id;
