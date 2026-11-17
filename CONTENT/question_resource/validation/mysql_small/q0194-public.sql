-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `budgets` (`budget_id` INT, `account_id` INT, `budget_month` DATE, `budget_amount` DECIMAL(12,2));
INSERT INTO `budgets` VALUES
(1,1,'2026-01-01',350),
(2,3,'2026-01-01',100);
CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `transactions` VALUES
(1,1,'2026-01-04',100,'expense','1'),
(2,2,'2026-01-10',1000,'income','2'),
(3,2,'2026-02-05',1200,'income','3'),
(4,3,'2026-01-20',75,'expense','4'),
(5,1,'2026-01-25',300,'expense','5');
-- 参考解；等价解须从同一初始快照另行运行。
WITH b AS (SELECT account_id,SUM(budget_amount) AS budget FROM budgets GROUP BY account_id),a AS (SELECT account_id,SUM(amount) AS actual FROM transactions WHERE direction='expense' GROUP BY account_id) SELECT b.account_id,COALESCE(a.actual,0)-b.budget AS variance FROM b LEFT JOIN a ON a.account_id=b.account_id ORDER BY b.account_id;
