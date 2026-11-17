# inventory 业务数据库

独立文件 `inventory.sqlite` 直接打开，不能修改基准。`mysql_import.sql` 在空 MySQL 8.4 库执行（原生尚未实测）。结构化 CSV 共用 `structured_data/`，以本目录 schema.json 的表清单为准。

22 张表，1 个视图，376190 行。支持本项目的5项实战或综合20项。共享表完整复制，可能含其他渠道依赖数据，不能默认所有记录属于本业务。
