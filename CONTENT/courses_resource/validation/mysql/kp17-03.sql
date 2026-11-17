-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE demo_stock(id INT PRIMARY KEY); INSERT INTO demo_stock VALUES(1); ALTER TABLE demo_stock ADD COLUMN label VARCHAR(80); CREATE TABLE disposable(id INT); DROP TABLE disposable;

SELECT id,label FROM demo_stock ORDER BY id;
