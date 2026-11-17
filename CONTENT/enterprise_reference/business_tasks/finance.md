# finance 实战任务

## BP16 预算与实际费用差异（中等）

财务负责人检查部门年度超支。

**要求：** 2025年部门×费用科目，预算与费用分录各自汇总，偏差=实际-预算；超过预算10%标记；保留有预算但没有发生额的项目；分母为零返回NULL。

**输出列：** department_id, account_id, budget_cents, actual_cents, variance_cents, variance_pct, over_budget

**思路：** 预算单独聚合；费用独立聚合；合并并计算差异。预算按月存在，连接分录后再求和会放大预算。

```sql
WITH b AS (SELECT department_id,account_id,SUM(budget_cents) budget FROM budgets WHERE month_key BETWEEN '2025-01' AND '2025-12' GROUP BY department_id,account_id), a AS (SELECT department_id,account_id,SUM(expense_cents) actual FROM v_expenses WHERE entry_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY department_id,account_id) SELECT b.department_id,b.account_id,b.budget budget_cents,COALESCE(a.actual,0) actual_cents,COALESCE(a.actual,0)-b.budget variance_cents,ROUND(100.0*(COALESCE(a.actual,0)-b.budget)/NULLIF(b.budget,0),2) variance_pct,CASE WHEN COALESCE(a.actual,0)>b.budget*1.1 THEN 1 ELSE 0 END over_budget FROM b LEFT JOIN a ON a.department_id=b.department_id AND a.account_id=b.account_id ORDER BY b.department_id,b.account_id;
```

真实执行结果：35 行；完整结果 `results/BP16.json`。

## BP17 供应商发票与付款核对（困难）

财务人员需要年底未付及异常付款清单。

**要求：** 所有截至12-31的供应商发票，汇总付款；输出发票金额、实付金额、余额和未付/部分/足额/超额状态。按余额倒序。

**输出列：** supplier_invoice_id, supplier_id, billed_cents, paid_cents, balance_cents, payment_state

**思路：** 先检查发票行与付款的预聚合视图；比较金额与余额；分类。不能直接将多笔付款与多行发票明细同时连接后求和。

```sql
SELECT supplier_invoice_id,supplier_id,billed_cents,paid_cents,balance_cents,CASE WHEN paid_cents=0 THEN 'unpaid' WHEN balance_cents>0 THEN 'partial' WHEN balance_cents=0 THEN 'paid' ELSE 'overpaid' END payment_state FROM v_invoice_balance ORDER BY balance_cents DESC,supplier_invoice_id;
```

真实执行结果：4658 行；完整结果 `results/BP17.json`。

## BP18 应付账款账龄（困难）

财务负责人评估逾期支付压力。

**要求：** 截至2025-12-31，余额>0；按到期日分为未逾期（到期日≥截止日）、1—30、31—60、61—90、90日以上；按供应商与账龄汇总发票数及应付余额。日期差用日历索引；到期日可能超出年底，先判未逾期。

**输出列：** supplier_id, bucket, invoices, outstanding_cents

**思路：** 先取截止日余额；按固定日期边界分箱；聚合供应商应付。账龄基于到期日，不是发票日期；未到期不能算逾期。

```sql
WITH a AS (SELECT v.*,CASE WHEN due_date>='2025-12-31' THEN 'current' WHEN due_date>='2025-12-01' THEN '01-30' WHEN due_date>='2025-11-01' THEN '31-60' WHEN due_date>='2025-10-02' THEN '61-90' ELSE '90+' END bucket FROM v_invoice_balance v WHERE balance_cents>0) SELECT supplier_id,bucket,COUNT(*) invoices,SUM(balance_cents) outstanding_cents FROM a GROUP BY supplier_id,bucket ORDER BY supplier_id,bucket;
```

真实执行结果：197 行；完整结果 `results/BP18.json`。

## BP19 银行流水与凭证全量对账（困难）

审计人员需要同时寻找银行端和账簿端的未匹配。

**要求：** 2025年费用凭证（按reference_no）与银行支出绝对值（按reference_no）分别汇总；采用全量键集合；空银行参考号使用每笔流水独立键；输出存在缺失或差异的记录及差额（银行-账簿）。

**输出列：** ref, ledger_cents, bank_cents, difference_cents, issue

**思路：** 按两端参考号汇总；构造全量键；左连接两端；筛选差异。内连接无法找出缺失记录；NULL参考号不可合并成一笔。

```sql
WITH a AS (SELECT reference_no ref,SUM(expense_cents) ledger FROM v_expenses WHERE entry_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY reference_no), b AS (SELECT COALESCE(reference_no,CONCAT('BANK-',bank_id)) ref,SUM(-amount_cents) bank FROM bank_transactions WHERE transaction_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY COALESCE(reference_no,CONCAT('BANK-',bank_id))), k AS (SELECT ref FROM a UNION SELECT ref FROM b) SELECT k.ref,a.ledger ledger_cents,b.bank bank_cents,COALESCE(b.bank,0)-COALESCE(a.ledger,0) difference_cents,CASE WHEN a.ref IS NULL THEN 'bank_only' WHEN b.ref IS NULL THEN 'ledger_only' ELSE 'difference' END issue FROM k LEFT JOIN a ON a.ref=k.ref LEFT JOIN b ON b.ref=k.ref WHERE a.ref IS NULL OR b.ref IS NULL OR a.ledger<>b.bank ORDER BY k.ref;
```

真实执行结果：656 行；完整结果 `results/BP19.json`。

## BP20 多规则异常交易筛查（挑战）

审计部门需要异常线索，不用于认定违规。

**要求：** 发票粒度；同一供应商同编号重复、金额超过供应商平均发票金额2倍、超额付款、账单金额与对应采购行按收货计价相差超过100分；每条发票计算异常数，至少一个标记。发票单价或数量差异需复核。

**输出列：** supplier_invoice_id, supplier_id, billed_cents, duplicate_flag, large_flag, overpay_flag, mismatch_flag, flags

**思路：** 窗口计算供应商基准；独立汇总收货金额；构造四个标志；输出候选。对每张发票使用整个采购单金额只有本模拟的一单一正常发票假设成立。

```sql
WITH a AS (SELECT v.*,COUNT(*) OVER(PARTITION BY supplier_id,invoice_no) copies,AVG(billed_cents) OVER(PARTITION BY supplier_id) avg_bill FROM v_invoice_balance v), b AS (SELECT po_id,SUM(accepted*unit_price_cents) expected FROM v_po_lines GROUP BY po_id), z AS (SELECT a.*,CASE WHEN copies>1 THEN 1 ELSE 0 END duplicate_flag,CASE WHEN billed_cents>2*avg_bill THEN 1 ELSE 0 END large_flag,CASE WHEN balance_cents<0 THEN 1 ELSE 0 END overpay_flag,CASE WHEN ABS(billed_cents-COALESCE(b.expected,0))>100 THEN 1 ELSE 0 END mismatch_flag FROM a LEFT JOIN b ON b.po_id=a.po_id) SELECT supplier_invoice_id,supplier_id,billed_cents,duplicate_flag,large_flag,overpay_flag,mismatch_flag,duplicate_flag+large_flag+overpay_flag+mismatch_flag flags FROM z WHERE duplicate_flag+large_flag+overpay_flag+mismatch_flag>0 ORDER BY flags DESC,supplier_invoice_id;
```

真实执行结果：1090 行；完整结果 `results/BP20.json`。

