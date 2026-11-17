-- fc0003：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE parent_card (id INT NOT NULL PRIMARY KEY); CREATE TABLE child_card (id INT NOT NULL PRIMARY KEY, client_id INT, FOREIGN KEY (client_id) REFERENCES parent_card(id)); INSERT INTO parent_card VALUES (1); INSERT INTO child_card VALUES (10,1);
SELECT id,client_id FROM child_card ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "client_id"], "rows": [[10, 1]]}
