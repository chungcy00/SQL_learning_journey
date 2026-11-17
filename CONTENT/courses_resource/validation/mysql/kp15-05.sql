-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `monthly` (`month` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT) ENGINE=InnoDB;

INSERT INTO `monthly` VALUES ('2026-01',1000),
('2026-02',1500),
('2026-03',1500),
('2026-04',900);

WITH m AS (SELECT month,sales,LAG(sales) OVER(ORDER BY month) AS prev,AVG(sales) OVER(ORDER BY month ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg FROM monthly) SELECT month,ROUND(moving_avg,2) AS avg3,ROUND(100.0*(sales-prev)/NULLIF(prev,0),2) AS growth_pct FROM m ORDER BY month;
