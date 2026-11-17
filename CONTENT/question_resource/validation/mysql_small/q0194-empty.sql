-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `budgets` (`budget_id` INT, `account_id` INT, `budget_month` DATE, `budget_amount` DECIMAL(12,2));
CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
-- 参考解；等价解须从同一初始快照另行运行。
WITH b AS (SELECT account_id,SUM(budget_amount) AS budget FROM budgets GROUP BY account_id),a AS (SELECT account_id,SUM(amount) AS actual FROM transactions WHERE direction='expense' GROUP BY account_id) SELECT b.account_id,COALESCE(a.actual,0)-b.budget AS variance FROM b LEFT JOIN a ON a.account_id=b.account_id ORDER BY b.account_id;
