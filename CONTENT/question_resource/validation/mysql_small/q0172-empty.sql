-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `payments` (`payment_id` INT, `invoice_id` INT, `payment_date` DATE, `amount` DECIMAL(12,2));
-- 参考解；等价解须从同一初始快照另行运行。
SELECT COALESCE(SUM(amount),0) AS total_paid FROM payments WHERE payment_date>='2026-02-01' AND payment_date<'2026-03-01';
