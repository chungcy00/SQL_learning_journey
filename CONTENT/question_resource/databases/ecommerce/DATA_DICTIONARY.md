# ecommerce 数据字典

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
