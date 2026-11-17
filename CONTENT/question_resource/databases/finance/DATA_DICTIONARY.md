# finance 数据字典

金额字段 `_cents` 为整数马币分，展示层除以100。日期范围2023—2025，部分承诺/到期日期延至2026。

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
