import sqlite3,random,datetime as dt, pathlib as P, json,csv,hashlib,shutil,calendar,collections, time
R=P.Path('/mnt/data/enterprise_sql_upgrade'); D=R/'databases'; D.mkdir(exist_ok=True); random.seed(20261117); r=random; start=dt.date(2023,1,1); end=dt.date(2025,12,31); cutoff='2025-12-31'; dates=[str(start+dt.timedelta(days=i)) for i in range((end-start).days+1)]; months=sorted(set(x[:7] for x in dates)); monthends=[m+'-'+str(calendar.monthrange(*map(int,m.split('-')))[1]).zfill(2) for m in months]; db=D/'enterprise'; db.mkdir(exist_ok=True); c=sqlite3.connect(db/'enterprise.sqlite'); c.execute('PRAGMA foreign_keys=ON'); schema={}; desc={}; rows={}; views={}; groups={}
def table(n,fields,description,group,extra=''):
 schema[n]=(fields,extra);desc[n]=description;groups[n]=group;rows[n]=[];c.execute(f'CREATE TABLE {n} ({fields}{", "+extra if extra else ""})')
def add(n,*v):rows[n].append(v)
def ins(n):
 if rows[n]:c.executemany(f'INSERT INTO {n} VALUES ({",".join("?" for _ in rows[n][0])})',rows[n])
def date_after(d,k):return str(dt.date.fromisoformat(d)+dt.timedelta(days=k))
def cents(x):return int(round(x))
def key(n):return f'{n}_id INTEGER PRIMARY KEY'
def fk(n,t,col=None,nullable=False):return f'{n} INTEGER {"" if nullable else "NOT NULL "}REFERENCES {t}({col or n})'
def pos(n):return f'{n} INTEGER NOT NULL CHECK({n}>=0)'
table('calendar_days','day TEXT PRIMARY KEY, month_key TEXT NOT NULL, day_index INTEGER NOT NULL UNIQUE','日历日期、月份和连续日序号','shared')
table('stores',key('store')+', store_name TEXT NOT NULL, city TEXT NOT NULL','门店','shared')
table('categories',key('category')+', category_name TEXT NOT NULL','商品分类','shared')
table('products',key('product')+', '+fk('category_id','categories')+', product_name TEXT NOT NULL, '+pos('list_price_cents')+', '+pos('reorder_level')+', active INTEGER NOT NULL CHECK(active IN (0,1))','商品主档；金额为分','shared')
table('customers',key('customer')+', customer_name TEXT NOT NULL, signup_date TEXT NOT NULL, region TEXT','虚构客户；地区允许空值','shared')
table('suppliers',key('supplier')+', supplier_name TEXT NOT NULL, region TEXT NOT NULL','供应商主档','shared')
table('warehouses',key('warehouse')+', warehouse_name TEXT NOT NULL','仓库主档','shared')
table('departments',key('department')+', department_name TEXT NOT NULL','部门主档','shared')
table('product_costs',key('cost')+', '+fk('product_id','products')+', effective_date TEXT NOT NULL, '+pos('unit_cost_cents'),'商品生效成本历史','shared','UNIQUE(product_id,effective_date)')
table('promotions',key('promotion')+', promotion_name TEXT NOT NULL, start_date TEXT NOT NULL, end_date TEXT NOT NULL','促销日期区间（含两端）','retail')
table('traffic_sources',key('source')+', source_name TEXT NOT NULL','电商营销渠道','ecommerce')
table('marketing_campaigns',key('campaign')+', '+fk('source_id','traffic_sources')+', month_key TEXT NOT NULL, '+pos('spend_cents'),'渠道每月广告支出','ecommerce','UNIQUE(source_id,month_key)')
table('customer_sessions',key('session')+', '+fk('customer_id','customers')+', '+fk('source_id','traffic_sources')+', session_date TEXT NOT NULL','独立访问会话；订单可关联一个会话','ecommerce')
table('orders',key('order')+', '+fk('store_id','stores',nullable=True)+', '+fk('customer_id','customers',nullable=True)+', '+fk('session_id','customer_sessions',nullable=True)+', order_date TEXT NOT NULL, channel TEXT NOT NULL CHECK(channel IN (\'store\',\'online\')), status TEXT NOT NULL CHECK(status IN (\'completed\',\'cancelled\',\'pending\'))','销售订单：线上无门店，线下可匿名','sales','CHECK((channel=\'online\' AND store_id IS NULL AND session_id IS NOT NULL) OR (channel=\'store\' AND store_id IS NOT NULL AND session_id IS NULL))')
table('order_items',key('order_item')+', '+fk('order_id','orders')+', '+fk('product_id','products')+', '+fk('promotion_id','promotions',nullable=True)+', quantity INTEGER NOT NULL CHECK(quantity>0), '+pos('unit_price_cents')+', '+pos('discount_cents')+', '+pos('unit_cost_cents'),'销售明细；成交时成本固定；折扣为整行','sales','CHECK(discount_cents<=quantity*unit_price_cents)')
table('returns',key('return')+', '+fk('order_item_id','order_items')+', return_date TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), '+pos('refund_cents'),'退货与退款；退回库存，退款按原订单归属分析','sales')
table('payments',key('payment')+', '+fk('order_id','orders')+', payment_date TEXT NOT NULL, '+pos('amount_cents')+', status TEXT NOT NULL CHECK(status IN (\'settled\',\'failed\'))','订单付款；可拆分付款，失败不计收入','sales')
table('sales_targets',key('target')+', '+fk('store_id','stores')+', month_key TEXT NOT NULL, '+pos('target_cents'),'门店月度净销售目标','retail','UNIQUE(store_id,month_key)')
table('supplier_contracts',key('contract')+', '+fk('supplier_id','suppliers')+', '+fk('product_id','products')+', start_date TEXT NOT NULL, end_date TEXT NOT NULL, '+pos('unit_price_cents'),'采购合同有效期价格','procurement','UNIQUE(supplier_id,product_id,start_date)')
table('purchase_orders',key('po')+', '+fk('supplier_id','suppliers')+', '+fk('warehouse_id','warehouses')+', order_date TEXT NOT NULL, promised_date TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN (\'placed\',\'cancelled\',\'closed\'))','采购订单','procurement')
table('purchase_order_items',key('po_item')+', '+fk('po_id','purchase_orders')+', '+fk('product_id','products')+', '+fk('contract_id','supplier_contracts')+', quantity_ordered INTEGER NOT NULL CHECK(quantity_ordered>0), '+pos('unit_price_cents'),'采购订单行','procurement')
table('goods_receipts',key('receipt')+', '+fk('po_item_id','purchase_order_items')+', receipt_date TEXT NOT NULL, quantity_received INTEGER NOT NULL CHECK(quantity_received>0), '+pos('quantity_rejected'),'收货及质检，拒收不入可售库存','procurement','CHECK(quantity_rejected<=quantity_received)')
table('supplier_invoices',key('supplier_invoice')+', '+fk('po_id','purchase_orders')+', invoice_no TEXT NOT NULL, invoice_date TEXT NOT NULL, due_date TEXT NOT NULL','供应商发票头；编号允许业务异常重复','procurement')
table('supplier_invoice_items',key('supplier_invoice_item')+', '+fk('supplier_invoice_id','supplier_invoices')+', '+fk('po_item_id','purchase_order_items')+', quantity_billed INTEGER NOT NULL CHECK(quantity_billed>0), '+pos('unit_price_cents'),'发票明细；可能与收货数量或采购价不一致','procurement')
table('supplier_payments',key('supplier_payment')+', '+fk('supplier_invoice_id','supplier_invoices')+', payment_date TEXT NOT NULL, '+pos('amount_cents'),'供应商发票付款分配（可多笔）','finance')
table('inventory_opening',key('opening')+', '+fk('warehouse_id','warehouses')+', '+fk('product_id','products')+', '+pos('quantity'),'2023年初库存','inventory','UNIQUE(warehouse_id,product_id)')
table('stock_movements',key('movement')+', '+fk('warehouse_id','warehouses')+', '+fk('product_id','products')+', movement_date TEXT NOT NULL, movement_type TEXT NOT NULL, quantity_delta INTEGER NOT NULL, '+pos('unit_cost_cents')+', '+fk('order_item_id','order_items',nullable=True)+', '+fk('receipt_id','goods_receipts',nullable=True),'入库正、出库负；销售、退货、收货可回溯','inventory','CHECK((movement_type IN (\'receipt\',\'return\') AND quantity_delta>0) OR (movement_type=\'sale\' AND quantity_delta<0))')
table('inventory_snapshots',key('snapshot')+', '+fk('warehouse_id','warehouses')+', '+fk('product_id','products')+', snapshot_date TEXT NOT NULL, '+pos('quantity')+', '+pos('unit_cost_cents'),'月末库存数量、当日标准成本；不等同财务FIFO成本','inventory','UNIQUE(warehouse_id,product_id,snapshot_date)')
table('accounts',key('account')+', account_name TEXT NOT NULL, account_type TEXT NOT NULL','财务费用科目','finance')
table('journal_entries',key('entry')+', entry_date TEXT NOT NULL, '+fk('department_id','departments')+', '+fk('store_id','stores',nullable=True)+', reference_no TEXT NOT NULL','费用凭证头；可与银行流水对账','finance')
table('journal_lines',key('line')+', '+fk('entry_id','journal_entries')+', '+fk('account_id','accounts')+', '+pos('debit_cents')+', '+pos('credit_cents'),'复式记账分录；每凭证借贷相等','finance','CHECK((debit_cents=0 AND credit_cents>0) OR (credit_cents=0 AND debit_cents>0))')
table('bank_transactions',key('bank')+', transaction_date TEXT NOT NULL, reference_no TEXT, amount_cents INTEGER NOT NULL','银行支出流水；负值为支出，含未匹配和差额','finance')
table('budgets',key('budget')+', '+fk('department_id','departments')+', '+fk('account_id','accounts')+', month_key TEXT NOT NULL, '+pos('budget_cents'),'部门科目月度预算','finance','UNIQUE(department_id,account_id,month_key)')
table('positions',key('position')+', position_name TEXT NOT NULL, grade INTEGER NOT NULL','职位与职级','hr')
table('employees',key('employee')+', employee_name TEXT NOT NULL, hire_date TEXT NOT NULL, termination_date TEXT','虚构员工；离职日为不在职首日','hr')
table('employment_history',key('employment')+', '+fk('employee_id','employees')+', '+fk('department_id','departments')+', '+fk('position_id','positions')+', '+fk('store_id','stores',nullable=True)+', start_date TEXT NOT NULL, end_date TEXT','任职历史，左闭右开；可跨部门/门店调动','hr')
table('salary_history',key('salary')+', '+fk('employee_id','employees')+', '+fk('employment_id','employment_history')+', month_key TEXT NOT NULL, '+pos('base_cents')+', '+pos('bonus_cents'),'每员工每月实发基础薪资与奖金；归属当月最后在职岗位','hr','UNIQUE(employee_id,month_key)')
table('attendance',key('attendance')+', '+fk('employee_id','employees')+', attendance_date TEXT NOT NULL, status TEXT NOT NULL, work_minutes INTEGER NOT NULL CHECK(work_minutes>=0), overtime_minutes INTEGER NOT NULL CHECK(overtime_minutes>=0)','工作日考勤；加班分钟独立','hr','UNIQUE(employee_id,attendance_date)')
table('raw_order_import',key('raw')+', source_ref TEXT, raw_date TEXT, raw_customer TEXT, raw_amount TEXT','独立未经清洗导入区；重复、空值、格式错误，不参与正式指标','quality')
for i,d in enumerate(dates):add('calendar_days',d,d[:7],i)
for i in range(1,13):add('stores',i,f'门店{i:02d}', ['古晋','美里','诗巫','吉隆坡'][i%4])
for i in range(1,16):add('categories',i,['护肤','清洁','家居','食品','饮品','文具','服饰','个护','母婴','运动','宠物','厨具','电器','礼品','配件'][i-1])
prices={};costs={}
for i in range(1,501):
 p=r.randrange(600,18001,100);prices[i]=p;add('products',i,(i-1)%15+1,f'商品{i:04d}',p,r.randint(8,40),int(i<=480));
 for y in (2023,2024,2025):
  co=cents(p*((.76 if i%17==0 and i<=100 else .46)+.025*(y-2023)+r.random()*.12));costs[i,y]=co;add('product_costs',len(rows['product_costs'])+1,i,f'{y}-01-01',co)
for i in range(1,5001):add('customers',i,f'客户{i:05d}',r.choice(dates[:300]),r.choice(['砂拉越','西马','沙巴',None]))
for i in range(1,61):add('suppliers',i,f'供应商{i:03d}',r.choice(['本地','区域','海外']))
for i in range(1,4):add('warehouses',i,f'配送仓{i}')
for i,n in enumerate(['零售运营','电商','采购','仓储','财务','人力资源','总部'],1):add('departments',i,n)
for i,m in enumerate(months,1):add('promotions',i,f'月度活动{i}',m+'-15',m+'-22')
for i,n in enumerate(['自然访问','搜索广告','社交媒体','邮件','合作推广'],1):add('traffic_sources',i,n)
for m in months:
 for s in range(1,6):add('marketing_campaigns',len(rows['marketing_campaigns'])+1,s,m,0 if s==1 else r.randint(100000,600000))
# sessions dates and customers aligned with signup
sg={v[0]:v[2] for v in rows['customers']}; sess=[]
for i in range(1,50001):
 u=r.randint(1,5000);d=r.choice(dates[(dt.date.fromisoformat(sg[u])-start).days:]);s=r.choices(range(1,6),[35,25,20,10,10])[0];add('customer_sessions',i,u,s,d);sess.append((i,u,d))
r.shuffle(sess); events=[]; sales_total=collections.defaultdict(int); sale_qty=collections.Counter()
for i in range(1,30001):
 online=i<=12000
 if online:sid,u,d=sess[i-1];st=None
 else:
  sid=None;u=r.choice([None]+list(range(1,5001)));d=r.choice(dates[(dt.date.fromisoformat(sg[u])-start).days:] if u else dates);st=r.choices(range(1,13),[15,14,13,12,11,10,9,8,7,6,5,4])[0];u=None if r.random()<.12 else u
 status=r.choices(['completed','cancelled','pending'],[91,6,3])[0];add('orders',i,st,u,sid,d,'online' if online else 'store',status);amt=0
 for j in range(r.randint(2,4)):
  p=r.choices([r.randint(1,100),r.randint(101,450),r.randint(451,500)],[65,33,2])[0];q=r.randint(1,5);up=prices[p];co=costs[p,int(d[:4])];promo=months.index(d[:7])+1 if 15<=int(d[-2:])<=22 else None;discount=cents(q*up*r.choice([0,.05,.10,.20,.35]) if promo else q*up*r.choice([0,0,0,.05])); iid=len(rows['order_items'])+1;add('order_items',iid,i,p,promo,q,up,discount,co);net=q*up-discount;amt+=net
  if status=='completed':
   w=(st-1)%3+1 if st else 1;events.append((d,w,p,'sale',-q,co,iid,None));sale_qty[w,p]+=q;ret=0
   if r.random()<.075 and date_after(d,3)<=cutoff:
    rd=date_after(d,r.randint(1,min(25,(end-dt.date.fromisoformat(d)).days)));rq=r.randint(1,q);ret=cents(net*rq/q);add('returns',len(rows['returns'])+1,iid,rd,rq,ret);events.append((rd,w,p,'return',rq,co,iid,None))
   if st:sales_total[st,d[:7]]+=net-ret
 if status=='completed':
  a=amt//2 if r.random()<.13 else amt;add('payments',len(rows['payments'])+1,i,d,a,'settled');
  if a<amt:add('payments',len(rows['payments'])+1,i,d,amt-a,'settled')
 elif r.random()<.25:add('payments',len(rows['payments'])+1,i,d,amt,'failed')
for st in range(1,13):
 for m in months:add('sales_targets',len(rows['sales_targets'])+1,st,m,max(0,cents(sales_total[st,m]*r.uniform(.82,1.3))))
# contracts supplier-specific product ranges (all selected pair available), annual history
contracts={}
for p in range(1,501):
 for s in sorted({(p-1)%60+1,(p+12)%60+1}):
  for y in (2023,2024,2025):
   cid=len(rows['supplier_contracts'])+1;cp=cents(costs[p,y]*r.uniform(.9,1.04));add('supplier_contracts',cid,s,p,f'{y}-01-01',f'{y}-12-31',cp);contracts[s,p,y]=(cid,cp)
by_supplier=collections.defaultdict(list)
for s,p,y in contracts:
 if y==2023:by_supplier[s].append(p)
for po in range(1,5001):
 s=r.randint(1,60);w=r.randint(1,3);d=r.choice(dates);prom=date_after(d,r.randint(5,24));status='cancelled' if r.random()<.055 else 'placed';add('purchase_orders',po,s,w,d,prom,status);items=[]
 for n in range(r.randint(1,3)):
  p=r.choice(by_supplier[s]);cid,cp=contracts[s,p,int(d[:4])];q=r.randint(8,100);up=cents(cp*r.choice([.95,1,1,1,1.08,1.18]));pi=len(rows['purchase_order_items'])+1;add('purchase_order_items',pi,po,p,cid,q,up);got=0
  if status!='cancelled':
   first=r.randint(1,q) if r.random()<.25 else q; rd=date_after(prom,r.randint(-4,14));
   for qty,dd in [(first,rd),(q-first,date_after(rd,r.randint(3,15)))]:
    if qty and dd<=cutoff and (qty==first or r.random()<.8):
     rej=min(qty,r.randint(1,3)) if r.random()<.09 else 0;rid=len(rows['goods_receipts'])+1;add('goods_receipts',rid,pi,dd,qty,rej);got+=qty-rej
     if qty>rej:events.append((dd,w,p,'receipt',qty-rej,up,None,rid))
   if got:items.append((pi,got,up))
 if items:
  invdate=min(cutoff,date_after(d,30));iv=len(rows['supplier_invoices'])+1;add('supplier_invoices',iv,po,f'INV-{po:05d}',invdate,date_after(invdate,30));total=0
  for pi,q,up in items:
   bq=q+(2 if r.random()<.05 else 0);bu=up+(100 if r.random()<.04 else 0);add('supplier_invoice_items',len(rows['supplier_invoice_items'])+1,iv,pi,bq,bu);total+=bq*bu
  if r.random()<.8 and date_after(invdate,10)<=cutoff:
   paid=total if r.random()<.78 else total//2;add('supplier_payments',len(rows['supplier_payments'])+1,iv,date_after(invdate,10),paid)
   if paid<total and date_after(invdate,25)<=cutoff and r.random()<.45:add('supplier_payments',len(rows['supplier_payments'])+1,iv,date_after(invdate,25),total-paid)
# inventory opening enough to cover maximal deficit, plus modest reserve, event ledger exact
by=collections.defaultdict(list)
for ev in sorted(events,key=lambda x:(x[0],0 if x[4]>0 else 1)):by[ev[1],ev[2]].append(ev)
for w in range(1,4):
 for p in range(1,501):
  es=by[w,p];bal=minimum=0
  for e in es:bal+=e[4];minimum=min(bal,minimum)
  q=-minimum+r.randint(0,35);add('inventory_opening',len(rows['inventory_opening'])+1,w,p,q);j=0
  for m in monthends:
   while j<len(es) and es[j][0]<=m:
    e=es[j];q+=e[4];add('stock_movements',len(rows['stock_movements'])+1,*[e[k] for k in [1,2,0,3,4,5,6,7]]);j+=1
   assert q>=0;add('inventory_snapshots',len(rows['inventory_snapshots'])+1,w,p,m,q,costs[p,int(m[:4])])
for i,n in enumerate(['租金','水电','营销','行政','运输','银行存款'],1):add('accounts',i,n,'cash' if i==6 else 'expense')
for i in range(1,18001):
 d=r.choice(dates);dep=r.randint(1,7);a=r.randint(1,5);st=r.randint(1,12) if dep==1 else None;amt=r.randint(500,700000);ref=f'J{i:06d}';add('journal_entries',i,d,dep,st,ref);add('journal_lines',2*i-1,i,a,amt,0);add('journal_lines',2*i,i,6,0,amt)
 if r.random()<.93:add('bank_transactions',len(rows['bank_transactions'])+1,d,ref,-amt+(100 if r.random()<.035 else 0))
for i in range(100):add('bank_transactions',len(rows['bank_transactions'])+1,r.choice(dates),None,-r.randint(1000,90000))
for m in months:
 for dep in range(1,8):
  for a in range(1,6):add('budgets',len(rows['budgets'])+1,dep,a,m,r.randint(800000,4000000))
for i,n in enumerate(['助理','专员','高级专员','主管','经理'],1):add('positions',i,n,i)
for e in range(1,251):
 h=r.choice(dates[:850]);term=date_after(h,r.randint(40,600)) if r.random()<.30 else None;term=term if term and term<=cutoff else None;add('employees',e,f'员工{e:03d}',h,term);dep=r.choices(range(1,8),[45,12,10,12,8,5,8])[0];p=r.randint(1,5);st=r.randint(1,12) if dep==1 else None;transfer=date_after(h,200) if r.random()<.25 and date_after(h,200)<(term or cutoff) else None;hist=[]
 for a,b in ([(h,transfer),(transfer,term)] if transfer else [(h,term)]):
  if a==transfer:dep=r.randint(1,7);st=r.randint(1,12) if dep==1 else None;p=min(5,p+1)
  eid=len(rows['employment_history'])+1;add('employment_history',eid,e,dep,p,st,a,b);hist.append((eid,a,b,p))
 for m,me in zip(months,monthends):
  if me<h or (term and m+'-01'>=term):continue
  last=min(me,date_after(term,-1)) if term else me; cur=next(x for x in hist if x[1]<=last and (x[2] is None or last<x[2]));base=150000+cur[3]*65000+r.randint(-20000,25000)+(120000 if e%23==0 else 0);add('salary_history',len(rows['salary_history'])+1,e,cur[0],m,base,r.choice([0,0,10000,25000]))
 for d in dates[(dt.date.fromisoformat(h)-start).days:]:
  if term and d>=term:break
  if dt.date.fromisoformat(d).weekday()>=5:continue
  stat=r.choices(['present','leave','absent'],[94,5,1])[0];add('attendance',len(rows['attendance'])+1,e,d,stat,480 if stat=='present' else 0,r.choice([0,0,0,30,60,90]) if stat=='present' else 0)
for i in range(1,401):add('raw_order_import',i,f'EXT-{i//2 if i%9==0 else i}',r.choice(['2025-12-01','01/12/2025','2025/12/02','bad',None]),r.choice([' 客户001 ','客户001','',None]),r.choice(['1,200.00','1200','RM 55','bad',None]))
for n in schema:ins(n)
# indexes all FK columns, frequent ranges
import re
for n,(f,x) in schema.items():
 for col in re.findall(r'(\w+) INTEGER (?:NOT NULL )?REFERENCES',f):c.execute(f'CREATE INDEX ix_{n}_{col} ON {n}({col})')
 for col in re.findall(r'(\w+(?:date|month|month_key)) TEXT',f):
  if col!='effective_date':c.execute(f'CREATE INDEX ix_{n}_{col} ON {n}({col})')
def view(n,sql,description,group):views[n]=sql;desc[n]=description;groups[n]=group;c.execute(f'CREATE VIEW {n} AS {sql}')
view('v_sales_lines',"SELECT i.order_item_id,o.order_id,o.store_id,o.customer_id,o.session_id,o.channel,o.order_date,i.product_id,i.promotion_id,i.quantity-COALESCE(r.qty,0) AS net_qty,i.quantity*i.unit_price_cents-i.discount_cents-COALESCE(r.refund,0) AS net_cents,(i.quantity-COALESCE(r.qty,0))*i.unit_cost_cents AS cost_cents,i.discount_cents,COALESCE(r.refund,0) AS refund_cents FROM orders o JOIN order_items i ON o.order_id=i.order_id LEFT JOIN (SELECT order_item_id,SUM(quantity) qty,SUM(refund_cents) refund FROM returns WHERE return_date<='2025-12-31' GROUP BY order_item_id) r ON r.order_item_id=i.order_item_id WHERE o.status='completed'",'有效销售行；按订单日期归属退款，截止2025年底','sales')
view('v_order_net',"SELECT order_id,store_id,customer_id,session_id,channel,order_date,SUM(net_cents) net_cents,SUM(cost_cents) cost_cents FROM v_sales_lines GROUP BY order_id,store_id,customer_id,session_id,channel,order_date",'订单粒度净额与成本，防止明细重复','sales')
view('v_po_lines',"SELECT i.po_item_id,i.po_id,p.supplier_id,p.warehouse_id,p.order_date,p.promised_date,i.product_id,i.quantity_ordered,i.unit_price_cents,i.quantity_ordered*i.unit_price_cents AS ordered_cents,COALESCE(r.received,0) received,COALESCE(r.accepted,0) accepted,COALESCE(r.on_time,0) on_time,COALESCE(b.billed,0) billed,COALESCE(b.billed_cents,0) billed_cents FROM purchase_order_items i JOIN purchase_orders p ON p.po_id=i.po_id LEFT JOIN (SELECT g.po_item_id,SUM(g.quantity_received) received,SUM(g.quantity_received-g.quantity_rejected) accepted,SUM(CASE WHEN g.receipt_date<=p.promised_date THEN g.quantity_received-g.quantity_rejected ELSE 0 END) on_time FROM goods_receipts g JOIN purchase_order_items i ON i.po_item_id=g.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id WHERE g.receipt_date<='2025-12-31' GROUP BY g.po_item_id) r ON r.po_item_id=i.po_item_id LEFT JOIN (SELECT po_item_id,SUM(quantity_billed) billed,SUM(quantity_billed*unit_price_cents) billed_cents FROM supplier_invoice_items GROUP BY po_item_id) b ON b.po_item_id=i.po_item_id WHERE p.status<>'cancelled'",'采购行三方预聚合；截止2025年底','procurement')
view('v_invoice_balance',"SELECT i.supplier_invoice_id,i.po_id,p.supplier_id,i.invoice_no,i.invoice_date,i.due_date,b.billed_cents,COALESCE(a.paid_cents,0) paid_cents,b.billed_cents-COALESCE(a.paid_cents,0) balance_cents FROM supplier_invoices i JOIN purchase_orders p ON p.po_id=i.po_id JOIN (SELECT supplier_invoice_id,SUM(quantity_billed*unit_price_cents) billed_cents FROM supplier_invoice_items GROUP BY supplier_invoice_id) b ON b.supplier_invoice_id=i.supplier_invoice_id LEFT JOIN (SELECT supplier_invoice_id,SUM(amount_cents) paid_cents FROM supplier_payments WHERE payment_date<='2025-12-31' GROUP BY supplier_invoice_id) a ON a.supplier_invoice_id=i.supplier_invoice_id WHERE i.invoice_date<='2025-12-31'",'发票粒度余额，截止日固定','finance')
view('v_expenses',"SELECT e.entry_id,e.entry_date,e.department_id,e.store_id,e.reference_no,l.account_id,l.debit_cents-l.credit_cents AS expense_cents FROM journal_entries e JOIN journal_lines l ON l.entry_id=e.entry_id JOIN accounts a ON a.account_id=l.account_id WHERE a.account_type='expense'",'费用分录，不含银行科目','finance')
view('v_staff_month',"SELECT s.employee_id,s.month_key,h.department_id,h.position_id,h.store_id,s.base_cents,s.bonus_cents,COALESCE(a.ot,0) overtime_minutes,COALESCE(a.worked,0) worked_minutes,s.base_cents+s.bonus_cents+ROUND(s.base_cents*1.5*COALESCE(a.ot,0)/10560.0,0) AS labor_cents FROM salary_history s JOIN employment_history h ON h.employment_id=s.employment_id LEFT JOIN (SELECT employee_id,SUBSTR(attendance_date,1,7) m,SUM(overtime_minutes) ot,SUM(work_minutes) worked FROM attendance GROUP BY employee_id,SUBSTR(attendance_date,1,7)) a ON a.employee_id=s.employee_id AND a.m=s.month_key",'员工月成本；示范统一每月176工时、1.5倍加班，非法律工资计算','hr')
c.commit()
print('rows',sum(map(len,rows.values())),len(schema),len(views),flush=True)
