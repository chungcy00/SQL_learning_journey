-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `transactions` VALUES (1,1,'2026-01-04',100,'expense','1'),
(2,2,'2026-01-10',1000,'income','2'),
(3,2,'2026-02-05',1200,'income','3'),
(4,3,'2026-01-20',75,'expense','4'),
(5,1,'2026-01-25',300,'expense','5');

WITH a AS (SELECT DATE_FORMAT(transaction_date,'%Y-%m') AS month,SUM(amount) AS income FROM transactions WHERE direction='income' GROUP BY DATE_FORMAT(transaction_date,'%Y-%m')), b AS (SELECT a.*,LAG(income) OVER(ORDER BY month) AS prev FROM a) SELECT month,income,ROUND(100.0*(income-prev)/NULLIF(prev,0),2) AS growth_pct FROM b ORDER BY month;
