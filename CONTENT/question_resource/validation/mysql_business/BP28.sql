-- 采购、收货、发票三方核对
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
SELECT po_item_id,po_id,supplier_id,product_id,quantity_ordered,accepted,billed,billed_cents,accepted*unit_price_cents expected_cents,billed_cents-accepted*unit_price_cents variance_cents FROM v_po_lines WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' AND (quantity_ordered<>accepted OR accepted<>billed OR billed_cents<>accepted*unit_price_cents) ORDER BY po_item_id;

-- 等价解（SQLite已验证，MySQL待复验）
SELECT po_item_id,po_id,supplier_id,product_id,quantity_ordered,accepted,billed,billed_cents,accepted*unit_price_cents expected_cents,billed_cents-accepted*unit_price_cents variance_cents FROM v_po_lines WHERE (order_date>='2025-01-01' AND order_date<='2025-12-31') AND (quantity_ordered<>accepted OR accepted<>billed OR billed_cents<>accepted*unit_price_cents) ORDER BY po_item_id;
