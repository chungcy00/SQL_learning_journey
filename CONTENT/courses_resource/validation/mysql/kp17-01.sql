-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE demo_stock(id INT PRIMARY KEY NOT NULL,qty INT NOT NULL CHECK(qty>=0)); INSERT INTO demo_stock VALUES(1,10),(2,20);

SELECT id,qty FROM demo_stock ORDER BY id;
