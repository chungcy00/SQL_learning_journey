-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE demo_stock(id INT PRIMARY KEY,qty INT); INSERT INTO demo_stock VALUES(1,10),(2,20),(3,30); UPDATE demo_stock SET qty=25 WHERE id=2; DELETE FROM demo_stock WHERE id=3;

SELECT id,qty FROM demo_stock ORDER BY id;
