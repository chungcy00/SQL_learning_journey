-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `transactions` VALUES
(11,1,'2026-01-01',100,'expense','x'),
(12,1,'2026-02-01',200,'expense','y'),
(13,3,'2026-03-01',50,'expense','z'),
(14,2,'2026-02-28',1000,'income','w');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT transaction_id,DATE_FORMAT(transaction_date,'%Y-%m') AS month_key FROM transactions ORDER BY transaction_id;
