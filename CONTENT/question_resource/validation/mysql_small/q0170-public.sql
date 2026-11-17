-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoices` (`invoice_id` INT, `account_id` INT, `invoice_date` DATE, `due_date` DATE, `amount` DECIMAL(12,2));
INSERT INTO `invoices` VALUES
(1,1,'2026-01-01','2026-01-31',100),
(2,1,'2026-01-02','2026-02-01',200);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT invoice_id,DATEDIFF('2026-02-15',invoice_date) AS age_days FROM invoices ORDER BY invoice_id;
