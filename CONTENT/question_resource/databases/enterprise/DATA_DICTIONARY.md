# enterprise 数据字典

金额字段 `_cents` 为整数马币分，展示层除以100。日期范围2023—2025，部分承诺/到期日期延至2026。

## calendar_days · 连续日期维度（1096 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| day | DATE | 否 | PK  | 日历日期 |
| month_key | CHAR(7) | 否 |  | 统计月份 YYYY-MM |
| day_index | INT | 否 |  | 从2023-01-01开始的日序号（0起） |

约束及索引：
```sql
CREATE TABLE calendar_days (
  day DATE PRIMARY KEY,
  month_key CHAR(7) NOT NULL,
  day_index INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## stores · 门店主档（12 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| store_id | BIGINT | 否 | PK  | 主键/标识 |
| store_name | VARCHAR(160) | 否 |  | 名称 |
| city | VARCHAR(160) | 否 |  | 业务字段 |

约束及索引：
```sql
CREATE TABLE stores (
  store_id BIGINT PRIMARY KEY,
  store_name VARCHAR(160) NOT NULL,
  city VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## categories · 商品分类（15 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| category_id | BIGINT | 否 | PK  | 主键/标识 |
| category_name | VARCHAR(160) | 否 |  | 名称 |

约束及索引：
```sql
CREATE TABLE categories (
  category_id BIGINT PRIMARY KEY,
  category_name VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## products · 商品主档（500 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| product_id | BIGINT | 否 | PK  | 主键/标识 |
| category_id | BIGINT | 否 | categories.category_id | 主键/标识 |
| product_name | VARCHAR(160) | 否 |  | 名称 |
| list_price_cents | BIGINT | 否 |  | 金额（马币分） |
| reorder_level | INT | 否 |  | 业务字段 |
| active | INT | 否 |  | 有效标志 1/0 |

约束及索引：
```sql
CREATE TABLE products (
  product_id BIGINT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  product_name VARCHAR(160) NOT NULL,
  list_price_cents BIGINT NOT NULL CHECK(list_price_cents>=0),
  reorder_level INT NOT NULL CHECK(reorder_level>=0),
  active INT NOT NULL CHECK(active IN (0,1)),
  FOREIGN KEY (category_id) REFERENCES categories(category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_products_category_id ON products(category_id)
```

## customers · 客户主档（虚构）（5000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| customer_id | BIGINT | 否 | PK  | 主键/标识 |
| customer_name | VARCHAR(160) | 否 |  | 名称 |
| signup_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| region | VARCHAR(160) | 是 |  | 区域（客户可缺失） |

约束及索引：
```sql
CREATE TABLE customers (
  customer_id BIGINT PRIMARY KEY,
  customer_name VARCHAR(160) NOT NULL,
  signup_date DATE NOT NULL,
  region VARCHAR(160)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_customers_signup_date ON customers(signup_date)
```

## suppliers · 供应商主档（60 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| supplier_id | BIGINT | 否 | PK  | 主键/标识 |
| supplier_name | VARCHAR(160) | 否 |  | 名称 |
| region | VARCHAR(160) | 否 |  | 区域（客户可缺失） |

约束及索引：
```sql
CREATE TABLE suppliers (
  supplier_id BIGINT PRIMARY KEY,
  supplier_name VARCHAR(160) NOT NULL,
  region VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## warehouses · 配送仓主档（3 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| warehouse_id | BIGINT | 否 | PK  | 主键/标识 |
| warehouse_name | VARCHAR(160) | 否 |  | 名称 |

约束及索引：
```sql
CREATE TABLE warehouses (
  warehouse_id BIGINT PRIMARY KEY,
  warehouse_name VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## departments · 部门主档（7 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| department_id | BIGINT | 否 | PK  | 主键/标识 |
| department_name | VARCHAR(160) | 否 |  | 名称 |

约束及索引：
```sql
CREATE TABLE departments (
  department_id BIGINT PRIMARY KEY,
  department_name VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## product_costs · 标准成本历史（1500 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| cost_id | BIGINT | 否 | PK  | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| effective_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| unit_cost_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE product_costs (
  cost_id BIGINT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  effective_date DATE NOT NULL,
  unit_cost_cents BIGINT NOT NULL CHECK(unit_cost_cents>=0),
  UNIQUE(product_id,effective_date),
  FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_product_costs_product_id ON product_costs(product_id)
```

## promotions · 促销期间（36 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| promotion_id | BIGINT | 否 | PK  | 主键/标识 |
| promotion_name | VARCHAR(160) | 否 |  | 名称 |
| start_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| end_date | DATE | 否 |  | 日期（YYYY-MM-DD） |

约束及索引：
```sql
CREATE TABLE promotions (
  promotion_id BIGINT PRIMARY KEY,
  promotion_name VARCHAR(160) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_promotions_end_date ON promotions(end_date);
CREATE INDEX ix_promotions_start_date ON promotions(start_date)
```

## traffic_sources · 线上访问渠道（5 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| source_id | BIGINT | 否 | PK  | 主键/标识 |
| source_name | VARCHAR(160) | 否 |  | 名称 |

约束及索引：
```sql
CREATE TABLE traffic_sources (
  source_id BIGINT PRIMARY KEY,
  source_name VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## marketing_campaigns · 渠道月广告支出（180 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| campaign_id | BIGINT | 否 | PK  | 主键/标识 |
| source_id | BIGINT | 否 | traffic_sources.source_id | 主键/标识 |
| month_key | CHAR(7) | 否 |  | 统计月份 YYYY-MM |
| spend_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE marketing_campaigns (
  campaign_id BIGINT PRIMARY KEY,
  source_id BIGINT NOT NULL,
  month_key CHAR(7) NOT NULL,
  spend_cents BIGINT NOT NULL CHECK(spend_cents>=0),
  UNIQUE(source_id,month_key),
  FOREIGN KEY (source_id) REFERENCES traffic_sources(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_marketing_campaigns_source_id ON marketing_campaigns(source_id)
```

## customer_sessions · 访问会话（50000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| session_id | BIGINT | 否 | PK  | 主键/标识 |
| customer_id | BIGINT | 否 | customers.customer_id | 主键/标识 |
| source_id | BIGINT | 否 | traffic_sources.source_id | 主键/标识 |
| session_date | DATE | 否 |  | 日期（YYYY-MM-DD） |

约束及索引：
```sql
CREATE TABLE customer_sessions (
  session_id BIGINT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  session_date DATE NOT NULL,
  FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
  FOREIGN KEY (source_id) REFERENCES traffic_sources(source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_customer_sessions_customer_id ON customer_sessions(customer_id);
CREATE INDEX ix_customer_sessions_session_date ON customer_sessions(session_date);
CREATE INDEX ix_customer_sessions_source_id ON customer_sessions(source_id)
```

## orders · 销售订单（线上线下统一）（30000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| order_id | BIGINT | 否 | PK  | 主键/标识 |
| store_id | BIGINT | 是 | stores.store_id | 主键/标识 |
| customer_id | BIGINT | 是 | customers.customer_id | 主键/标识 |
| session_id | BIGINT | 是 | customer_sessions.session_id | 主键/标识 |
| order_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| channel | VARCHAR(160) | 否 |  | store 线下 / online 线上 |
| status | VARCHAR(160) | 否 |  | 业务状态（见表约束及口径） |

约束及索引：
```sql
CREATE TABLE orders (
  order_id BIGINT PRIMARY KEY,
  store_id BIGINT,
  customer_id BIGINT,
  session_id BIGINT,
  order_date DATE NOT NULL,
  channel VARCHAR(160) NOT NULL CHECK(channel IN ('store','online')),
  status VARCHAR(160) NOT NULL CHECK(status IN ('completed','cancelled','pending')),
  CHECK((channel='online' AND store_id IS NULL AND session_id IS NOT NULL) OR (channel='store' AND store_id IS NOT NULL AND session_id IS NULL)),
  FOREIGN KEY (store_id) REFERENCES stores(store_id),
  FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
  FOREIGN KEY (session_id) REFERENCES customer_sessions(session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_orders_customer_id ON orders(customer_id);
CREATE INDEX ix_orders_order_date ON orders(order_date);
CREATE INDEX ix_orders_session_id ON orders(session_id);
CREATE INDEX ix_orders_store_id ON orders(store_id)
```

## order_items · 销售明细（90074 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| order_item_id | BIGINT | 否 | PK  | 主键/标识 |
| order_id | BIGINT | 否 | orders.order_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| promotion_id | BIGINT | 是 | promotions.promotion_id | 主键/标识 |
| quantity | INT | 否 |  | 数量（件） |
| unit_price_cents | BIGINT | 否 |  | 金额（马币分） |
| discount_cents | BIGINT | 否 |  | 金额（马币分） |
| unit_cost_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE order_items (
  order_item_id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  promotion_id BIGINT,
  quantity INT NOT NULL CHECK(quantity>0),
  unit_price_cents BIGINT NOT NULL CHECK(unit_price_cents>=0),
  discount_cents BIGINT NOT NULL CHECK(discount_cents>=0),
  unit_cost_cents BIGINT NOT NULL CHECK(unit_cost_cents>=0),
  CHECK(discount_cents<=quantity*unit_price_cents),
  FOREIGN KEY (order_id) REFERENCES orders(order_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id),
  FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_order_items_order_id ON order_items(order_id);
CREATE INDEX ix_order_items_product_id ON order_items(product_id);
CREATE INDEX ix_order_items_promotion_id ON order_items(promotion_id)
```

## returns · 退货/退款（6117 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| return_id | BIGINT | 否 | PK  | 主键/标识 |
| order_item_id | BIGINT | 否 | order_items.order_item_id | 主键/标识 |
| return_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| quantity | INT | 否 |  | 数量（件） |
| refund_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE returns (
  return_id BIGINT PRIMARY KEY,
  order_item_id BIGINT NOT NULL,
  return_date DATE NOT NULL,
  quantity INT NOT NULL CHECK(quantity>0),
  refund_cents BIGINT NOT NULL CHECK(refund_cents>=0),
  FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_returns_order_item_id ON returns(order_item_id);
CREATE INDEX ix_returns_return_date ON returns(return_date)
```

## payments · 订单支付（31521 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| payment_id | BIGINT | 否 | PK  | 主键/标识 |
| order_id | BIGINT | 否 | orders.order_id | 主键/标识 |
| payment_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| amount_cents | BIGINT | 否 |  | 金额（马币分） |
| status | VARCHAR(160) | 否 |  | 业务状态（见表约束及口径） |

约束及索引：
```sql
CREATE TABLE payments (
  payment_id BIGINT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  payment_date DATE NOT NULL,
  amount_cents BIGINT NOT NULL CHECK(amount_cents>=0),
  status VARCHAR(160) NOT NULL CHECK(status IN ('settled','failed')),
  FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_payments_order_id ON payments(order_id);
CREATE INDEX ix_payments_payment_date ON payments(payment_date)
```

## sales_targets · 月度门店净销售目标（432 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| target_id | BIGINT | 否 | PK  | 主键/标识 |
| store_id | BIGINT | 否 | stores.store_id | 主键/标识 |
| month_key | CHAR(7) | 否 |  | 统计月份 YYYY-MM |
| target_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE sales_targets (
  target_id BIGINT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  month_key CHAR(7) NOT NULL,
  target_cents BIGINT NOT NULL CHECK(target_cents>=0),
  UNIQUE(store_id,month_key),
  FOREIGN KEY (store_id) REFERENCES stores(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_sales_targets_store_id ON sales_targets(store_id)
```

## supplier_contracts · 年度采购合同价格（3000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| contract_id | BIGINT | 否 | PK  | 主键/标识 |
| supplier_id | BIGINT | 否 | suppliers.supplier_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| start_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| end_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| unit_price_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE supplier_contracts (
  contract_id BIGINT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  unit_price_cents BIGINT NOT NULL CHECK(unit_price_cents>=0),
  UNIQUE(supplier_id,product_id,start_date),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_supplier_contracts_end_date ON supplier_contracts(end_date);
CREATE INDEX ix_supplier_contracts_product_id ON supplier_contracts(product_id);
CREATE INDEX ix_supplier_contracts_start_date ON supplier_contracts(start_date);
CREATE INDEX ix_supplier_contracts_supplier_id ON supplier_contracts(supplier_id)
```

## purchase_orders · 采购订单（5000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| po_id | BIGINT | 否 | PK  | 主键/标识 |
| supplier_id | BIGINT | 否 | suppliers.supplier_id | 主键/标识 |
| warehouse_id | BIGINT | 否 | warehouses.warehouse_id | 主键/标识 |
| order_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| promised_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| status | VARCHAR(160) | 否 |  | 业务状态（见表约束及口径） |

约束及索引：
```sql
CREATE TABLE purchase_orders (
  po_id BIGINT PRIMARY KEY,
  supplier_id BIGINT NOT NULL,
  warehouse_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  promised_date DATE NOT NULL,
  status VARCHAR(160) NOT NULL CHECK(status IN ('placed','cancelled','closed')),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_purchase_orders_order_date ON purchase_orders(order_date);
CREATE INDEX ix_purchase_orders_promised_date ON purchase_orders(promised_date);
CREATE INDEX ix_purchase_orders_supplier_id ON purchase_orders(supplier_id);
CREATE INDEX ix_purchase_orders_warehouse_id ON purchase_orders(warehouse_id)
```

## purchase_order_items · 采购明细（9993 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| po_item_id | BIGINT | 否 | PK  | 主键/标识 |
| po_id | BIGINT | 否 | purchase_orders.po_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| contract_id | BIGINT | 否 | supplier_contracts.contract_id | 主键/标识 |
| quantity_ordered | INT | 否 |  | 订购件数 |
| unit_price_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE purchase_order_items (
  po_item_id BIGINT PRIMARY KEY,
  po_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  contract_id BIGINT NOT NULL,
  quantity_ordered INT NOT NULL CHECK(quantity_ordered>0),
  unit_price_cents BIGINT NOT NULL CHECK(unit_price_cents>=0),
  FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id),
  FOREIGN KEY (contract_id) REFERENCES supplier_contracts(contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_purchase_order_items_contract_id ON purchase_order_items(contract_id);
CREATE INDEX ix_purchase_order_items_po_id ON purchase_order_items(po_id);
CREATE INDEX ix_purchase_order_items_product_id ON purchase_order_items(product_id)
```

## goods_receipts · 收货与质检数量（11103 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| receipt_id | BIGINT | 否 | PK  | 主键/标识 |
| po_item_id | BIGINT | 否 | purchase_order_items.po_item_id | 主键/标识 |
| receipt_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| quantity_received | INT | 否 |  | 实际收货件数（含拒收） |
| quantity_rejected | INT | 否 |  | 拒收件数 |

约束及索引：
```sql
CREATE TABLE goods_receipts (
  receipt_id BIGINT PRIMARY KEY,
  po_item_id BIGINT NOT NULL,
  receipt_date DATE NOT NULL,
  quantity_received INT NOT NULL CHECK(quantity_received>0),
  quantity_rejected INT NOT NULL CHECK(quantity_rejected>=0),
  CHECK(quantity_rejected<=quantity_received),
  FOREIGN KEY (po_item_id) REFERENCES purchase_order_items(po_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_goods_receipts_po_item_id ON goods_receipts(po_item_id);
CREATE INDEX ix_goods_receipts_receipt_date ON goods_receipts(receipt_date)
```

## supplier_invoices · 供应商发票头（4658 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| supplier_invoice_id | BIGINT | 否 | PK  | 主键/标识 |
| po_id | BIGINT | 否 | purchase_orders.po_id | 主键/标识 |
| invoice_no | VARCHAR(160) | 否 |  | 供应商发票业务编号（可重复异常） |
| invoice_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| due_date | DATE | 否 |  | 日期（YYYY-MM-DD） |

约束及索引：
```sql
CREATE TABLE supplier_invoices (
  supplier_invoice_id BIGINT PRIMARY KEY,
  po_id BIGINT NOT NULL,
  invoice_no VARCHAR(160) NOT NULL,
  invoice_date DATE NOT NULL,
  due_date DATE NOT NULL,
  FOREIGN KEY (po_id) REFERENCES purchase_orders(po_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_supplier_invoices_due_date ON supplier_invoices(due_date);
CREATE INDEX ix_supplier_invoices_invoice_date ON supplier_invoices(invoice_date);
CREATE INDEX ix_supplier_invoices_po_id ON supplier_invoices(po_id)
```

## supplier_invoice_items · 供应商发票明细（9266 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| supplier_invoice_item_id | BIGINT | 否 | PK  | 主键/标识 |
| supplier_invoice_id | BIGINT | 否 | supplier_invoices.supplier_invoice_id | 主键/标识 |
| po_item_id | BIGINT | 否 | purchase_order_items.po_item_id | 主键/标识 |
| quantity_billed | INT | 否 |  | 开票件数 |
| unit_price_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE supplier_invoice_items (
  supplier_invoice_item_id BIGINT PRIMARY KEY,
  supplier_invoice_id BIGINT NOT NULL,
  po_item_id BIGINT NOT NULL,
  quantity_billed INT NOT NULL CHECK(quantity_billed>0),
  unit_price_cents BIGINT NOT NULL CHECK(unit_price_cents>=0),
  FOREIGN KEY (supplier_invoice_id) REFERENCES supplier_invoices(supplier_invoice_id),
  FOREIGN KEY (po_item_id) REFERENCES purchase_order_items(po_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_supplier_invoice_items_po_item_id ON supplier_invoice_items(po_item_id);
CREATE INDEX ix_supplier_invoice_items_supplier_invoice_id ON supplier_invoice_items(supplier_invoice_id)
```

## supplier_payments · 供应商付款（4076 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| supplier_payment_id | BIGINT | 否 | PK  | 主键/标识 |
| supplier_invoice_id | BIGINT | 否 | supplier_invoices.supplier_invoice_id | 主键/标识 |
| payment_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| amount_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE supplier_payments (
  supplier_payment_id BIGINT PRIMARY KEY,
  supplier_invoice_id BIGINT NOT NULL,
  payment_date DATE NOT NULL,
  amount_cents BIGINT NOT NULL CHECK(amount_cents>=0),
  FOREIGN KEY (supplier_invoice_id) REFERENCES supplier_invoices(supplier_invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_supplier_payments_payment_date ON supplier_payments(payment_date);
CREATE INDEX ix_supplier_payments_supplier_invoice_id ON supplier_payments(supplier_invoice_id)
```

## inventory_opening · 2023年初库存（1500 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| opening_id | BIGINT | 否 | PK  | 主键/标识 |
| warehouse_id | BIGINT | 否 | warehouses.warehouse_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| quantity | INT | 否 |  | 数量（件） |

约束及索引：
```sql
CREATE TABLE inventory_opening (
  opening_id BIGINT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL CHECK(quantity>=0),
  UNIQUE(warehouse_id,product_id),
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_inventory_opening_product_id ON inventory_opening(product_id);
CREATE INDEX ix_inventory_opening_warehouse_id ON inventory_opening(warehouse_id)
```

## stock_movements · 可售库存入出流水（99369 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| movement_id | BIGINT | 否 | PK  | 主键/标识 |
| warehouse_id | BIGINT | 否 | warehouses.warehouse_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| movement_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| movement_type | VARCHAR(160) | 否 |  | receipt 收货 / return 退货 / sale 销售 |
| quantity_delta | INT | 否 |  | 库存变化数量（入库正、出库负） |
| unit_cost_cents | BIGINT | 否 |  | 金额（马币分） |
| order_item_id | BIGINT | 是 | order_items.order_item_id | 主键/标识 |
| receipt_id | BIGINT | 是 | goods_receipts.receipt_id | 主键/标识 |

约束及索引：
```sql
CREATE TABLE stock_movements (
  movement_id BIGINT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  movement_date DATE NOT NULL,
  movement_type VARCHAR(160) NOT NULL,
  quantity_delta INT NOT NULL,
  unit_cost_cents BIGINT NOT NULL CHECK(unit_cost_cents>=0),
  order_item_id BIGINT,
  receipt_id BIGINT,
  CHECK((movement_type IN ('receipt','return') AND quantity_delta>0) OR (movement_type='sale' AND quantity_delta<0)),
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id),
  FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
  FOREIGN KEY (receipt_id) REFERENCES goods_receipts(receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_stock_movements_movement_date ON stock_movements(movement_date);
CREATE INDEX ix_stock_movements_order_item_id ON stock_movements(order_item_id);
CREATE INDEX ix_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX ix_stock_movements_receipt_id ON stock_movements(receipt_id);
CREATE INDEX ix_stock_movements_warehouse_id ON stock_movements(warehouse_id)
```

## inventory_snapshots · 月末库存及标准成本（54000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| snapshot_id | BIGINT | 否 | PK  | 主键/标识 |
| warehouse_id | BIGINT | 否 | warehouses.warehouse_id | 主键/标识 |
| product_id | BIGINT | 否 | products.product_id | 主键/标识 |
| snapshot_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| quantity | INT | 否 |  | 数量（件） |
| unit_cost_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE inventory_snapshots (
  snapshot_id BIGINT PRIMARY KEY,
  warehouse_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  snapshot_date DATE NOT NULL,
  quantity INT NOT NULL CHECK(quantity>=0),
  unit_cost_cents BIGINT NOT NULL CHECK(unit_cost_cents>=0),
  UNIQUE(warehouse_id,product_id,snapshot_date),
  FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
  FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_inventory_snapshots_product_id ON inventory_snapshots(product_id);
CREATE INDEX ix_inventory_snapshots_snapshot_date ON inventory_snapshots(snapshot_date);
CREATE INDEX ix_inventory_snapshots_warehouse_id ON inventory_snapshots(warehouse_id)
```

## accounts · 财务科目（6 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| account_id | BIGINT | 否 | PK  | 主键/标识 |
| account_name | VARCHAR(160) | 否 |  | 名称 |
| account_type | VARCHAR(160) | 否 |  | expense 费用 / cash 银行存款 |

约束及索引：
```sql
CREATE TABLE accounts (
  account_id BIGINT PRIMARY KEY,
  account_name VARCHAR(160) NOT NULL,
  account_type VARCHAR(160) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## journal_entries · 费用凭证头（18000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| entry_id | BIGINT | 否 | PK  | 主键/标识 |
| entry_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| department_id | BIGINT | 否 | departments.department_id | 主键/标识 |
| store_id | BIGINT | 是 | stores.store_id | 主键/标识 |
| reference_no | VARCHAR(160) | 否 |  | 对账参考编号 |

约束及索引：
```sql
CREATE TABLE journal_entries (
  entry_id BIGINT PRIMARY KEY,
  entry_date DATE NOT NULL,
  department_id BIGINT NOT NULL,
  store_id BIGINT,
  reference_no VARCHAR(160) NOT NULL,
  FOREIGN KEY (department_id) REFERENCES departments(department_id),
  FOREIGN KEY (store_id) REFERENCES stores(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_journal_entries_department_id ON journal_entries(department_id);
CREATE INDEX ix_journal_entries_entry_date ON journal_entries(entry_date);
CREATE INDEX ix_journal_entries_store_id ON journal_entries(store_id)
```

## journal_lines · 借贷分录（36000 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| line_id | BIGINT | 否 | PK  | 主键/标识 |
| entry_id | BIGINT | 否 | journal_entries.entry_id | 主键/标识 |
| account_id | BIGINT | 否 | accounts.account_id | 主键/标识 |
| debit_cents | BIGINT | 否 |  | 金额（马币分） |
| credit_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE journal_lines (
  line_id BIGINT PRIMARY KEY,
  entry_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  debit_cents BIGINT NOT NULL CHECK(debit_cents>=0),
  credit_cents BIGINT NOT NULL CHECK(credit_cents>=0),
  CHECK((debit_cents=0 AND credit_cents>0) OR (credit_cents=0 AND debit_cents>0)),
  FOREIGN KEY (entry_id) REFERENCES journal_entries(entry_id),
  FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_journal_lines_account_id ON journal_lines(account_id);
CREATE INDEX ix_journal_lines_entry_id ON journal_lines(entry_id)
```

## bank_transactions · 银行流水（16868 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| bank_id | BIGINT | 否 | PK  | 主键/标识 |
| transaction_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| reference_no | VARCHAR(160) | 是 |  | 对账参考编号 |
| amount_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE bank_transactions (
  bank_id BIGINT PRIMARY KEY,
  transaction_date DATE NOT NULL,
  reference_no VARCHAR(160),
  amount_cents BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_bank_transactions_transaction_date ON bank_transactions(transaction_date)
```

## budgets · 部门科目预算（1260 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| budget_id | BIGINT | 否 | PK  | 主键/标识 |
| department_id | BIGINT | 否 | departments.department_id | 主键/标识 |
| account_id | BIGINT | 否 | accounts.account_id | 主键/标识 |
| month_key | CHAR(7) | 否 |  | 统计月份 YYYY-MM |
| budget_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE budgets (
  budget_id BIGINT PRIMARY KEY,
  department_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  month_key CHAR(7) NOT NULL,
  budget_cents BIGINT NOT NULL CHECK(budget_cents>=0),
  UNIQUE(department_id,account_id,month_key),
  FOREIGN KEY (department_id) REFERENCES departments(department_id),
  FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_budgets_account_id ON budgets(account_id);
CREATE INDEX ix_budgets_department_id ON budgets(department_id)
```

## positions · 职位职级（5 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| position_id | BIGINT | 否 | PK  | 主键/标识 |
| position_name | VARCHAR(160) | 否 |  | 名称 |
| grade | INT | 否 |  | 职级 |

约束及索引：
```sql
CREATE TABLE positions (
  position_id BIGINT PRIMARY KEY,
  position_name VARCHAR(160) NOT NULL,
  grade INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

```

## employees · 虚构员工（250 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| employee_id | BIGINT | 否 | PK  | 主键/标识 |
| employee_name | VARCHAR(160) | 否 |  | 名称 |
| hire_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| termination_date | DATE | 是 |  | 日期（YYYY-MM-DD） |

约束及索引：
```sql
CREATE TABLE employees (
  employee_id BIGINT PRIMARY KEY,
  employee_name VARCHAR(160) NOT NULL,
  hire_date DATE NOT NULL,
  termination_date DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_employees_hire_date ON employees(hire_date);
CREATE INDEX ix_employees_termination_date ON employees(termination_date)
```

## employment_history · 历史岗位和门店（307 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| employment_id | BIGINT | 否 | PK  | 主键/标识 |
| employee_id | BIGINT | 否 | employees.employee_id | 主键/标识 |
| department_id | BIGINT | 否 | departments.department_id | 主键/标识 |
| position_id | BIGINT | 否 | positions.position_id | 主键/标识 |
| store_id | BIGINT | 是 | stores.store_id | 主键/标识 |
| start_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| end_date | DATE | 是 |  | 日期（YYYY-MM-DD） |

约束及索引：
```sql
CREATE TABLE employment_history (
  employment_id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  position_id BIGINT NOT NULL,
  store_id BIGINT,
  start_date DATE NOT NULL,
  end_date DATE,
  FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
  FOREIGN KEY (department_id) REFERENCES departments(department_id),
  FOREIGN KEY (position_id) REFERENCES positions(position_id),
  FOREIGN KEY (store_id) REFERENCES stores(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_employment_history_department_id ON employment_history(department_id);
CREATE INDEX ix_employment_history_employee_id ON employment_history(employee_id);
CREATE INDEX ix_employment_history_end_date ON employment_history(end_date);
CREATE INDEX ix_employment_history_position_id ON employment_history(position_id);
CREATE INDEX ix_employment_history_start_date ON employment_history(start_date);
CREATE INDEX ix_employment_history_store_id ON employment_history(store_id)
```

## salary_history · 员工月薪（4966 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| salary_id | BIGINT | 否 | PK  | 主键/标识 |
| employee_id | BIGINT | 否 | employees.employee_id | 主键/标识 |
| employment_id | BIGINT | 否 | employment_history.employment_id | 主键/标识 |
| month_key | CHAR(7) | 否 |  | 统计月份 YYYY-MM |
| base_cents | BIGINT | 否 |  | 金额（马币分） |
| bonus_cents | BIGINT | 否 |  | 金额（马币分） |

约束及索引：
```sql
CREATE TABLE salary_history (
  salary_id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  employment_id BIGINT NOT NULL,
  month_key CHAR(7) NOT NULL,
  base_cents BIGINT NOT NULL CHECK(base_cents>=0),
  bonus_cents BIGINT NOT NULL CHECK(bonus_cents>=0),
  UNIQUE(employee_id,month_key),
  FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
  FOREIGN KEY (employment_id) REFERENCES employment_history(employment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_salary_history_employee_id ON salary_history(employee_id);
CREATE INDEX ix_salary_history_employment_id ON salary_history(employment_id)
```

## attendance · 出勤及加班（104754 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| attendance_id | BIGINT | 否 | PK  | 主键/标识 |
| employee_id | BIGINT | 否 | employees.employee_id | 主键/标识 |
| attendance_date | DATE | 否 |  | 日期（YYYY-MM-DD） |
| status | VARCHAR(160) | 否 |  | 业务状态（见表约束及口径） |
| work_minutes | INT | 否 |  | 正常出勤分钟 |
| overtime_minutes | INT | 否 |  | 加班分钟 |

约束及索引：
```sql
CREATE TABLE attendance (
  attendance_id BIGINT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  attendance_date DATE NOT NULL,
  status VARCHAR(160) NOT NULL,
  work_minutes INT NOT NULL CHECK(work_minutes>=0),
  overtime_minutes INT NOT NULL CHECK(overtime_minutes>=0),
  UNIQUE(employee_id,attendance_date),
  FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_attendance_attendance_date ON attendance(attendance_date);
CREATE INDEX ix_attendance_employee_id ON attendance(employee_id)
```

## raw_order_import · 原始导入脏数据隔离区（400 行）

| 字段 | MySQL 类型 | 可空 | 键 | 说明 |
|---|---|---|---|---|
| raw_id | BIGINT | 否 | PK  | 主键/标识 |
| source_ref | VARCHAR(160) | 是 |  | 原始导入参考号 |
| raw_date | VARCHAR(160) | 是 |  | 未经清洗日期 |
| raw_customer | VARCHAR(160) | 是 |  | 未经清洗客户文本 |
| raw_amount | VARCHAR(160) | 是 |  | 未经清洗金额文本 |

约束及索引：
```sql
CREATE TABLE raw_order_import (
  raw_id BIGINT PRIMARY KEY,
  source_ref VARCHAR(160),
  raw_date VARCHAR(160),
  raw_customer VARCHAR(160),
  raw_amount VARCHAR(160)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
CREATE INDEX ix_raw_order_import_raw_date ON raw_order_import(raw_date)
```

## v_sales_lines（派生视图）
```sql
CREATE VIEW v_sales_lines AS SELECT i.order_item_id,o.order_id,o.store_id,o.customer_id,o.session_id,o.channel,o.order_date,i.product_id,i.promotion_id,i.quantity-COALESCE(r.qty,0) AS net_qty,i.quantity*i.unit_price_cents-i.discount_cents-COALESCE(r.refund,0) AS net_cents,(i.quantity-COALESCE(r.qty,0))*i.unit_cost_cents AS cost_cents,i.discount_cents,COALESCE(r.refund,0) AS refund_cents FROM orders o JOIN order_items i ON o.order_id=i.order_id LEFT JOIN (SELECT order_item_id,SUM(quantity) qty,SUM(refund_cents) refund FROM returns WHERE return_date<='2025-12-31' GROUP BY order_item_id) r ON r.order_item_id=i.order_item_id WHERE o.status='completed';
```

## v_order_net（派生视图）
```sql
CREATE VIEW v_order_net AS SELECT order_id,store_id,customer_id,session_id,channel,order_date,SUM(net_cents) net_cents,SUM(cost_cents) cost_cents FROM v_sales_lines GROUP BY order_id,store_id,customer_id,session_id,channel,order_date;
```

## v_po_lines（派生视图）
```sql
CREATE VIEW v_po_lines AS SELECT i.po_item_id,i.po_id,p.supplier_id,p.warehouse_id,p.order_date,p.promised_date,i.product_id,i.quantity_ordered,i.unit_price_cents,i.quantity_ordered*i.unit_price_cents AS ordered_cents,COALESCE(r.received,0) received,COALESCE(r.accepted,0) accepted,COALESCE(r.on_time,0) on_time,COALESCE(b.billed,0) billed,COALESCE(b.billed_cents,0) billed_cents FROM purchase_order_items i JOIN purchase_orders p ON p.po_id=i.po_id LEFT JOIN (SELECT g.po_item_id,SUM(g.quantity_received) received,SUM(g.quantity_received-g.quantity_rejected) accepted,SUM(CASE WHEN g.receipt_date<=p.promised_date THEN g.quantity_received-g.quantity_rejected ELSE 0 END) on_time FROM goods_receipts g JOIN purchase_order_items i ON i.po_item_id=g.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id WHERE g.receipt_date<='2025-12-31' GROUP BY g.po_item_id) r ON r.po_item_id=i.po_item_id LEFT JOIN (SELECT po_item_id,SUM(quantity_billed) billed,SUM(quantity_billed*unit_price_cents) billed_cents FROM supplier_invoice_items GROUP BY po_item_id) b ON b.po_item_id=i.po_item_id WHERE p.status<>'cancelled';
```

## v_invoice_balance（派生视图）
```sql
CREATE VIEW v_invoice_balance AS SELECT i.supplier_invoice_id,i.po_id,p.supplier_id,i.invoice_no,i.invoice_date,i.due_date,b.billed_cents,COALESCE(a.paid_cents,0) paid_cents,b.billed_cents-COALESCE(a.paid_cents,0) balance_cents FROM supplier_invoices i JOIN purchase_orders p ON p.po_id=i.po_id JOIN (SELECT supplier_invoice_id,SUM(quantity_billed*unit_price_cents) billed_cents FROM supplier_invoice_items GROUP BY supplier_invoice_id) b ON b.supplier_invoice_id=i.supplier_invoice_id LEFT JOIN (SELECT supplier_invoice_id,SUM(amount_cents) paid_cents FROM supplier_payments WHERE payment_date<='2025-12-31' GROUP BY supplier_invoice_id) a ON a.supplier_invoice_id=i.supplier_invoice_id WHERE i.invoice_date<='2025-12-31';
```

## v_expenses（派生视图）
```sql
CREATE VIEW v_expenses AS SELECT e.entry_id,e.entry_date,e.department_id,e.store_id,e.reference_no,l.account_id,l.debit_cents-l.credit_cents AS expense_cents FROM journal_entries e JOIN journal_lines l ON l.entry_id=e.entry_id JOIN accounts a ON a.account_id=l.account_id WHERE a.account_type='expense';
```

## v_staff_month（派生视图）
```sql
CREATE VIEW v_staff_month AS SELECT s.employee_id,s.month_key,h.department_id,h.position_id,h.store_id,s.base_cents,s.bonus_cents,COALESCE(a.ot,0) overtime_minutes,COALESCE(a.worked,0) worked_minutes,s.base_cents+s.bonus_cents+ROUND(s.base_cents*1.5*COALESCE(a.ot,0)/10560.0,0) AS labor_cents FROM salary_history s JOIN employment_history h ON h.employment_id=s.employment_id LEFT JOIN (SELECT employee_id,SUBSTR(attendance_date,1,7) m,SUM(overtime_minutes) ot,SUM(work_minutes) worked FROM attendance GROUP BY employee_id,SUBSTR(attendance_date,1,7)) a ON a.employee_id=s.employee_id AND a.m=s.month_key;
```
