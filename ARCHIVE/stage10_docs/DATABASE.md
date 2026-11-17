# 六套业务数据集及任务规范

下列为开发建议数据字典，是将历史图中不一致字段统一后的基线。正式种子尚未创建。所有表/字段使用英文小写蛇形命名；业务展示中文名。数据为虚构，不使用真实客户数据。金额以马币为例、DECIMAL(12,2)；本地执行需验证定点或舍入语义。整数主键，外键类型一致；日期 ISO 格式，时间固定时区。数据生成用固定随机种子，涵盖边界、空值、重复非主键、无匹配及并列，主外键有效，保留独立测试夹具。建议每库核心表几十到几百行，交易明细数百至数千行，手机性能实测调整，不预设固定行数。

## 1 零售销售 retail
- stores(store_id PK, store_name, city)
- products(product_id PK, product_name, category, unit_cost, list_price)
- customers(customer_id PK, customer_name, signup_date)
- orders(order_id PK, store_id FK, customer_id FK nullable, order_date, status)
- order_items(order_item_id PK, order_id FK, product_id FK, quantity, unit_price, discount_amount)
关系：stores/customers→orders→order_items；products→order_items，一对多。完成订单口径 status='completed'；净销售额=quantity*unit_price-discount_amount（整行折扣），折扣不能超过金额。
五任务：门店销售总额；类别销售表现（分组筛选）；月度销售增长率（上月为0或缺失返回NULL）；各门店销售额前三商品（明确并列使用排名或确定性前三行）；类别销售贡献率（分母全体有效销售，零分母NULL）。

## 2 库存 inventory
- products(product_id PK, product_name, category, unit_cost, reorder_level)
- warehouses(warehouse_id PK, warehouse_name)
- stock_snapshots(snapshot_id PK, product_id FK, warehouse_id FK, snapshot_date, quantity; 唯一组合 product/warehouse/date)
- stock_movements(movement_id PK, product_id FK, warehouse_id FK, movement_date, movement_type, quantity, unit_cost)
关系：products/warehouses→snapshots/movements。movement_type 为入库、销售出库、其他出库等固定枚举，quantity为正。五任务：库存积压（题目给阈值）；低库存预警（按指定日期快照）；库存周转率（指定期间销售出库成本/期初期末平均库存成本）；滞销（期间无销售出库及期末有库存）；库存金额（指定快照数量×单位成本）。

## 3 电商 ecommerce
- customers(customer_id PK, customer_name, signup_date, region)
- products(product_id PK, product_name, category)
- orders(order_id PK, customer_id FK, order_date, status)
- order_items(order_item_id PK, order_id FK, product_id FK, quantity, unit_price, discount_amount)
- payments(payment_id PK, order_id FK, payment_date, amount, status)
关系：customers→orders→items/payments；products→items。避免明细及付款直接多对多相乘。五任务：客单价（有效订单净额/有效订单数）；复购率（指定窗口购买≥2次客户/至少购买1次客户）；客户消费分层（给阈值）；月度新客次月留存（首购月份 cohort，次月有效订单客户数/首购客户数）；消费排名（明确并列）。

## 4 财务 finance
- accounts(account_id PK, account_name, account_type)
- transactions(transaction_id PK, account_id FK, transaction_date, amount, direction, reference_no)
- invoices(invoice_id PK, account_id FK, invoice_date, due_date, amount)
- payments(payment_id PK, invoice_id FK, payment_date, amount)
- budgets(budget_id PK, account_id FK, budget_month, budget_amount; 唯一账户月份)
关系：accounts→transactions/invoices/budgets；invoices→payments。金额正数，方向定义借贷或收入/支出，题目明确取值；不可将发票未付同交易金额直接重复计算。五任务：费用分类汇总；收入月度增长；发票未付金额（截止日）；异常金额（给业务阈值或比较基准）；预算与实际差异（账户/月）。

## 5 人力资源 hr
- departments(department_id PK, department_name)
- employees(employee_id PK, department_id FK, employee_name, hire_date, termination_date nullable)
- salaries(salary_id PK, employee_id FK, salary_month, base_salary, bonus; 唯一员工月份)
- attendance(attendance_id PK, employee_id FK, attendance_date, status)
关系：departments→employees→salaries/attendance。五任务：部门薪资排名；指定日期在职人数；期间离职率（离职数/期初期末平均人数，明确口径与零分母）；部门薪资对比；员工任职时长（固定统计日期）。员工终止日期边界明确，不按当前日期计算测试。

## 6 采购 procurement
- suppliers(supplier_id PK, supplier_name, region)
- products(product_id PK, product_name, category)
- purchase_orders(po_id PK, supplier_id FK, order_date, promised_date, status)
- purchase_items(po_item_id PK, po_id FK, product_id FK, quantity_ordered, unit_price)
- receipts(receipt_id PK, po_item_id FK, receipt_date, quantity_received)
- supplier_invoices(invoice_id PK, po_id FK, invoice_date, amount)
关系：suppliers→orders→items→receipts；products→items；orders→invoices。五任务：采购成本（有效订单）；供应商履约率（按订单或明细，题目明确）；订购与累计收货差异；交付延误（到齐日期或截至日，未到齐标明）；采购发票核对（订单金额对累计发票，避免连接膨胀）。

## 字典/生成/维护
每表补齐字段类型、长度、可空、默认、唯一/检查约束、索引、主外键、单位、枚举及中文解释；图中字段不一致以此为准。所有任务给定统计截止日、状态定义与边界，数据字典与应用真实 schema 同步生成。生成 MySQL DDL/种子及等价 SQLite 版本；数据行数、项目表数量、图中关系从元数据读取。禁止破坏内置模板。课程小型演示可有独立 fixture（不是新增第7套项目），课堂/练习数据与实验室个人库隔离。所有项目任务以多组夹具和双引擎参考查询验证后发布。
