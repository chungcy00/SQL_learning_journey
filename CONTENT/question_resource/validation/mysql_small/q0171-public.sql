-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `transactions` VALUES
(1,1,'2026-01-04',100,'expense','1'),
(2,2,'2026-01-10',1000,'income','2'),
(3,2,'2026-02-05',1200,'income','3'),
(4,3,'2026-01-20',75,'expense','4'),
(5,1,'2026-01-25',300,'expense','5');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT transaction_id,DATE_FORMAT(transaction_date,'%Y-%m') AS month_key FROM transactions ORDER BY transaction_id;
