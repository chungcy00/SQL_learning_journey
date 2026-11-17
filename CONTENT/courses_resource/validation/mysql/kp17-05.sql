-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE demo_parent(id INT PRIMARY KEY NOT NULL, code VARCHAR(20) NOT NULL UNIQUE); CREATE TABLE demo_child(id INT PRIMARY KEY NOT NULL,parent_id INT,FOREIGN KEY(parent_id) REFERENCES demo_parent(id)); INSERT INTO demo_parent VALUES(1,'A'),(2,'B'); INSERT INTO demo_child VALUES(1,1),(2,1);

SELECT p.id,COUNT(c.id) AS children FROM demo_parent p LEFT JOIN demo_child c ON p.id=c.parent_id GROUP BY p.id ORDER BY p.id;
