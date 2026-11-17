from pathlib import Path
import json, re, sqlite3, copy, math, datetime, hashlib, random, shutil, tempfile, collections, itertools, time, sys
ROOT=Path(__file__).resolve().parents[1]
UP=Path('/mnt/data/enterprise_sql_upgrade')
COURSE=UP/'CONTENT'/'courses'; BUS=UP/'business_tasks'
D=lambda p:json.loads(Path(p).read_text())
J=lambda p,x: Path(p).write_text(json.dumps(x,ensure_ascii=False,indent=2,allow_nan=False)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def jhash(x):return hashlib.sha256(json.dumps(x,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False).encode()).hexdigest()
def register(c):
 def nullprop(f):return lambda *a: None if any(x is None for x in a) else f(*a)
 c.create_function('CONCAT',-1,lambda *a: None if any(x is None for x in a) else ''.join(str(x) for x in a),deterministic=True)
 c.create_function('CHAR_LENGTH',1,nullprop(lambda a:len(str(a))),deterministic=True)
 c.create_function('OCTET_LENGTH',1,nullprop(lambda a:len(str(a).encode('utf-8'))),deterministic=True)
 c.create_function('YEAR',1,nullprop(lambda a:int(str(a)[:4])),deterministic=True)
 c.create_function('MONTH',1,nullprop(lambda a:int(str(a)[5:7])),deterministic=True)
 c.create_function('DATE_FORMAT',2,nullprop(lambda a,b:datetime.date.fromisoformat(str(a)[:10]).strftime(b)),deterministic=True)
 c.create_function('DATEDIFF',2,nullprop(lambda a,b:(datetime.date.fromisoformat(str(a)[:10])-datetime.date.fromisoformat(str(b)[:10])).days),deterministic=True)
 c.create_function('FLOOR',1,nullprop(math.floor),deterministic=True)
 c.create_function('CEIL',1,nullprop(math.ceil),deterministic=True)
 c.execute('PRAGMA foreign_keys=ON'); c.execute('PRAGMA case_sensitive_like=ON')
 return c
FIX={d['table']:d for p in (UP/'CONTENT'/'fixtures').glob('*.json') for d in [D(p)]}
CH=[D(COURSE/f'ch{i:02}.json') for i in range(1,21)]; KP={k['id']:k for c in CH for k in c['knowledgePoints']}
# 独立小型边界夹具。关联键跨表一致；各题只复制其所需表。未列出表保持单独设计的替代记录或原数据。
H={
'orders':[[11,'护肤品',300,1,'2026-01-31',1],[12,'化妆品',800,2,'2026-02-01',2],[13,'护肤品',700,1,'2026-02-01',1],[14,'日用品',None,3,'2026-03-01',None],[15,'化妆品',400,2,'2026-02-28',2],[16,'护肤品',1000,1,'2026-01-01',4],[17,'日用品',1500,3,'2026-02-20',4],[18,'护肤品',800,1,'2026-02-28',1],[19,'化妆品',800,2,'2026-02-25',3],[20,'日用品',None,3,'2026-02-10',None],[21,'日用品',100,4,'2026-02-01',None],[22,'护肤品',0,2,'2026-01-01',5],[23,'护肤品',100,2,'2026-01-10',5]],
'products':[[11,'S_ale','护肤品',30,50],[12,'Serum2','护肤品',45,150],[13,'Cream2','护肤品',60,180],[14,'Mask50','化妆品',35,200],[15,'Sunscreen2','化妆品',60,80],[16,'Soap_2','日用品',15,0],[17,'A B','日用品',89,49],[18,'古晋皂','日用品',15,10],[19,'S%','护肤品',40,0],[20,'Zero','日用品',0,100],[21,'未知','护肤品',None,None],[22,'Xer','化妆品',65,80],[23,'Soda','化妆品',60,181],[24,'Other','护肤品',15,15],[25,'Test','化妆品',89,1],[26,'Aloe','护肤品',50,100],[27,'Brush','日用品',45,80],[28,'Axy','特殊',60,0],[29,'Solo','特殊',61,1]],
'customers':[[1,'甲'],[2,'乙'],[3,'丙'],[4,'丁'],[5,'戊'],[6,'己'],[7,'庚']],
'stores':[[1,'古晋'],[2,'美里'],[3,'诗巫'],[4,'民都鲁'],[5,'未营业店']],
'employees':[[1,'总经理',None,7000],[2,'主管',1,5000],[3,'专员',1,5000],[4,'顾问',2,8000],[5,'新人',2,3000],[6,'未知薪资',4,None],[7,'另一个根',None,5000]],
'nullable_values':[[11,None,None],[12,0,''],[13,10,'ok'],[14,10,'ok'],[15,-5,' '],[16,42,None],[17,None,'存在']],
'monthly':[['2026-01',1000],['2026-02',0],['2026-03',None],['2026-04',500],['2026-06',1500],['2026-07',1500]],
'raw_data':[[11,'  Mask A  ','15',' SKIN '],[12,'Mask A','15','skin'],[13,'古晋',None,'Skin'],[14,'S%','-5','HOME'],[15,'  ','0','home'],[16,None,'25',None],[17,'A_B','0','makeup'],[18,'张 三','100.25',' OTHER '],[19,'S%','-5','home'],[20,'','20','home']],
'old_customers':[[1],[1],[2],[3],[5],[None]], 'new_customers':[[2],[4],[4],[None]],
'order_items':[[20,11,11,2,100,10],[21,11,12,1,25,5],[22,12,12,3,60,3],[23,13,14,1,60,0],[24,15,15,2,40,0]],
'hr_employees':[[1,1,'甲','2025-01-01',None],[2,1,'乙','2025-02-01','2026-02-01'],[3,2,'丙','2026-02-01','2026-03-01'],[4,2,'丁','2026-02-02',None],[5,1,'戊','2025-11-01',None]],
'salaries':[[21,1,'2026-01-01',4000,10],[22,1,'2026-02-01',4100,20],[23,3,'2026-02-01',5000,0],[24,2,'2026-01-01',3800,0]],
'invoices':[[11,1,'2026-01-01','2026-01-31',150],[12,1,'2026-01-02','2026-02-15',200],[13,3,'2026-02-01','2026-03-01',0],[14,3,'2026-02-07','2026-02-28',80]],
'payments':[[20,11,'2026-01-31',60],[21,11,'2026-02-01',10],[22,12,'2026-02-28',200],[23,12,'2026-03-01',5]],
'purchase_items':[[21,1,11,12,10],[22,1,12,5,20],[23,2,13,0,30],[24,3,15,11,10],[25,3,18,8,5]],
'purchase_orders':[[1,1,'2026-01-01','2026-01-11','approved'],[2,2,'2026-01-02','2026-02-01','approved'],[3,3,'2026-02-01','2026-02-10','approved']],
'receipts':[[11,21,'2026-01-11',4],[12,21,'2026-01-12',5],[13,23,'2026-01-31',0],[14,24,'2026-02-09',6]],
'suppliers':[[1,'甲供','North'],[2,'乙供','South'],[3,'丙供',None]],
'commerce_orders':[[11,1,'2026-01-01','completed'],[12,2,'2026-02-01','cancelled'],[13,5,'2026-03-01','completed'],[14,1,'2026-03-01','completed']],
'budgets':[[11,1,'2026-01-01',300],[12,1,'2026-02-01',600],[13,3,'2026-01-01',50]],
 'transactions':[[11,1,'2026-01-01',100,'expense','x'],[12,1,'2026-02-01',200,'expense','y'],[13,3,'2026-03-01',50,'expense','z'],[14,2,'2026-02-28',1000,'income','w']],
}
# 状态题的两套独立初始快照
STOCK_BASE="CREATE TABLE practice_stock(product_id INT NOT NULL PRIMARY KEY,qty INT NOT NULL CHECK(qty>=0)); INSERT INTO practice_stock VALUES(1,10),(2,20),(3,0); CREATE TABLE guard(id INT PRIMARY KEY); INSERT INTO guard VALUES(99);"
STOCK_TEST="CREATE TABLE practice_stock(product_id INT NOT NULL PRIMARY KEY,qty INT NOT NULL CHECK(qty>=0)); INSERT INTO practice_stock VALUES(1,30),(2,0),(3,10),(4,0); CREATE TABLE guard(id INT PRIMARY KEY); INSERT INTO guard VALUES(98);"
GUARD=["CREATE TABLE guard(id INT PRIMARY KEY); INSERT INTO guard VALUES(99);","CREATE TABLE guard(id INT PRIMARY KEY); INSERT INTO guard VALUES(98);"]
STATE=[]
def st(ch,k,title,req,sql,alt,bad,reason,setup,probes=None,feature=None,mysql=None,mysqlalt=None):
 STATE.append(dict(ch=ch,kp=f'kp{ch:02}-{k:02}',title=title,req=req,sql=sql,alt=alt,bad=bad,reason=reason,setup=setup,probes=probes or [],features=feature or [],mysql=mysql or sql,mysqlalt=mysqlalt or alt,state=True))
st(17,1,'创建受约束的库存表','在已有 guard 表的独立数据库中建立 practice_stock，字段依次为 product_id（INT、非空主键）、qty（INT、非空且>=0），新表为空。保留原表。','CREATE TABLE practice_stock(product_id INT NOT NULL PRIMARY KEY,qty INT NOT NULL CHECK(qty>=0));','CREATE TABLE practice_stock(product_id INT NOT NULL,qty INT NOT NULL CHECK(0<=qty),PRIMARY KEY(product_id));','CREATE TABLE practice_stock(product_id INT NOT NULL PRIMARY KEY,qty INT NOT NULL);','主键、非空和非负约束都需要真实生效，不能只建两个无约束的字段。',GUARD,[{"sql":"INSERT INTO practice_stock VALUES(10,-1)","mustReject":True},{"sql":"INSERT INTO practice_stock VALUES(10,NULL)","mustReject":True},{"sql":"INSERT INTO practice_stock VALUES(NULL,1)","mustReject":True},{"sql":"INSERT INTO practice_stock VALUES(10,0)","mustReject":False},{"sql":"INSERT INTO practice_stock VALUES(10,1); INSERT INTO practice_stock VALUES(10,2)","mustReject":True}])
st(17,2,'新增盘点记录','在 practice_stock 加入唯一新商品编号7、库存25，不影响其他商品、结构或 guard 表。','INSERT INTO practice_stock(product_id,qty) VALUES(7,25);','INSERT INTO practice_stock(qty,product_id) VALUES(25,7);','INSERT INTO practice_stock(product_id,qty) VALUES(7,52);','显式列名保证值对应正确字段，写操作判题比较最终记录。',[STOCK_BASE,STOCK_TEST])
st(17,2,'对指定商品补货','仅将编号2的库存增加5件，不重置原有库存，不修改其他表。','UPDATE practice_stock SET qty=qty+5 WHERE product_id=2;','UPDATE practice_stock SET qty=5+qty WHERE product_id=2;','UPDATE practice_stock SET qty=5 WHERE product_id=2;','增加量与直接覆盖不同；WHERE 防止修改所有商品。',[STOCK_BASE,STOCK_TEST])
st(17,2,'清除零库存演示记录','仅删除 qty=0 的商品，不删除其他商品或数据表。','DELETE FROM practice_stock WHERE qty=0;','DELETE FROM practice_stock WHERE 0=qty;','DELETE FROM practice_stock WHERE qty<=10;','删除前确认边界：零库存才符合，10件仍为有效记录。',[STOCK_BASE,STOCK_TEST])
st(17,3,'增加库存文字标签列','向 practice_stock 末尾添加 label VARCHAR(30) NOT NULL DEFAULT \'未标记\'，原记录和 guard 保留。','ALTER TABLE practice_stock ADD COLUMN label VARCHAR(30) NOT NULL DEFAULT \'未标记\';','ALTER TABLE practice_stock ADD label VARCHAR(30) NOT NULL DEFAULT \'未标记\';','ALTER TABLE practice_stock ADD COLUMN label VARCHAR(30) NOT NULL DEFAULT \'未知\';','新增字段必须同时满足类型、非空、默认值及旧记录填充要求。',[STOCK_BASE,STOCK_TEST],[{"sql":"INSERT INTO practice_stock(product_id,qty) VALUES(8,3)","mustReject":False,"observe":"SELECT label FROM practice_stock WHERE product_id=8"},{"sql":"INSERT INTO practice_stock(product_id,qty,label) VALUES(8,3,NULL)","mustReject":True}])
st(17,3,'删除过期临时数据表','删除 stale_stock 表本身（不是仅清空数据），保留 practice_stock 与 guard。','DROP TABLE stale_stock;','DROP TABLE IF EXISTS stale_stock;','DELETE FROM stale_stock;','DROP删除表定义，DELETE只删除表内记录。',[x+' CREATE TABLE stale_stock(id INT); INSERT INTO stale_stock VALUES(1);' for x in [STOCK_BASE,STOCK_TEST]])
st(17,4,'通过保存点撤销临时补货','在显式事务中给商品2加5件；随后创建保存点，把商品1暂时置0，再回滚到保存点、提交事务。最终仅商品2增加5，其他记录不变。考核保存点操作（原生语法与本地兼容语法不同）。','BEGIN TRANSACTION; UPDATE practice_stock SET qty=qty+5 WHERE product_id=2; SAVEPOINT sp; UPDATE practice_stock SET qty=0 WHERE product_id=1; ROLLBACK TO SAVEPOINT sp; RELEASE SAVEPOINT sp; COMMIT;','BEGIN; UPDATE practice_stock SET qty=5+qty WHERE product_id=2; SAVEPOINT keep_state; UPDATE practice_stock SET qty=0 WHERE product_id=1; ROLLBACK TO keep_state; RELEASE keep_state; COMMIT;','BEGIN; UPDATE practice_stock SET qty=qty+5 WHERE product_id=2; SAVEPOINT sp; UPDATE practice_stock SET qty=0 WHERE product_id=1; RELEASE SAVEPOINT sp; COMMIT;','回滚到保存点仅撤销该点之后的更新；提交前不能遗漏回滚。',[STOCK_BASE,STOCK_TEST],feature=[{'kind':'transaction_savepoint','sequence':['begin','update','savepoint','update','rollback_to','commit'],'status':'需要运行时解析及事务轨迹校验；本次只验证最终状态与事务已结束'}],mysql='START TRANSACTION; UPDATE practice_stock SET qty=qty+5 WHERE product_id=2; SAVEPOINT sp; UPDATE practice_stock SET qty=0 WHERE product_id=1; ROLLBACK TO SAVEPOINT sp; RELEASE SAVEPOINT sp; COMMIT;',mysqlalt='START TRANSACTION; UPDATE practice_stock SET qty=5+qty WHERE product_id=2; SAVEPOINT keep_state; UPDATE practice_stock SET qty=0 WHERE product_id=1; ROLLBACK TO SAVEPOINT keep_state; RELEASE SAVEPOINT keep_state; COMMIT;')
st(17,5,'创建引用库存表的流水表','建立空表 practice_moves，字段 move_id（INT非空主键）、product_id（INT非空，外键引用 practice_stock.product_id）、ref（VARCHAR(30) 非空且唯一），顺序如题；保留现有表。','CREATE TABLE practice_moves(move_id INT NOT NULL PRIMARY KEY,product_id INT NOT NULL,ref VARCHAR(30) NOT NULL UNIQUE,FOREIGN KEY(product_id) REFERENCES practice_stock(product_id));','CREATE TABLE practice_moves(move_id INT NOT NULL,product_id INT NOT NULL,ref VARCHAR(30) NOT NULL,PRIMARY KEY(move_id),UNIQUE(ref),FOREIGN KEY(product_id) REFERENCES practice_stock(product_id));','CREATE TABLE practice_moves(move_id INT NOT NULL PRIMARY KEY,product_id INT NOT NULL,ref VARCHAR(30) NOT NULL UNIQUE);','外键确保流水对应有效商品，唯一约束防止同一参考号重复。',[STOCK_BASE,STOCK_TEST],[{"sql":"INSERT INTO practice_moves VALUES(1,999,'a')","mustReject":True},{"sql":"INSERT INTO practice_moves VALUES(1,1,'a'); INSERT INTO practice_moves VALUES(2,2,'a')","mustReject":True},{"sql":"INSERT INTO practice_moves VALUES(1,1,NULL)","mustReject":True},{"sql":"INSERT INTO practice_moves VALUES(1,1,'a')","mustReject":False}])
st(18,2,'创建指定顺序的复合索引','在已有 orders 表建立名为 idx_store_date 的非唯一索引，键顺序必须为 (store_id,order_date)，不修改记录及其他表。','CREATE INDEX idx_store_date ON orders(store_id,order_date);','CREATE INDEX idx_store_date ON orders (store_id ASC,order_date ASC);','CREATE INDEX idx_store_date ON orders(order_date,store_id);','复合索引的顺序改变可用左前缀；验收检查索引字段和实际数据库结构，不猜测执行计划。', ["CREATE TABLE orders(id INT PRIMARY KEY,store_id INT,order_date DATE); INSERT INTO orders VALUES(1,1,'2026-01-01'); CREATE TABLE guard(i INT);", "CREATE TABLE orders(id INT PRIMARY KEY,store_id INT,order_date DATE); INSERT INTO orders VALUES(2,1,'2026-02-01'),(3,2,'2026-02-01'); CREATE TABLE guard(i INT);"])

def parse_source(file,kind):
 result=[]; ch=0
 for line in file.read_text().splitlines():
  if not line.strip() or line.startswith('#'):continue
  if line.startswith('@'):ch=int(line[1:]);continue
  parts=line.split('|');k=parts[0]; kp=f'kp{ch:02}-{int(k):02}'
  if kind=='query':
   _,title,req,sql,change,reason=parts; old,new=change.split('~',1)
   if old not in sql:raise Exception((ch,title,old,sql))
   result.append(dict(ch=ch,kp=kp,title=title,req=req,sql=sql,bad=sql.replace(old,new,1),old=old,new=new,reason=reason,state=False))
  else:
   _,stem,correct,correctReason,*rest=parts
   result.append(dict(ch=ch,kp=kp,title=stem,req=stem,mc=True,correct=correct,correctReason=correctReason,distractors=[(rest[i],rest[i+1]) for i in range(0,6,2)]))
 return result

def top_order(sql):
 # 仅用于读取已编写的参考SQL排序说明；并非用户SQL判题解析器。
 level=0; q=None; pos=[]; i=0
 while i<len(sql):
  ch=sql[i]
  if q:
   if ch==q:
    if i+1<len(sql) and sql[i+1]==q:i+=2;continue
    q=None
  elif ch in "'\"`":q=ch
  elif ch=='(':level+=1
  elif ch==')':level-=1
  elif level==0 and re.match(r'ORDER\s+BY\b',sql[i:],re.I) and (i==0 or not sql[i-1].isalnum()):pos.append(i)
  i+=1
 if not pos:return ''
 return re.split(r'\bLIMIT\b|\bOFFSET\b|;',sql[pos[-1]:],maxsplit=1,flags=re.I)[0].strip()

def source_tables(sql):return sorted({m for m in re.findall(r'\b(?:FROM|JOIN)\s+([\w]+)',sql,re.I) if m in FIX})
def build_small(tables,variant):
 out=[]
 for t in tables:
  f=FIX[t]
  rows= f['rows'] if variant=='public' else ([] if variant=='empty' else H.get(t,f['rows']))
  out.append({'name':t,'columns':f['columns'],'rows':rows})
 return out

def make_conn(fix):
 c=register(sqlite3.connect(':memory:',isolation_level=None))
 if fix.get('setupSql'):c.executescript(fix['setupSql']);return c
 for t in fix.get('tables',[]):
  c.execute('CREATE TABLE "'+t['name']+'"('+','.join('"'+x['name']+'" '+x['sqliteType'] for x in t['columns'])+')')
  if t['rows']:c.executemany('INSERT INTO "'+t['name']+'" VALUES('+','.join('?' for _ in t['columns'])+')',t['rows'])
 return c

def run(c,sql):
 r=c.execute(sql.strip().rstrip(';'));return {'columns':[d[0] for d in r.description],'rows':[list(x) for x in r.fetchall()]}
def sort_key(r):return json.dumps(r,ensure_ascii=False,separators=(',',':'))
def closeval(a,b):
 if a is None or b is None:return a is b
 if isinstance(a,(int,float)) and isinstance(b,(int,float)):
  if isinstance(a,int) and isinstance(b,int):return a==b
  return abs(a-b)<=1e-6
 return type(a)==type(b) and a==b

def equal(a,b,ordered):
 if a.get('columns')!=b.get('columns') or len(a['rows'])!=len(b['rows']):return False
 if ordered:return all(all(closeval(x,y) for x,y in zip(l,r)) for l,r in zip(a['rows'],b['rows']))
 # canonicalization handles same numeric values of int/float and duplicate rows; exact NULL/text.
 def key(row):return tuple(('n',round(v,6)) if isinstance(v,(int,float)) else ('null',None) if v is None else ('s',v) for v in row)
 return collections.Counter(key(x) for x in a['rows'])==collections.Counter(key(x) for x in b['rows'])

def snap(c,probes):
 data={}
 for (name,) in c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"):
  cols=[list(r[1:]) for r in c.execute(f'PRAGMA table_info("{name}")')]
  # canonical structural types, not raw DDL text. Preserve required column order, keys, constraints.
  indexes=[]
  for rr in c.execute(f'PRAGMA index_list("{name}")'):
   _,ix,unique,origin,partial=rr
   x=[list(r[2:4]) for r in c.execute(f'PRAGMA index_xinfo("{ix}")') if r[5]]
   indexes.append({'name':ix if origin=='c' else None,'unique':unique,'origin':origin,'partial':partial,'keys':x})
  data[name]={'columns':cols,'foreignKeys':sorted([list(r[2:]) for r in c.execute(f'PRAGMA foreign_key_list("{name}")')],key=sort_key),'indexes':sorted(indexes,key=sort_key),'rows':sorted([list(r) for r in c.execute(f'SELECT * FROM "{name}"')],key=sort_key)}
 res=[]
 for x in probes:
  c.execute('SAVEPOINT probe'); failed=False; val=None
  try:
   for stmt in x['sql'].split(';'):
    if stmt.strip():c.execute(stmt)
   if x.get('observe'):val=run(c,x['observe'])
  except sqlite3.IntegrityError:failed=True
  finally:c.execute('ROLLBACK TO probe');c.execute('RELEASE probe')
  res.append({'rejected':failed,'observation':val})
 return {'tables':data,'probes':res,'transactionOpen':c.in_transaction}

def run_small(fix,sql,state=False):
 c=make_conn(fix)
 try:
  if state:c.executescript(sql);return snap(c,fix.get('probes',[]))
  return run(c,sql)
 finally:c.close()

def mysql_for(sql):
 # 原生源码在大部分小型题中与测试注册兼容函数的SQLite源码相同。
 return sql

def variants(sql):
 out=[]
 def add(v,reason,kind='代数或函数等价'):
  if v!=sql and v not in [x[0] for x in out]:out.append((v,reason,kind))
 # 边界条件改写（仅在匹配简单标识符及字面常量时）。
 atom=r"(?:-?\d+(?:\.\d+)?|'(?:[^']|'')*')"; ident=r'\b(?:[A-Za-z_][\w]*\.)?[A-Za-z_][\w]*'
 def bt(m):
  col,n,l,h=m.groups(); return f"({col}<{l} OR {col}>{h})" if n else f"({col}>={l} AND {col}<={h})"
 add(re.sub(rf'({ident})\s+(NOT\s+)?BETWEEN\s+({atom})\s+AND\s+({atom})',bt,sql,flags=re.I),'用闭区间的两个比较（或区间外的 OR）替换 BETWEEN。')
 def in_fn(m):
  col,n,ls=m.groups(); vals=re.findall(atom,ls); op=' AND ' if n else ' OR '; sign='<>' if n else '=';return '('+op.join(f'{col}{sign}{v}' for v in vals)+')'
 add(re.sub(rf'({ident})\s+(NOT\s+)?IN\s*\(\s*({atom}(?:\s*,\s*{atom})+)\s*\)',in_fn,sql,flags=re.I),'用逐值相等或不等谓词表示同一成员集合。')
 add(re.sub(r'COUNT\s*\(\s*\*\s*\)','COUNT(1)',sql,flags=re.I),'常量1对每行非NULL，因此 COUNT(1) 与 COUNT(*) 同计行数。')
 add(re.sub(r'COALESCE\(\s*([\w.]+)\s*,\s*([^(),]+)\)','IFNULL(\1,\2)',sql,flags=re.I),'对两个参数使用 IFNULL 表达同一空值替换。')
 add(re.sub(r'IFNULL\(', 'COALESCE(',sql,flags=re.I),'两个参数的 COALESCE 与 IFNULL 等价。')
 def pred(m):
  col,op,val=m.groups();opp={'>=':'<','<=':'>','<>':'=','!=':'=','>':'<=','<':'>=','=':'<>'}[op];return f'NOT ({col}{opp}{val})'
 add(re.sub(rf'({ident})\s*(>=|<=|<>|!=|=|>|<)\s*({atom})',pred,sql,count=1,flags=re.I),'用反向比较并取 NOT 表达相同三值逻辑。')
 m=re.match(r'(?is)^SELECT DISTINCT\s+([\w., ]+)\s+FROM\s+([\w]+)\s+(ORDER BY\s+.+)$',sql)
 if m:add(f'SELECT {m[1]} FROM {m[2]} GROUP BY {m[1]} {m[3]}','用仅含投影字段的 GROUP BY 对结果去重。')
 add(re.sub(r'\bSUM\(CASE WHEN (.*?) THEN 1 ELSE 0 END\)',r'COUNT(CASE WHEN \1 THEN 1 END)',sql,count=1,flags=re.I),'用只计非NULL的条件计数替代0/1求和（仅在空集和其他边界均相同的本题接受）。')
 # 可验证的等价连接语法及显式ALL为后备；与独立算法区分。
 add(re.sub(r'\bLEFT JOIN\b','LEFT OUTER JOIN',sql,count=1,flags=re.I),'省略或写出 OUTER 不改变左连接。','语法等价')
 add(re.sub(r'\bRIGHT JOIN\b','RIGHT OUTER JOIN',sql,count=1,flags=re.I),'省略或写出 OUTER 不改变右连接。','语法等价')
 add(re.sub(r'\bSELECT\s+(?!ALL\b|DISTINCT\b)', 'SELECT ALL ',sql,count=1,flags=re.I),'普通 SELECT 默认保留重复记录，显式 ALL 不改变结果。','语法等价')
 add(re.sub(r'\bAS\s+([\w]+)',r'AS \1',sql,count=1,flags=re.I),'语法等价','语法等价')
 # 当只有简单 SELECT DISTINCT 时，以 DISTINCT 明示原本语义，用括号包装投影（不改变列名的前提由比对验证）
 add(re.sub(r'^SELECT DISTINCT\s+([\w.]+)',r'SELECT DISTINCT (\1)',sql,count=1,flags=re.I),'在单个投影表达式外添加括号。','语法等价')
 # 仅作为可运行且有列名的语法等价回退。
 add(sql+' /* 等价查询：注释不改变执行 */','SQL 注释不影响查询语义。','注释等价')
 return out

def wrong_candidates(sql):
 # 逐条替换：首选不满足原题业务条件的可执行错误，不使用关键字匹配作为判题。
 xs=[]
 def a(v,r):
  if v!=sql and v not in [x[0] for x in xs]:xs.append((v,r))
 for f,g,why in [('LEFT JOIN','JOIN','误用内连接，可能遗漏没有匹配记录的主体。'),('NOT EXISTS','EXISTS','将反存在性筛选颠倒。'),('COUNT(DISTINCT ','COUNT(','遗漏去重，可能重复计数。'),('UNION ALL','UNION','错误去重，可能丢失实际重复记录。'),('SUM(','AVG(','把累加量错误计算成均值。'),('AVG(','SUM(','把均值错误计算成总量。'),('MAX(','MIN(','最大值与最小值口径颠倒。'),('MIN(','MAX(','最小值与最大值口径颠倒。'),('>=','>','漏掉等于阈值的边界记录。'),('<=','<','漏掉等于阈值的边界记录。'),('>','>=','误包含严格阈值的边界记录。'),('<','<=','误包含严格阈值的边界记录。'),('LAG(','LEAD(','把历史值误作后续值。'),('DESC','ASC','排序方向错误。'),('RANK()','ROW_NUMBER()','没有按并列规则排名。'),('2025-01-01','2025-02-01','错误改变统计期间起点。'),('2025-12-31','2025-11-30','错误缩短固定截止日。'),('2025-01','2025-02','错误改变统计期间起点。'),('2025-12','2025-11','错误缩短统计期间。'),("='store'","='online'",'错误采用线上销售代替门店销售。'),("='online'","='store'",'错误渠道口径。')]:
  if f in sql:a(sql.replace(f,g,1),why)
 # 用本题最后的排序方向作为备选，不用于判定是否包含某个关键词。
 if top_order(sql):
  pos=sql.upper().rfind('ORDER BY');tail=sql[pos:];m=re.match(r'(?i)(ORDER BY\s+[^,\s;]+)(\s+ASC|\s+DESC)?',tail)
  if m:a(sql[:pos]+m[1]+(' ASC' if (m[2] or '').upper().strip()=='DESC' else ' DESC')+tail[m.end():],'输出排序与题设不一致。')
 return xs

def projection_blank(q):
 sql=q['sql']; old=q.get('old','')
 # 排除含长SQL、空片段以及不完整括号的替换。每题以执行结果复判填写完成的SQL。
 if old and len(old)<=36 and old in sql and not q.get('state') and not re.search(r'\bSELECT\b',old,re.I):
  i=sql.index(old);return {'template':sql[:i]+'{{blank1}}'+sql[i+len(old):],'blanks':[{'id':'blank1','referenceText':old,'acceptedForms':[old]}]}
 for token in ['DISTINCT','LEFT JOIN','RIGHT JOIN','UNION ALL','INTERSECT','EXCEPT','NOT EXISTS','HAVING','BETWEEN','COALESCE','NULLIF','ROW_NUMBER','DENSE_RANK','LAG','LEAD','PARTITION BY','GROUP BY','ORDER BY','WHERE','COUNT','SUM','AVG','MAX','MIN','ROUND','TRIM','UPPER','LOWER','CASE','FLOOR']:
  m=re.search(r'\b'+re.escape(token)+r'\b',sql,re.I)
  if m:return {'template':sql[:m.start()]+'{{blank1}}'+sql[m.end():],'blanks':[{'id':'blank1','referenceText':m[0],'acceptedForms':[m[0]]}]}
 return None

def check_attempt(fix,sql,state,ref,ordered):
 try:
  out=run_small(fix,sql,state)
  return {'ran':True,'match':out==ref if state else equal(out,ref,ordered),'resultHash':jhash(out),'rows':None if state else len(out['rows'])}
 except Exception as e:return {'ran':False,'match':False,'error':str(e)}
# 追加并列最高及第二档边界，令排名错误真正产生可判别结果。
H['products'] += [[30,'TieHigh','化妆品',89,11],[31,'TieSecond','化妆品',65,12]]
# 10 道普通业务情景，另50道直接复用企业任务。
SCENARIOS={
 '按数量加权的平均售价':'采购主管需要判断当前商品组合的库存数量加权售价，而不是每个商品等权重的挂牌价平均。',
 '销售明细折后合计':'财务人员核对促销期间销售明细，折扣字段记录每行总折扣，需要核算一致的折后销售金额。',
 '类别库存资金代理指标':'盘点主管用售价估算商品在零售端的潜在销售价值。此指标不是按采购成本计算的库存账面价值。',
 '付款汇总后计算发票余额':'审计人员收到一张发票可能对应多条付款的记录，需逐发票核对尚未结清的金额。',
 '采购订购量与收货量':'采购人员核对分批收货情况，需要按采购明细行显示订购量和实际收货总量，未收货也必须出现。',
 '门店一月二月销售并列展示':'区域经理想在同一行比较门店两个月份的销售额，以便发现业绩变化。',
 '实际收货覆盖比例':'仓库管理员核查每批到货相对于原采购行订购量的覆盖率，零订购量视作无法计算。',
 '对比实际与预算统一汇总':'财务控制部门按会计科目核对费用预算与实际发生额，预算与交易是不同明细粒度。',
 '门店内销售占比最高订单':'销售管理部门希望筛出单笔订单已占本店总销售额至少一半的门店交易，以复核大额订单集中风险。',
 '清洗后计算有效金額':'财务助理在统计导入金额前需要剔除缺名称、缺金额或负数的原始记录，负数在本题定义为待复核。',
}
FILL=[0,2,4,1,2,2,3,4,2,4,2,2,1,1,4,1,0,0]
DEBUG=[0,1,3,1,1,2,2,4,2,3,1,2,1,1,3,1,1,1]
PRED=[1,1,2,1,1,2,2,2,1,2,1,2,1,1,3,1,0,1]
DIFF=[(8,0,0,0),(11,1,0,0),(14,7,1,0),(5,3,0,0),(6,4,0,0),(6,7,2,0),(4,9,5,0),(3,12,10,0),(2,7,3,0),(1,8,10,1),(0,5,5,0),(0,7,7,1),(0,4,4,0),(0,3,7,2),(0,4,14,7),(0,3,5,2),(0,3,6,1),(0,3,5,2)]
TYPES={'选择':30,'代码填空':35,'独立编写':120,'代码纠错':30,'结果预测':25,'业务情景':60}
QUOTAS=[8,12,22,8,10,15,18,25,12,20,10,15,8,12,25,10,10,10,30,20]
DIFF_TOTAL={'入门':60,'简单':90,'中等':90,'困难':45,'挑战':15}
SQL_NAMES={'orders':'订单','products':'商品','stores':'门店','customers':'客户','employees':'员工','nullable_values':'缺失值示例','monthly':'月度销售','raw_data':'原始导入资料','old_customers':'旧客户名单','new_customers':'新客户名单','order_items':'订单明细','invoices':'发票','payments':'付款','purchase_items':'采购明细','purchase_orders':'采购订单','receipts':'收货','suppliers':'供应商','commerce_orders':'电商订单','salaries':'薪资','hr_employees':'人事档案','budgets':'预算','transactions':'交易'}
HINT1={1:'先确认每行代表的业务对象，再区分记录键、属性及约束。',2:'明确结果需要哪些输入字段，以及是否保留原始行的重复。',3:'将题设条件逐项翻译为真假判断，特别注意边界值和括号。',4:'先确定主排序键和并列决胜键，再考虑需要保留的行段。',5:'区分未知、零与空字符串，判断是否需要排除或填补缺失。',6:'先确定统计对象是行、有效观测、不同值还是累计量。',7:'明确每一行结果代表哪一个组；区分分组前后筛选。',8:'先画出两个关系中使用的键与一对多方向，判断哪一侧必须保留。',9:'把业务规则按互斥条件顺序排列，并检查未满足条件的处理。',10:'确定内层查询应计算单一值、键集合还是某行的匹配关系。',11:'明确字符位置、单位、替换方向和NULL处理，避免改写原字段。',12:'确认计算单位；确定日期或数值的边界，对零分母另作判断。',13:'判断需求是保留重复、去重合并、求共同成员还是求差。',14:'将要求拆成可独立核对的中间集合，并为递归确定起点与终点。',15:'决定窗口内的分区、排序与范围，注意是否需要保留全部原行。',16:'先明确原始数据异常规则和规范化标准，再决定保留哪些记录。',17:'明确是数据行还是结构发生变化；保留不应修改的其他数据与约束。',18:'将查询正确性与访问路径分开考量，先确认谓词和索引键是否符合要求。',19:'先明确指标、期间、业务粒度与需要保留的维度，再整合数据。',20:'将各业务模块按共同分析键预先聚合，明确跨模块比较口径。'}
MC_HINTS={ 'kp01-02':'分辨字段类型用于存储何种信息，考虑是否需要精确表示小数。', 'kp03-02':'比较两个逻辑连接词在不加括号时的求值优先级。','kp06-01':'列出表中不同的已知观测值，再考虑聚合函数的计数范围。','kp06-02':'分别考虑无输入行时两个聚合函数的定义。','kp12-05':'注意普通除法与专门的整数商运算不同。','kp15-02':'先写出前两人是否共享名次，再想下一人的编号规则。'}

def make_query_material(q):
 tables=source_tables(q['sql']);state=q['state']; ord=bool(top_order(q['sql'])); fs=[]
 if state:
  for i,setup in enumerate(q['setup']):fs.append({'visibility':'public' if i==0 else 'hidden','variant':'public' if i==0 else 'boundary','kind':'database_state','setupSql':setup,'probes':q['probes']})
 else:
  for v in ['public','boundary','empty'] if tables else ['public']:
   fs.append({'visibility':'public' if v=='public' else 'hidden','variant':v,'kind':'query','tables':build_small(tables,'hidden' if v=='boundary' else v)})
 refs=[run_small(f,q['sql'],state) for f in fs]
 # 独立约束探针的预定答案，不由 referenceSql 决定。
 if state:
  for f,r in zip(fs,refs):
   for spec,got in zip(f.get('probes',[]),r['probes']):assert got['rejected']==spec['mustReject'],q['title']
  alt=[(q['alt'],'不同SQL结构或运算次序实现相同目标。','状态等价')]
 else:
  s=q['sql']; alt=[]
  manual={
   '右连接保留每家门店':'SELECT s.store_id,o.id AS order_id FROM stores s LEFT JOIN orders o ON o.store_id=s.store_id ORDER BY s.store_id,o.id',
   '右连接保留所有发票':'SELECT i.invoice_id,p.payment_id FROM invoices i LEFT JOIN payments p ON p.invoice_id=i.invoice_id ORDER BY i.invoice_id,p.payment_id',
   '没有登记订单的客户':'SELECT c.customer_id FROM customers c LEFT JOIN orders o ON o.customer_id=c.customer_id WHERE o.id IS NULL ORDER BY c.customer_id',
   '没有采购记录的商品':'SELECT p.product_id FROM products p LEFT JOIN purchase_items i ON i.product_id=p.product_id WHERE i.po_item_id IS NULL ORDER BY p.product_id',
   '从未付款的发票':'SELECT i.invoice_id,i.amount FROM invoices i LEFT JOIN payments p ON p.invoice_id=i.invoice_id WHERE p.payment_id IS NULL ORDER BY i.invoice_id',
   '每件商品是否有销售明细':'SELECT p.product_id,CASE WHEN p.product_id IN (SELECT product_id FROM order_items) THEN 1 ELSE 0 END AS has_item FROM products p ORDER BY p.product_id',
   '最高销售额的全部订单':'SELECT id,sales FROM (SELECT id,sales,MAX(sales) OVER() AS m FROM orders) a WHERE sales=m ORDER BY id',
   '按客户逐个计算订单数':'SELECT c.customer_id,COUNT(o.id) AS n FROM customers c LEFT JOIN orders o ON o.customer_id=c.customer_id GROUP BY c.customer_id ORDER BY c.customer_id',
   '两期均存在的客户':'SELECT DISTINCT a.customer_id FROM old_customers a JOIN new_customers b ON a.customer_id=b.customer_id OR (a.customer_id IS NULL AND b.customer_id IS NULL) ORDER BY a.customer_id',
   '老名单中流失的客户':'SELECT DISTINCT a.customer_id FROM old_customers a WHERE NOT EXISTS(SELECT 1 FROM new_customers b WHERE a.customer_id=b.customer_id OR (a.customer_id IS NULL AND b.customer_id IS NULL)) ORDER BY a.customer_id',
   '非缺失数据最大值':'SELECT (SELECT value FROM nullable_values WHERE value IS NOT NULL ORDER BY value DESC LIMIT 1) AS max_value',
   '数据缺失数量':'SELECT COUNT(*) AS missing_count FROM nullable_values WHERE value IS NULL',
   '有客户编号的订单数':'SELECT COUNT(*) AS registered_orders FROM orders WHERE customer_id IS NOT NULL',
   '未知备注默认文字':"SELECT id,CASE WHEN note IS NULL THEN '未备注' ELSE note END AS display_note FROM nullable_values ORDER BY id",
   '零值不是缺失':'SELECT id FROM nullable_values WHERE value IN (0) ORDER BY id',
   '最高三笔订单':'WITH a AS (SELECT id,sales, ROW_NUMBER() OVER(ORDER BY sales DESC,id) AS rn FROM orders) SELECT id,sales FROM a WHERE rn<=3 ORDER BY rn',
   '库存区间之外':'SELECT product_id,stock FROM products WHERE stock<50 OR stock>150 ORDER BY product_id',
   '相邻月份销售绝对变化':'WITH m AS (SELECT month,sales,LAG(sales) OVER(ORDER BY month) AS p FROM monthly) SELECT month,sales,sales-p AS delta FROM m ORDER BY month',
  }
  if q['title'] in manual:alt.append((manual[q['title']],'采用另一种关系运算或查询结构计算相同结果。','独立方法等价'))
  alt+=variants(s)
 # 优先选实际算法/函数改写，如不适用选语法等价，并如实标注。
 selected=None
 for a,why,kind in alt:
  aa=[check_attempt(f,a,state,r,ord) for f,r in zip(fs,refs)]
  if all(x['match'] for x in aa):selected=(a,why,kind,aa);break
 if selected is None:raise Exception(('没有等价解',q['title']))
 w=q['bad'];ww=[check_attempt(f,w,state,r,ord) for f,r in zip(fs,refs)]
 if all(x['match'] for x in ww):raise Exception(('错误解没有被区分',q['title']))
 q.update(fixture_data=fs,expected=refs,alternative=selected,wrong_attempts=ww,ordered=ord,tables=tables)
 q['fill']=projection_blank(q) if not state else None
 return q

def complexity(q):
 if q.get('mc'):return 18+len(q['req'])*0.35
 if q.get('state'):return 55+len(q['sql'])*0.42+len(q.get('features',[]))*80
 s=q['sql'];return len(s)*0.45+15*len(re.findall(r'\bJOIN\b',s,re.I))+20*len(re.findall(r'\bOVER\s*\(',s,re.I))+18*len(re.findall(r'\bWITH\b|\bEXISTS\b',s,re.I))+10*len(re.findall(r'\bCASE\b|\bGROUP BY\b|\bHAVING\b',s,re.I))

def assign_types(qs):
 for q in qs:q['type']='选择' if q.get('mc') else '业务情景' if q['title'] in SCENARIOS else '独立编写'
 # 各章不同题型保留分散覆盖。优先结果较小的预测题、真正可执行错误的纠错题。
 def spread(candidates,n,seed):
  if n==0:return []
  if len(candidates)<n:raise Exception(('配额不够',seed,n,len(candidates)))
  # 在候选知识点间交错选择，避免同一知识点集中为同一形式。
  by=collections.defaultdict(list)
  for x in candidates:by[x['kp']].append(x)
  rng=random.Random(61117+seed)
  for rows in by.values():rng.shuffle(rows)
  keys=sorted(by);rng.shuffle(keys);out=[]
  while len(out)<n:
   for k in keys:
    if by[k]:out.append(by[k].pop());
    if len(out)==n:break
  return out
 for ch in range(1,19):
  l=[q for q in qs if q['ch']==ch and q['type']=='独立编写']
  a=[q for q in l if not q['state'] and len(q['expected'][0]['rows'])<=12 and len(q['expected'][0]['columns'])<=5]
  for q in spread(a,PRED[ch-1],ch*3):q['type']='结果预测'
  l=[q for q in l if q['type']=='独立编写']
  a=[q for q in l if q.get('fill') is not None]
  for q in spread(a,FILL[ch-1],ch*3+1):q['type']='代码填空'
  l=[q for q in l if q['type']=='独立编写']
  # 错误SQL需真正有错误；优先在公开夹具已失败的实际错误。
  a=sorted(l,key=lambda q:(not any(x.get('ran') and not x['match'] for x in q['wrong_attempts']),q['title']))
  for q in spread(a,DEBUG[ch-1],ch*3+2):q['type']='代码纠错'
 return qs

def fixfile(fid):return ROOT/'fixtures'/fid[:5]/(fid+'.json')
def save_fixture(f):
 p=fixfile(f['id']);p.parent.mkdir(parents=True,exist_ok=True);J(p,f);return str(p.relative_to(ROOT))
def fixture_meta(f):return {'id':f['id'],'visibility':f['visibility'],'variant':f['variant'],'path':str(fixfile(f['id']).relative_to(ROOT)),'sha256':sha(fixfile(f['id']))}

def mc_record(q,qid):
 opts=[(q['correct'],q['correctReason'],True)]+[(v,r,False) for v,r in q['distractors']];random.Random(61117+int(qid[1:])).shuffle(opts)
 choices=[{'id':chr(65+i),'text':v,'explanation':why} for i,(v,why,_) in enumerate(opts)];ans=next(chr(65+i) for i,(_,_,ok) in enumerate(opts) if ok)
 fix={'id':qid+'-key','questionId':qid,'visibility':'hidden','variant':'objective_key','kind':'objective_choice','options':choices,'expected':{'correctOptionId':ans},'verification':'按课程知识及MySQL官方资料编写的客观知识答案；非运行引擎结果'}
 save_fixture(fix);correct_id=choices[ord(ans)-65]['id']; alt_wrong=[{'optionId':x['id'],'expectedAccepted':x['id']==ans,'passed':True} for x in choices]
 hints=[MC_HINTS.get(q['kp'],HINT1[q['ch']]),'回想本章《'+KP[q['kp']]['title']+'》中定义的运算对象和限制，再逐个排除不满足题设的选项。','将每个候选与题设的明确条件对照，不能把偶然成立的情况当成通用规则。']
 return {'id':qid,'chapterId':f'ch{q["ch"]:02}','knowledgePointIds':[q['kp']],'primaryKnowledgePointIds':[q['kp']],'difficulty':q['difficulty'],'type':'选择','title':KP[q['kp']]['title']+' · 概念判断','context':'本题考查《'+KP[q['kp']]['title']+'》中实际使用时容易混淆的规则。','requirements':q['req'],'datasetId':None,'schemaVersion':3,'contentVersion':'3.0.0','initialCode':None,'options':choices,'correctOptionId':ans,'acceptedBlanks':[],'expectedOutput':{'kind':'objective_choice','correctOptionId':ans},'referenceSolutions':[{'kind':'choice','optionId':ans,'answer':q['correct'],'explanation':q['correctReason']}],'hints':hints,'explanation':q['correctReason']+'\n'+ '\n'.join(f"{x['id']}. {x['explanation']}" for x in choices),'commonErrors':[{'optionId':x['id'],'reason':x['explanation']} for x in choices if x['id']!=ans],'testFixtureIds':[fix['id']],'testFixtures':[fixture_meta(fix)],'orderPolicy':'not_applicable','columnPolicy':'not_applicable','numericTolerance':None,'requiredAstFeatures':[],'supportStatus':'仅理论','executionMode':'objective_choice','interviewTag':'概念辨析','validationStatus':'客观答案键检查（人工编写事实依据；无原生执行）','mysqlVerification':'不适用：客观概念题','validation':{'kind':'objective_key','options':alt_wrong,'source':source_url(q['ch'])}}

def source_url(ch):
 section={1:'data-types.html',2:'select.html',3:'operator-precedence.html',4:'limit-optimization.html',5:'problems-with-null.html',6:'aggregate-functions.html',7:'group-by-handling.html',8:'join.html',9:'flow-control-functions.html',10:'subquery-restrictions.html',11:'string-functions.html',12:'arithmetic-functions.html',13:'set-operations.html',14:'with.html',15:'window-function-descriptions.html',16:'string-functions.html',17:'create-table-check-constraints.html',18:'multiple-column-indexes.html'}
 return 'https://dev.mysql.com/doc/refman/8.4/en/'+section.get(ch,'select.html')

def partial_hint(q):
 if q.get('fill'):
  return '关注这个局部结构（空缺部分仍需自行完成）：\n'+q['fill']['template'].replace('{{blank1}}','□□□')
 if q['state']:
  return '可以从以下语句骨架开始：\n'+re.sub(r'\b(VALUES\s*\([^)]*\)|SET\s+[^;]+|CHECK\([^)]*\)|ON\s+orders\s*\([^)]*\))','…',q['mysql'],count=1)
 return '以本题表结构为输入，先写出核心查询骨架，再补全输出表达式与条件。'

def normal_record(q,qid):
 state=q['state'];kp=KP[q['kp']];sql=q.get('mysql',mysql_for(q['sql']));alt,alt_reason,alt_kind,alt_attempts=q['alternative'];mysqlalt=q.get('mysqlalt',mysql_for(alt));t=q['type'];fms=[]; fvs=[]; wrong=q['bad']; req=q['req'];
 for i,(f,r) in enumerate(zip(q['fixture_data'],q['expected'])):
  f.update(id=qid+'-'+f['variant'],questionId=qid,datasetId=qid+'-lesson',expected=r,orderPolicy='not_applicable' if state else 'ordered' if q['ordered'] else 'unordered_multiset',columnPolicy='state' if state else 'exact_names_in_order',numericTolerance={'absolute':1e-6,'integerExact':True,'textExact':True,'nullDistinct':True})
  save_fixture(f);fms.append(fixture_meta(f));fvs.append({'fixtureId':f['id'],'reference':{'ran':True,'match':True,'resultHash':jhash(r),'rowCount':None if state else len(r['rows'])},'equivalent':alt_attempts[i],'wrong':q['wrong_attempts'][i]})
 p=q['expected'][0];output={'kind':'database_state','state':p} if state else {'kind':'query','columns':p['columns'],'rows':p['rows'],'rowCount':len(p['rows'])}
 if not state:
  req += '\n输出列名称及顺序：'+', '.join(p['columns'])+'。'+('按题设排序；参考排序键：'+top_order(q['sql']).removeprefix('ORDER BY ').strip()+'。' if q['ordered'] else '未要求额外排序；按含重复行的多重集合比较。')
  req += ' 未指定舍入时保留原计算精度；NULL、零、空字符串不同。'
 ctype='database_state' if state else 'query_result'; initial=None; blanks=[]
 if t=='代码填空':
  initial=q['fill']['template'];blanks=q['fill']['blanks']; req='补全代码中的一个空位，使完整SQL满足以下要求。可输入具有相同语义的片段，提交后按完整代码执行结果核对。\n'+req
 elif t=='代码纠错':initial=wrong;req='下面提供的初始代码存在逻辑、语法或结构错误。请修改整段代码，使其满足以下要求。\n'+req
 elif t=='结果预测':initial=sql;req='不运行下方SQL，手工推算其在公开夹具中的完整返回结果。保留列名和重复行；明确区分NULL、空字符串和数值。\n'+req
 elif t in ['独立编写','业务情景']:initial=''
 if t=='结果预测':ctype='predict_public_result'
 # 对专项语法及优化要求记录执行期结构校验，不能靠关键词匹配。
 ast=q.get('features',[])[:]
 if not state and ('不在 ' in q['req'] or '不在列' in q['req'] or '不要为筛选' in q['req'] or '筛选直接使用' in q['req'] or '直接对 stock' in q['req']):
  ast.append({'kind':'sargable_predicate','status':'需运行时语法树核查比较的列侧表达式；本地内容测试仅比对结果','requirement':q['req']})
 # 要求某一教学语法时以结构校验表达，而不是关键词。
 for marker,name in [('使用 LIKE','like_predicate'),('使用 CONCAT','concat_call'),('使用密集排名','dense_rank_window'),('使用递归','recursive_cte'),('递归生成','recursive_cte'),('递归计算','recursive_cte'),('定义 high_products','cte_high_products'),('定义 jan','cte_jan'),('做 RANGE','range_frame')]:
  if marker in q['req']:ast.append({'kind':name,'status':'需运行时语法树核查；不以关键词匹配判分','requirement':q['req']})
 # 预测题是客观公开结果，隐藏夹具用于验证参考逻辑，不要求预测未知数据。
 hint1=HINT1[q['ch']]
 hint2='结合《'+kp['title']+'》：'+kp['concept']
 hint3=partial_hint(q) if t!='结果预测' else '逐行标记输入是否命中条件，再根据查询的分组、窗口或投影阶段逐一计算；最后按题设顺序整理结果。'
 if t=='代码纠错':hint3='检查初始代码的 '+(q.get('old') or '写操作或结构')+' 对应的业务规则。'+q['reason']
 if t=='代码填空':hint3='回想空位所在子句的功能：'+kp['title']+'；保留周围代码，填入实现题设的表达式。'
 summary='。'.join([x for x in kp.get('executionSteps',[]) if isinstance(x,str)])
 expl=q['reason']+'\n执行思路：'+(summary+'；再按本题指定的条件与输出粒度进行调整。' if summary and not state else '在独立快照中执行目标操作，检查最终记录、结构与约束。')+'\n'+('本题比较操作后的表结构、索引、约束探针、完整记录及事务关闭状态。' if state else f'公开夹具实际返回 {len(p["rows"])} 行、{len(p["columns"])} 列；完整结果见 expectedOutput。')+'\n另一种可用写法：'+alt_reason
 comp_funcs=re.findall(r'\b(?:CONCAT|CHAR_LENGTH|OCTET_LENGTH|YEAR|MONTH|DATE_FORMAT|DATEDIFF|FLOOR|CEIL)\s*\(',q['sql'],re.I)
 status='转换执行' if comp_funcs or (state and sql!=q['sql']) else '直接兼容'
 if q['ch'] in (17,18) and state:compat='仅限题设已执行的DDL/DML/事务及索引子集；原生MySQL和浏览器兼容层尚待验证。'
 elif comp_funcs:compat='SQLite通过本地注册的单独兼容函数真实执行；函数范围限定于本题数据和格式，不代表浏览器兼容层已实现。'
 else:compat='当前SQLite真实执行；MySQL 8.4原生验证未执行，词法、类型、排序及严格模式差异需复验。'
 context=SCENARIOS.get(q['title']) or ('在独立教学数据库上练习安全的结构与记录修改，不会变动内置企业数据库。' if state else '使用课程中小型'+('、'.join(SQL_NAMES.get(x,x) for x in q['tables']) or '计算')+'数据练习'+q['title']+'。教学表中的金额以模拟元为单位，不使用企业库的整数分口径。')
 return {'id':qid,'chapterId':f'ch{q["ch"]:02}','knowledgePointIds':[q['kp']],'primaryKnowledgePointIds':[q['kp']],'difficulty':q['difficulty'],'type':t,'title':q['title'],'context':context,'requirements':req,'datasetId':qid+'-lesson','schemaVersion':3,'contentVersion':'3.0.0','sourceTables':q['tables'] if not state else [],'initialCode':initial,'options':[],'acceptedBlanks':blanks,'expectedOutput':output,'referenceSolutions':[{'kind':'reference','mysqlSql':sql,'sqliteSql':q['sql']},{'kind':alt_kind,'mysqlSql':mysqlalt,'sqliteSql':alt,'explanation':alt_reason}],'hints':[hint1,hint2,hint3],'explanation':expl,'commonErrors':[{'sql':wrong,'reason':q['reason'],'validation':'典型错误已在至少一组夹具被结果/状态比对拒绝' }],'testFixtureIds':[d['id'] for d in fms],'testFixtures':fms,'orderPolicy':'not_applicable' if state else 'ordered' if q['ordered'] else 'unordered_multiset','columnPolicy':'database_state' if state else 'exact_names_in_order','numericTolerance':{'absolute':1e-6,'integerExact':True,'textExact':True,'nullDistinct':True},'requiredAstFeatures':ast,'supportStatus':status,'compatibility':compat,'executionMode':ctype,'runAllowedBeforeSubmit':t!='结果预测','hiddenFixturePolicy':'参考逻辑验证；用户只预测公开结果' if t=='结果预测' else '用户代码独立执行公开与隐藏夹具','interviewTag':'业务分析' if t=='业务情景' else kp['title'],'validationStatus':'SQLite参考、等价、错误解内容测试通过；非原生或手机验收','mysqlVerification':'未运行 MySQL 8.4','validation':{'kind':'state' if state else 'query','equivalentKind':alt_kind,'fixtures':fvs,'source':source_url(q['ch'])}}

OLD={1:['kp19-01'],2:['kp19-08','kp19-20','kp20-02'],3:['kp19-07','kp19-09','kp20-03'],4:['kp19-01'],5:['kp19-09','kp20-04'],6:['kp19-11','kp19-13','kp19-14'],7:['kp19-02','kp19-14'],8:['kp19-12'],9:['kp19-11','kp19-14'],10:['kp19-12','kp19-13'],11:['kp19-03','kp19-15'],12:['kp19-16','kp19-18'],13:['kp19-17','kp19-03'],14:['kp19-15'],15:['kp19-16','kp19-18'],16:['kp19-19','kp19-22'],17:['kp19-04'],18:['kp19-04'],19:['kp19-19','kp20-04'],20:['kp19-21','kp20-04'],21:['kp19-05'],22:['kp19-24'],23:['kp19-23','kp19-25'],24:['kp19-25'],25:['kp19-26'],26:['kp19-27'],27:['kp19-28','kp19-29'],28:['kp19-06','kp19-30'],29:['kp19-27'],30:['kp19-28'],31:['kp19-11','kp19-12','kp20-04'],32:['kp19-07','kp19-10','kp20-01'],33:['kp19-12','kp20-01'],34:['kp19-02','kp19-10','kp20-01'],35:['kp19-29','kp20-04'],36:['kp19-06','kp20-04'],37:['kp19-29','kp20-01'],38:['kp19-01','kp20-01'],39:['kp19-04','kp20-04'],40:['kp19-20','kp20-02'],41:['kp19-04','kp19-27','kp20-01'],42:['kp19-30','kp20-04'],43:['kp19-04','kp19-28','kp20-04'],44:['kp19-15','kp20-01'],45:['kp19-17','kp20-02'],46:['kp19-01','kp20-04'],47:['kp19-05','kp20-01'],48:['kp19-05','kp20-02'],49:['kp19-20','kp20-01','kp20-02'],50:['kp20-01','kp20-04']}

def business_patch(i,d,rows):
 first=dict(zip(d['expectedColumns'],rows[0])) if rows else {}
 def k(n,default=1):return first.get(n,default) or default
 salesstore="SELECT order_item_id FROM order_items i JOIN orders o ON o.order_id=i.order_id WHERE o.status='completed' AND o.channel='store' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31' ORDER BY i.order_item_id LIMIT 1"
 online="SELECT order_item_id FROM order_items i JOIN orders o ON o.order_id=i.order_id WHERE o.status='completed' AND o.channel='online' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31' ORDER BY i.order_item_id LIMIT 1"
 po="SELECT i.po_item_id FROM purchase_order_items i JOIN purchase_orders p ON p.po_id=i.po_id WHERE p.status<>'cancelled' AND p.order_date BETWEEN '2025-01-01' AND '2025-12-31' ORDER BY i.po_item_id LIMIT 1"
 due="SELECT g.receipt_id FROM goods_receipts g JOIN purchase_order_items i ON i.po_item_id=g.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id WHERE p.promised_date BETWEEN '2025-01-01' AND '2025-12-31' AND p.status<>'cancelled' AND g.quantity_received>g.quantity_rejected ORDER BY g.receipt_id LIMIT 1"
 emp_sub="SELECT employee_id FROM employment_history WHERE end_date IS NULL AND start_date<'2025-12-01' ORDER BY employee_id LIMIT 1"
 store_emp="SELECT employee_id FROM employment_history WHERE store_id IS NOT NULL AND end_date IS NULL AND start_date<'2025-12-01' ORDER BY employee_id LIMIT 1"
 salespatch=f"UPDATE order_items SET unit_price_cents=unit_price_cents+12345 WHERE order_item_id IN ({salesstore})"
 onlinepatch=f"UPDATE order_items SET unit_price_cents=unit_price_cents+12345 WHERE order_item_id IN ({online})"
 cancels="UPDATE orders SET status='cancelled' WHERE channel='store' AND store_id=1 AND status='completed' AND order_date BETWEEN '2025-01-01' AND '2025-01-31'"
 snap="UPDATE inventory_snapshots SET quantity=quantity+100 WHERE snapshot_date='2025-12-31' AND warehouse_id=1 AND product_id BETWEEN 1 AND 30"
 pochange=f"UPDATE purchase_order_items SET unit_price_cents=unit_price_cents+137 WHERE po_item_id IN ({po})"
 recv=f"UPDATE goods_receipts SET quantity_rejected=quantity_received WHERE receipt_id IN ({due})"
 salary="UPDATE salary_history SET bonus_cents=bonus_cents+12345 WHERE salary_id=(SELECT MIN(salary_id) FROM salary_history WHERE month_key BETWEEN '2025-01' AND '2025-12')"
 budget="UPDATE budgets SET budget_cents=0 WHERE department_id=1 AND account_id=(SELECT MIN(account_id) FROM budgets WHERE department_id=1) AND month_key BETWEEN '2025-01' AND '2025-12'"
 addpay=f"INSERT INTO supplier_payments(supplier_payment_id,supplier_invoice_id,payment_date,amount_cents) SELECT (SELECT COALESCE(MAX(supplier_payment_id),0)+1 FROM supplier_payments),{k('supplier_invoice_id')},'2025-12-31',12345"
 patch={
 1:(["UPDATE sales_targets SET target_cents=0 WHERE store_id=1 AND month_key BETWEEN '2025-01' AND '2025-12'"],'门店1年度目标全部为0，达成率应为NULL，而非除零或0%。'),
 2:([cancels],'门店1在2025年一月没有有效销售订单，但该月仍须出现在完整月份轴上。'),
 3:([f"UPDATE order_items SET unit_cost_cents=0 WHERE product_id={k('product_id')} AND order_id IN (SELECT order_id FROM orders WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31')"],'将原先候选商品的年内销售成本设为零，检验毛利阈值和排名过滤是否真正生效。'),
 4:(["UPDATE promotions SET start_date='2025-01-01',end_date='2025-12-31' WHERE promotion_id=(SELECT MIN(promotion_id) FROM promotions)"],'增加覆盖全年促销日期的情形，日历仍不应因促销区间重叠而重复计数。'),
 5:(["UPDATE sales_targets SET target_cents=0 WHERE store_id=2 AND month_key BETWEEN '2025-10' AND '2025-12'"],'门店2季度目标为零，验证缺失比率与风险标志口径。'),
 6:([f"UPDATE inventory_snapshots SET quantity=0 WHERE snapshot_date='2025-12-31' AND warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}"],'原积压候选期末库存变为0，必须从金额门槛筛选移除。'),
 7:([f"UPDATE inventory_snapshots SET quantity=0 WHERE warehouse_id={k('warehouse_id')} AND snapshot_date IN ('2024-12-31','2025-12-31') AND product_id IN (SELECT product_id FROM products WHERE category_id={k('category_id')})"],'指定仓库类别期初及期末库存均为0，检验平均库存零分母。'),
 8:([f"UPDATE inventory_snapshots SET quantity=1000000 WHERE snapshot_date='2025-12-31' AND warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}"],'提高原缺货候选的库存量，使其覆盖历史采购交期需求。'),
 9:([f"UPDATE inventory_opening SET quantity=quantity+50 WHERE warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}",f"UPDATE inventory_snapshots SET quantity=quantity+50 WHERE warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}"],'给期初增加旧批次并同步所有库存快照，考查先进先出剩余批次年龄。'),
 10:([f"UPDATE inventory_snapshots SET quantity=1000000 WHERE snapshot_date='2025-12-31' AND warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}"],'给原缺口商品设置充分期末库存，补货优先级应随之变化。'),
 11:(["UPDATE orders SET status='cancelled' WHERE channel='online' AND status='completed' AND order_date BETWEEN '2025-01-01' AND '2025-01-31'"],'一月有效线上订单改成取消，重新计算首次有效购买和年度新老客。'),
 12:([onlinepatch],'改变有效线上订单金额，检验客户金额分层与排序。'),
 13:(["UPDATE orders SET status='cancelled' WHERE channel='online' AND order_date BETWEEN '2025-12-01' AND '2025-12-31' AND status='completed'"],'十二月没有有效线上订单，已观察月份应显示零留存而非丢失月份。'),
 14:(["UPDATE marketing_campaigns SET spend_cents=0 WHERE source_id=1 AND month_key BETWEEN '2025-01' AND '2025-12'"],'渠道1全年广告费用为零；获客成本和广告回报率采用不同分母。'),
 15:([f"UPDATE orders SET order_date='2025-12-31' WHERE order_id=(SELECT MAX(order_id) FROM orders WHERE status='completed' AND channel='online' AND customer_id={k('customer_id')})"],'令原高价值流失客户在年底购买一次，检验最近购买边界。'),
 16:([budget],'将一组年度预算变为0，保留预算行并检测零分母。'),
 17:([addpay],'为原发票增加一笔截至年底的付款，检查一对多付款汇总及余额。'),
 18:(["UPDATE supplier_invoices SET due_date='2025-12-31' WHERE supplier_invoice_id=(SELECT MIN(supplier_invoice_id) FROM v_invoice_balance WHERE balance_cents>0 AND due_date<'2025-12-31')"],'将一笔逾期未付发票的到期日改为截止日当天，必须归入未逾期。'),
 19:(["UPDATE bank_transactions SET reference_no=NULL WHERE bank_id=(SELECT MIN(bank_id) FROM bank_transactions WHERE transaction_date BETWEEN '2025-01-01' AND '2025-12-31' AND reference_no IS NOT NULL AND amount_cents<0)"],'银行支出的参考号缺失，不能与其他NULL参考号错误合并。'),
 20:([f"UPDATE supplier_invoice_items SET unit_price_cents=unit_price_cents+1000 WHERE supplier_invoice_id={k('supplier_invoice_id')}"],'改变发票明细价格，核对异常规则及实际发票粒度。'),
 21:([f"UPDATE employees SET termination_date='2025-12-31' WHERE employee_id=({emp_sub})", "UPDATE employment_history SET end_date='2025-12-31' WHERE end_date IS NULL AND employee_id=(SELECT MIN(employee_id) FROM employees WHERE termination_date='2025-12-31' AND employee_id IN (SELECT employee_id FROM employment_history WHERE end_date IS NULL))"],'新增截止日离职事件，验证左闭右开任职区间与月末规模。'),
 22:([f"UPDATE employees SET termination_date='2025-12-01' WHERE employee_id=({emp_sub})", "UPDATE employment_history SET end_date='2025-12-01' WHERE end_date IS NULL AND employee_id=(SELECT MIN(employee_id) FROM employees WHERE termination_date='2025-12-01' AND employee_id IN (SELECT employee_id FROM employment_history WHERE end_date IS NULL))"],'月初离职事件应计入本月离职，且月末不在职；验证滚动分母。'),
 23:([f"UPDATE salary_history SET base_cents=base_cents+100000 WHERE employee_id={k('employee_id')} AND month_key='2025-12'"],'改变一位员工当月基础薪资，检验部门职位均值、偏离率与排名。'),
 24:([salary.replace("2025-01","2025-10")],'同一员工月奖金增加，部门人工成本应按员工月归属增加。'),
 25:(["UPDATE employees SET termination_date=date(hire_date,'+90 day') WHERE employee_id=(SELECT MIN(e.employee_id) FROM employees e WHERE e.termination_date IS NULL AND e.hire_date<'2025-01-01' AND (SELECT COUNT(*) FROM employment_history h WHERE h.employee_id=e.employee_id)=1)","UPDATE employment_history SET end_date=(SELECT termination_date FROM employees e WHERE e.employee_id=employment_history.employee_id) WHERE end_date IS NULL AND employee_id IN (SELECT employee_id FROM employees WHERE termination_date IS NOT NULL)"],'设定单一任职记录员工在第90日观察日离职，检验留任边界。'),
 26:([pochange],'一条目标期采购行的成交价变化，需更新合同差价及同供应商商品的后续行比较。'),
 27:([recv],'一批原先有合格收货的记录改成全部拒收，验证合格件数、足量率与质量率。'),
 28:([f"UPDATE purchase_order_items SET unit_price_cents=unit_price_cents+333 WHERE po_item_id={k('po_item_id')}"],'改变原不一致采购行单价，核对合格收货计价与金额差。'),
 29:([pochange],'改变一笔实际采购价，考查加权价格及同商品跨供应商最低价。'),
 30:([f"UPDATE supplier_contracts SET unit_price_cents=0 WHERE supplier_id={k('supplier_id')}"],'将原供应商合同价格置零，测试合同价格达标率对评分的影响。'),
 31:([f"UPDATE inventory_snapshots SET quantity=1000000 WHERE snapshot_date='2025-12-31' AND warehouse_id={k('warehouse_id')} AND product_id={k('product_id')}"],'给某仓库商品充分库存，验证畅销缺货与积压规则。'),
 32:(["UPDATE inventory_snapshots SET quantity=0 WHERE snapshot_date='2025-12-31' AND product_id=1"],'商品1库存价值为0，检验商品保留和全局份额分母。'),
 33:([f"UPDATE inventory_snapshots SET quantity=0 WHERE snapshot_date='2025-12-31' AND warehouse_id={k('from_warehouse')} AND product_id={k('product_id')}"],'原调出仓无库存，不能继续作为供给方。'),
 34:([f"UPDATE inventory_snapshots SET unit_cost_cents=0 WHERE snapshot_date IN ('2024-12-31','2025-12-31') AND product_id IN (SELECT product_id FROM products WHERE category_id={k('category_id')})"],'某类别两端标准库存价值为零，回报倍数应为NULL。'),
 35:([f"UPDATE inventory_snapshots SET quantity=1000000 WHERE snapshot_date='2025-12-31' AND product_id={k('product_id')} AND warehouse_id={k('warehouse_id')}"],'逾期未收货仍存在但库存已足够，不应误判叠加缺货。'),
 36:(["DELETE FROM stock_movements WHERE movement_id=(SELECT MIN(movement_id) FROM stock_movements WHERE movement_type='receipt' AND receipt_id IS NOT NULL)"],'故意模拟漏记一笔收货入库流水；原库无差异，测试副本应出现确实漏记的收货编号。'),
 37:([f"UPDATE purchase_order_items SET quantity_ordered=quantity_ordered+30 WHERE po_item_id=(SELECT MIN(po_item_id) FROM v_po_lines WHERE supplier_id={k('supplier_id')} AND promised_date BETWEEN '2025-01-01' AND '2025-12-31' AND quantity_ordered>accepted)"],'增加一个已逾期未足量采购行的订购量，检验采购敞口而非库存金额重复计算。'),
 38:([salespatch],'改变某门店真实销售明细金额，检验销售与费用的独立汇总及贡献利润。'),
 39:(["UPDATE payments SET amount_cents=amount_cents+1234 WHERE payment_id=(SELECT MIN(p.payment_id) FROM payments p JOIN orders o ON o.order_id=p.order_id WHERE p.status='settled' AND p.payment_date<='2025-12-31' AND o.status='completed' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31')"],'故意令一筆销售成功结算多1234分，原库0差异，测试应返回真实差额。'),
 40:([cancels],'某门店某月无有效销售，月轴仍完整，费用率需处理零分母。'),
 41:(["INSERT INTO suppliers(supplier_id,supplier_name,region) SELECT MAX(supplier_id)+1,'测试无采购供应商','North' FROM suppliers"],'新增没有任何采购或发票的新供应商，五个金额指标均应为0。'),
 42:([addpay],'给原三方不一致发票再增加一笔付款，检验付款与合格收货价值的差值。'),
 43:(["UPDATE supplier_invoices SET due_date='2024-01-01' WHERE supplier_invoice_id=(SELECT MIN(supplier_invoice_id) FROM v_invoice_balance WHERE balance_cents>0 AND due_date>='2025-12-31')"],'将原未逾期未付发票变为逾期，检验逾期占未付比例。'),
 44:(["UPDATE marketing_campaigns SET spend_cents=0 WHERE source_id=1 AND month_key BETWEEN '2025-01' AND '2025-12'"],'渠道1无广告支出，检验贡献利润而非遗漏该渠道。'),
 45:([f"UPDATE marketing_campaigns SET spend_cents=spend_cents+12345 WHERE source_id={k('source_id')} AND month_key='{k('cohort','2025-01')}'"],'增加首购同期群对应渠道月份广告费，检验获客成本分母与90天观察期的区别。'),
 46:(["UPDATE returns SET refund_cents=refund_cents+1 WHERE return_id=(SELECT MIN(r.return_id) FROM returns r JOIN order_items i ON i.order_item_id=r.order_item_id JOIN orders o ON o.order_id=i.order_id WHERE o.status='completed' AND o.channel='online' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31')"],'一条线上有效订单退款增加1分，检验退款率及退款前后利润。'),
 47:(["UPDATE salary_history SET bonus_cents=bonus_cents+12345 WHERE salary_id=(SELECT MIN(s.salary_id) FROM salary_history s JOIN employment_history h ON h.employment_id=s.employment_id WHERE h.store_id IS NOT NULL AND s.month_key BETWEEN '2025-01' AND '2025-12')"],'某位门店员工的月奖金提高，销售保持不变，验证人工占比和人效。'),
 48:([f"UPDATE employees SET termination_date='2025-12-31' WHERE employee_id=({store_emp})","UPDATE employment_history SET end_date='2025-12-31' WHERE end_date IS NULL AND store_id IS NOT NULL AND employee_id=(SELECT MIN(employee_id) FROM employees WHERE termination_date='2025-12-31' AND employee_id IN (SELECT employee_id FROM employment_history WHERE end_date IS NULL AND store_id IS NOT NULL))"],'新增某门店员工在截止日离职，检验完整门店月份轴与人数变化。'),
 49:(["UPDATE marketing_campaigns SET spend_cents=spend_cents+12345 WHERE campaign_id=(SELECT MIN(campaign_id) FROM marketing_campaigns WHERE month_key BETWEEN '2025-01' AND '2025-12')"],'广告支出单独变化，经营贡献不能重复扣除广告金额。'),
 50:(["UPDATE inventory_snapshots SET quantity=0 WHERE snapshot_date='2025-12-31'"],'第四季度期末库存金额归零，季度轴、风险指标定义仍须保留。'),
 }
 candidates=[patch[i]]
 for sql,why in [(salespatch,'改变目标期销售金额'),(onlinepatch,'改变目标期线上销售金额'),(snap,'改变期末仓库商品数量'),(pochange,'改变目标期采购价格'),(recv,'改变目标期合格收货量'),(salary,'改变目标期员工月奖金'),(budget,'改变目标期预算'),(addpay,'增加截止日前付款')]:
  if [sql]!=patch[i][0]:candidates.append(([sql],why+'，以独立数据变体核对查询是否适应底层记录变化。'))
 return candidates

def apply_patch(c,stmts):
 c.execute('SAVEPOINT fixture')
 try:
  change=0
  for s in stmts:
   r=c.execute(s);change+=max(0,r.rowcount)
  return change
 except Exception:
  c.execute('ROLLBACK TO fixture');c.execute('RELEASE fixture');raise

def revert_patch(c):c.execute('ROLLBACK TO fixture');c.execute('RELEASE fixture')

def biz_independent(i,c,expected):
 # 对若干非常明确的边界条件添加不依赖参考SQL的额外断言。
 rows=[dict(zip(expected['columns'],r)) for r in expected['rows']]
 if i==1:
  r=next(x for x in rows if x['store_id']==1);return {'description':'门店1目标0且达成率NULL','passed':r['target_cents']==0 and r['achievement_pct'] is None}
 if i==14:
  r=next(x for x in rows if x['source_id']==1);return {'description':'广告支出为零时回报率NULL','passed':r['spend_cents']==0 and r['roas'] is None}
 if i==36:return {'description':'原库无差异、删除一条收货入库流水后产生唯一负差异','passed':len(rows)==1 and rows[0]['difference']<0 and rows[0]['difference']==rows[0]['posted_units']-rows[0]['expected_units']}
 if i==39:return {'description':'原库无差异、增加成功支付1234分后产生唯一差异','passed':len(rows)==1 and rows[0]['difference_cents']==1234}
 if i==41:
  sid=c.execute('SELECT MAX(supplier_id) FROM suppliers').fetchone()[0];r=next(x for x in rows if x['supplier_id']==sid);return {'description':'无采购新供应商金额列全部为0','passed':all(v==0 for k,v in r.items() if k!='supplier_id')}
 if i==50:
  r=next(x for x in rows if 'Q4' in x['q']);return {'description':'第四季度库存金额为0且四季度仍存在','passed':r['stock_cents']==0 and len(rows)==4}
 return None

def business_record(i,d,db):
 qid=f'q{250+i:04}';sql=d['sqliteSql'];mysql=d['mysqlSql'];ordered=d['orderPolicy']=='ordered';orig=D(UP/d['expectedOutputPath']);start=time.monotonic();p=run(db,sql)
 assert equal(p,orig,ordered),(d['id'],'公开基准与上一阶段不同')
 patches=business_patch(i,d,p['rows']);chosen=None
 for stmts,reason in patches:
  try:
   change=apply_patch(db,stmts)
  except sqlite3.Error:continue
  try:
   r=run(db,sql)
   if change>0 and not equal(p,r,ordered):
    # 选定隐藏数据具有变化的正确结果；保持外键无违规。业务盲点用单独测试副本明确说明。
    fk=db.execute('PRAGMA foreign_key_check').fetchone();
    if fk is None:
     independent=biz_independent(i,db,r) if stmts==patches[0][0] else None
     if independent is not None and not independent['passed']:raise Exception((qid,'独立断言失败',independent))
     chosen=(stmts,reason,r,change,independent);break
  finally:revert_patch(db)
 if chosen is None:raise Exception((qid,'没有可生效隐藏数据变体'))
 sts,reason,hid,nchanged,independent=chosen
 # 尝试另一种业务等价实现；如果只能找到语法等价，在记录中保留真实级别。
 def exec_b(s,ref):
  try:
   x=run(db,s);return {'ran':True,'match':equal(x,ref,ordered),'rows':len(x['rows']),'resultHash':jhash(x)}
  except sqlite3.Error as e:return {'ran':False,'match':False,'error':str(e)}
 aselected=None
 for variant,why,kind in variants(sql):
  pub=exec_b(variant,p)
  if not pub['match']:continue
  apply_patch(db,sts)
  try:h=exec_b(variant,hid)
  finally:revert_patch(db)
  if h['match']:
   aselected=(variant,why,kind,pub,h);break
 if aselected is None:raise Exception((qid,'missing eq'))
 alt,altwhy,altkind,ap,ah=aselected
 # 将已通过SQLite验证的等价变换应用于原生版本；若方言不同且无法匹配，提供单独明确的原生等价源代码。
 malt=next((v for v,w,k in variants(mysql) if w==altwhy and k==altkind),None)
 if malt is None:malt=mysql+' /* 同一参考语义，MySQL 独立等价复验待完成 */'
 wrongselect=None
 for w,why in wrong_candidates(sql):
  wp=exec_b(w,p)
  # 优先可运行的逻辑错误，且至少一份测试数据能拒绝。
  if not wp['ran']:continue
  apply_patch(db,sts)
  try:wh=exec_b(w,hid)
  finally:revert_patch(db)
  if wh['ran'] and (not wp['match'] or not wh['match']):wrongselect=w,why,wp,wh;break
 if wrongselect is None:
  # 在空结果任务上必须使用有数据的隐藏副本；以不符合需求的额外筛选测试。
  for w,why in wrong_candidates(sql):
   wp=exec_b(w,p);apply_patch(db,sts)
   try:wh=exec_b(w,hid)
   finally:revert_patch(db)
   if not wp['match'] or not wh['match']:wrongselect=w,why,wp,wh;break
 if wrongselect is None:raise Exception((qid,'没有可判别错误解'))
 w,wwhy,wp,wh=wrongselect
 # 对 BP10 口径歧义明确为承诺日严格晚于截止日。
 req=d['requirements'].replace('承诺日未到年底的未完成采购数量','承诺日严格晚于2025-12-31、且未完成采购的剩余数量') if i==10 else d['requirements']
 req+='\n输出字段名称及顺序：'+', '.join(p['columns'])+'。'+('输出顺序（含决胜键）：'+top_order(sql).removeprefix('ORDER BY ').strip()+'。' if ordered and top_order(sql) else '未规定排序时按含重复行的多重集合比较。')+' 除明确为件、日、小时、计数、比例或百分比字段外，金额字段统一以整数马币分计；按题设保留小数位，零分母或未知比率按定义返回NULL。'
 comp='已按上一阶段原生MySQL源代码及SQLite查询分别保留；原生MySQL和浏览器WASM仍未运行。'
 fms=[]; fvs=[];ds=d['datasetId'];idname=ds.removesuffix('-v2')
 for v,ref,eq,err in [('public',p,ap,wp),('boundary',hid,ah,wh)]:
  f={'id':qid+'-'+v,'questionId':qid,'visibility':'public' if v=='public' else 'hidden','variant':v,'kind':'database_delta','datasetId':ds,'baseDatabase':f'databases/{idname}/{idname}.sqlite','baseSha256':DB_HASH[idname],'patchSql':[] if v=='public' else sts,'patchDescription':'不更改上一阶段的正式模拟企业库' if v=='public' else reason,'patchRowChanges':0 if v=='public' else nchanged,'testIsolation':'每次使用全新数据库副本；夹具可故意引入对账差异或改变业务记录，仅用于判题，绝不可写入基准库','expected':ref,'orderPolicy':'ordered' if ordered else 'unordered_multiset','columnPolicy':'exact_names_in_order','numericTolerance':{'absolute':1e-6,'integerExact':True,'textExact':True,'nullDistinct':True}}
  if v=='boundary' and independent:f['independentAssertion']=independent
  save_fixture(f);fms.append(fixture_meta(f));fvs.append({'fixtureId':f['id'],'reference':{'ran':True,'match':True,'rowCount':len(ref['rows']),'resultHash':jhash(ref)},'equivalent':eq,'wrong':err,'independentAssertion':independent if v=='boundary' else None})
 primary=d['knowledgePointIds'];related=OLD.get(i,[])
 h=d['hints'][:];steps=d['executionSteps']
 if len(h)<3:h += [HINT1[int(d['chapterId'][2:])]]*(3-len(h))
 # 原任务第三级为粒度提示时补充明确的局部结构但不提供完整查询。
 h[2]=h[2]+'；可先将各来源数据按共同业务键汇总为独立中间结果，再完成最终条件及排序。' if 'WITH ' in sql.upper() else h[2]+'；检查 NULL、期间和返回列是否均符合题设。'
 record=copy.deepcopy(d);record.update(id=qid,taskId=d['id'],schemaVersion=3,contentVersion='3.0.0',type='业务情景',knowledgePointIds=list(dict.fromkeys(primary+related)),primaryKnowledgePointIds=primary,prerequisiteKnowledgePointIds=related,requirements=req,expectedOutput={'kind':'query','columns':p['columns'],'rows':p['rows'],'rowCount':len(p['rows'])},expectedOutputPath=fms[0]['path']+'#expected',referenceSolutions=[{'kind':'reference','mysqlSql':mysql,'sqliteSql':sql},{'kind':altkind,'mysqlSql':malt,'sqliteSql':alt,'explanation':altwhy}],hints=h,explanation=d['explanation']+'\n原任务执行步骤：'+' → '.join(steps)+'。\n本次已复核公开完整结果与原业务任务一致，并增加独立数据变体：'+reason+'\n另一种可通过的写法：'+altwhy,commonErrors=[{'reason':d['commonError']},{'sql':w,'reason':wwhy,'validation':'已经过公开与隐藏数据对照'}],testFixtureIds=[f['id'] for f in fms],testFixtures=fms,initialCode='',options=[],acceptedBlanks=[],columnPolicy='exact_names_in_order',numericTolerance={'absolute':1e-6,'integerExact':True,'textExact':True,'nullDistinct':True},requiredAstFeatures=[],supportStatus='转换执行' if mysql!=sql or 'CONCAT(' in sql else '直接兼容',compatibility=comp,executionMode='query_result',runAllowedBeforeSubmit=True,hiddenFixturePolicy='真实运行独立企业库副本及增量变体',interviewTag='企业业务实战',validationStatus='SQLite公开、隐藏、等价及典型错误解验证通过；未完成原生MySQL与手机验收',mysqlVerification='未运行 MySQL 8.4',validation={'kind':'business','equivalentKind':altkind,'baselineMatchesPrevious':True,'fixtures':fvs,'elapsedMs':round((time.monotonic()-start)*1000,1)})
 return record
_old_variants=variants
def variants(sql):
 original=_old_variants(sql);more=[]
 def add(v,why):
  if v!=sql:more.append((v,why,'关系结构等价'))
 m=re.match(r'(?is)^SELECT\s+(.+?)\s+FROM\s+([A-Za-z_][\w]*)(?:\s+(?!WHERE\b|ORDER\b|GROUP\b|HAVING\b|LIMIT\b|JOIN\b|LEFT\b|RIGHT\b|CROSS\b|UNION\b|EXCEPT\b|INTERSECT\b|INNER\b)([A-Za-z_]\w*))?(?=\s|$)(.*)$',sql)
 if m and m[2] in FIX:
  sel,t,a,tail=m.groups();a=a or ''
  if not re.match(r'(?is)^\s*(JOIN|LEFT JOIN|RIGHT JOIN|CROSS JOIN|INNER JOIN|UNION|EXCEPT|INTERSECT)\b',tail) and not re.search(r'\b(?:UNION|INTERSECT|EXCEPT)\b',tail,re.I):
   g=re.match(r'(?is)^\s*GROUP BY\s+([\w., ]+?)(\s+ORDER BY\s+.*|$)',tail)
   if g and not re.search(r'\bDISTINCT\b',sel,re.I) and re.search(r'\b(?:SUM|AVG|COUNT|MIN|MAX)\(',sel,re.I):
    keys=g[1].strip();select=re.sub(r'\b(SUM|AVG|COUNT|MIN|MAX)\(([^()]*)\)',lambda z:z[0]+' OVER(PARTITION BY '+keys+')',sel,flags=re.I)
    add('SELECT DISTINCT '+select+' FROM '+t+(' '+a if a else '')+g[2],'用相同分区键的聚合窗口计算结果，再去重恢复分组粒度。')
   lm=re.search(r'(?is)\s+LIMIT\s+(\d+)(?:\s+OFFSET\s+(\d+))?\s*$',tail);order=top_order(sql)
   if lm and order and not a and not re.search(r'GROUP BY|HAVING|DISTINCT|\bOVER\b',sql,re.I):
    count=int(lm[1]);offset=int(lm[2] or 0);pos=tail.upper().rfind('ORDER BY');before=tail[:pos]
    add(f'WITH ranked_input AS (SELECT *,ROW_NUMBER() OVER({order}) AS row_pos FROM {t}{before}) SELECT {sel} FROM ranked_input WHERE row_pos>{offset} AND row_pos<={offset+count} ORDER BY row_pos','使用相同排序生成行号，再映射分页起止行。')
   add(f'SELECT {sel} FROM (SELECT * FROM {t}) AS {a or t}{tail}','先构造派生输入关系，再执行相同投影、聚合和筛选；保持结果语义。')
 return [x for x in original if x[2] not in ['语法等价','注释等价']]+more+[x for x in original if x[2] in ['语法等价','注释等价']]
DB_HASH={}
def sql_literal(v):
 if v is None:return 'NULL'
 if isinstance(v,(int,float)):return str(v)
 return "'"+str(v).replace('\\','\\\\').replace("'","''")+"'"
def write_mysql_small(f,record,folder):
 s=['-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。','SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;',"SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';"]
 if f.get('setupSql'):s.append(f['setupSql'])
 else:
  for t in f['tables']:
   col=', '.join('`'+x['name']+'` '+x['mysqlType'] for x in t['columns']);s.append(f'CREATE TABLE `{t["name"]}` ({col});')
   if t['rows']:s.append('INSERT INTO `'+t['name']+'` VALUES\n'+',\n'.join('('+','.join(sql_literal(v) for v in row)+')' for row in t['rows'])+';')
 s+=['-- 参考解；等价解须从同一初始快照另行运行。',record['referenceSolutions'][0]['mysqlSql'].rstrip(';')+';']
 if f['kind']=='database_state':
  for t in f['expected']['tables']:s+=['SHOW CREATE TABLE `'+t+'`;','SELECT * FROM `'+t+'`;']
  s.append('-- 约束探针应在独立测试快照逐项运行。')
 (folder/(f['id']+'.sql')).write_text('\n'.join(s)+'\n',encoding='utf-8')
def independent_small(q,records):
 title=q['title'];fs=q['fixture_data'];out=[]
 funcs={
 '订单记录数':lambda t:[[len(t['orders'])]],
 '有客户编号的订单数':lambda t:[[sum(1 for r in t['orders'] if r[5] is not None)]],
 '购买客户去重计数':lambda t:[[len({r[5] for r in t['orders'] if r[5] is not None})]],
 '数据缺失数量':lambda t:[[sum(1 for r in t['nullable_values'] if r[1] is None)]],
 '销售额总计':lambda t:[[sum(x) if (x:=[r[2] for r in t['orders'] if r[2] is not None]) else None]],
 '库存合计可用数量':lambda t:[[sum(r[4] for r in t['products'] if r[4] is not None)]],
 '最高与最低售价差额':lambda t:[[max(x)-min(x) if (x:=[r[3] for r in t['products'] if r[3] is not None]) else None]],
 '严格高于门槛的商品':lambda t:[[r[0],r[3]] for r in sorted(t['products']) if r[3] is not None and r[3]>45],
 '不超过库存警戒数量':lambda t:[[r[0],r[4]] for r in sorted(t['products']) if r[4] is not None and r[4]<=80],
 '找出缺货商品':lambda t:[[r[0],r[1]] for r in sorted(t['products']) if r[4]==0],
 '闭区间销售金额':lambda t:[[r[0],r[2]] for r in sorted(t['orders']) if r[2] is not None and 300<=r[2]<=800],
 '缺少客户键的订单':lambda t:[[r[0]] for r in sorted(t['orders']) if r[5] is None],
 '零值不是缺失':lambda t:[[r[0]] for r in sorted(t['nullable_values']) if r[1]==0],
 '空文本与未知文本':lambda t:[[r[0],r[2]] for r in sorted(t['nullable_values']) if r[2] in ('',None)],
 '分页第二页':lambda t:[[r[0],r[2]] for r in sorted(t['orders'])[2:4]],
 '员工薪资有跳号排名':lambda t:[[r[0],r[3],1+sum(1 for x in t['employees'] if x[3] is not None and x[3]>r[3])] for r in sorted([r for r in t['employees'] if r[3] is not None],key=lambda r:(-r[3],r[0]))],
 '不同薪资档的连续名次':lambda t:[[r[0],r[3],1+len({x[3] for x in t['employees'] if x[3] is not None and x[3]>r[3]})] for r in sorted([r for r in t['employees'] if r[3] is not None],key=lambda r:(-r[3],r[0]))],
 '没有登记订单的客户':lambda t:[[r[0]] for r in sorted(t['customers']) if r[0] not in {x[5] for x in t['orders'] if x[5] is not None}],
 '每门店最高单笔订单编号':lambda t:[[store,r[0],r[2]] for store in sorted({x[3] for x in t['orders'] if x[2] is not None}) for r in [sorted([x for x in t['orders'] if x[3]==store and x[2] is not None],key=lambda r:(-r[2],r[0]))[0]]],
 '两期均存在的客户':lambda t:[[v] for v in sorted({r[0] for r in t['old_customers']}&{r[0] for r in t['new_customers']},key=lambda x:(x is not None,x if x is not None else 0))],
 '老名单中流失的客户':lambda t:[[v] for v in sorted({r[0] for r in t['old_customers']}-{r[0] for r in t['new_customers']},key=lambda x:(x is not None,x if x is not None else 0))],
 '企业两期客户覆盖规模':lambda t:[[len({r[0] for name in ['old_customers','new_customers'] for r in t[name] if r[0] is not None})]],
 '生成1至6整数序列':lambda t:[[x] for x in range(1,7)],
 '生成每隔三天的编号':lambda t:[[x] for x in range(0,13,3)],
 '计算递归阶乘':lambda t:[[x,math.factorial(x)] for x in range(1,6)],
 '相邻月份销售绝对变化':lambda t:[[m,v,None if i==0 or v is None or z[i-1][1] is None else v-z[i-1][1]] for z in [sorted(t['monthly'])] for i,(m,v) in enumerate(z)],
 '从未付款的发票':lambda t:[[r[0],r[4]] for r in sorted(t['invoices']) if r[0] not in {x[1] for x in t['payments']}],
 '低于零的金额待核':lambda t:[[r[0],float(r[2])] for r in sorted(t['raw_data']) if r[2] is not None and float(r[2])<0],
 '识别重复原始名称':lambda t:[[k,n] for k,n in sorted(collections.Counter(r[1].strip() for r in t['raw_data'] if r[1] is not None and r[1].strip()).items()) if n>1],
 '缺失名称或金额的筛查':lambda t:[[r[0]] for r in sorted(t['raw_data']) if r[1] is None or r[1].strip()=='' or r[2] is None],
 '清洗后计算有效金額':lambda t:[[sum(a) if (a:=[float(r[2]) for r in t['raw_data'] if r[1] is not None and r[1].strip() and r[2] is not None and float(r[2])>=0]) else None]],
 '高价商品与低价商品计数':lambda t:[[sum(1 for r in t['products'] if r[3] is not None and r[3]>=50),sum(1 for r in t['products'] if r[3] is not None and r[3]<50)]],
 '有登记客户的订单':lambda t:[[r[0],r[5]] for r in sorted(t['orders']) if r[5] is not None],
 }
 if title not in funcs:return out
 for f,r,log in zip(fs,q['expected'],records):
  vals=funcs[title]({t['name']:t['rows'] for t in f.get('tables',[])})
  ok=equal({'columns':r['columns'],'rows':vals},r,q['ordered']);assert ok,(title,vals,r)
  out.append({'fixtureId':log['fixtureId'],'description':'使用独立Python迭代、集合或算术规则计算期望结果','passed':ok})
 return out

def md_write(ch,records):
 p=ROOT/'reading'/f'ch{ch:02}.md';l=[f'# 第{ch:02}章 · {CH[ch-1]["title"]}（{len(records)}题）','','答案与提示用于开发审阅；正式答题界面不得提前展示答案和隐藏夹具。']
 def table(cols,rows):return ['| '+' | '.join(cols)+' |','| '+' | '.join(['---']*len(cols))+' |']+['| '+' | '.join(('NULL' if v is None else '空字符串' if v=='' else str(v).replace('|','\\|')) for v in row)+' |' for row in rows]
 for q in records:
  l+=['','---','',f'## {q["id"]} · {q["title"]}',f'题型：{q["type"]}　难度：{q["difficulty"]}　知识点：'+', '.join(q['knowledgePointIds']),'',q['context'],'',q['requirements'],'']
  if q.get('taskId'):l+=['任务：'+q['taskId']+'；数据库：'+q['datasetId'],'']
  if q.get('initialCode'):l+=['初始代码：','```sql',q['initialCode'].replace('{{blank1}}','□□□'),'```','']
  if q['type']=='选择':l+=['选项：']+[f'- {o["id"]}. {o["text"]}' for o in q['options']]
  else:
   f=D(ROOT/q['testFixtures'][0]['path']);l+=['公开数据：']
   for t in f.get('tables',[]):l+=['',f'**{t["name"]}**']+table([x['name'] for x in t['columns']],t['rows'])
   if f.get('setupSql'):l+=['```sql',f['setupSql'],'```']
   if f.get('baseDatabase'):l+=['数据库文件：`'+f['baseDatabase']+'`；完整记录通过数据表读取。']
  l+=['','提示：']+[f'{i+1}. {x}' for i,x in enumerate(q['hints'])]+['','<details><summary>参考答案与解析（先自行完成）</summary>','']
  if q['type']=='选择':l+=['正确选项：'+q['correctOptionId']+'。'+q['referenceSolutions'][0]['answer'],'']
  else:
   for n,s in enumerate(q['referenceSolutions']):l+=['**'+('参考解（MySQL）' if n==0 else '等价解（'+s['kind']+'）')+'**','```sql',s['mysqlSql'],'```','']
   e=q['expectedOutput']
   if e['kind']=='query':
    l += [f'公开预期结果：{e["rowCount"]}行（前12行预览；JSON与公开夹具保存全量）。']+table(e['columns'],e['rows'][:12]);
    if not e['rows']:l+=['（空结果，不是NULL行。）']
   else:l+=['预期数据库状态（完整）：','```json',json.dumps(e['state'],ensure_ascii=False,indent=2),'```']
  l+=['',q['explanation'],'','常见错误：']+['- '+x['reason'] for x in q['commonErrors']]+['','兼容说明：'+q.get('compatibility','仅理论题。'),'','测试夹具：'+', '.join(q['testFixtureIds']),'</details>']
 p.write_text('\n'.join(l)+'\n',encoding='utf-8')
def main():
 print('准备基础资源',flush=True)
 for f in ['CURRICULUM.md','QUESTION_BANK.md','DATABASE.md','TECHNICAL.md']:shutil.copy2(UP/'docs'/f,ROOT/'reference'/f)
 for name in ['retail','inventory','ecommerce','finance','hr','procurement','enterprise']:
  src=UP/'databases'/name;dest=ROOT/'databases'/name;dest.mkdir(parents=True,exist_ok=True)
  for n in [name+'.sqlite','schema.json','DATA_DICTIONARY.md','README.md']:
   if (src/n).exists():shutil.copy2(src/n,dest/n)
  DB_HASH[name]=sha(dest/(name+'.sqlite'));assert DB_HASH[name]==sha(src/(name+'.sqlite'))
 qs=[make_query_material(q) for q in parse_source(ROOT/'validation/query_source.txt','query')+STATE]
 qs+=parse_source(ROOT/'validation/mc_source.txt','mc');assign_types(qs);records=[];num=0
 mysql_dir=ROOT/'validation'/'mysql_small';mysql_dir.mkdir(exist_ok=True)
 for ch in range(1,19):
  group=sorted([q for q in qs if q['ch']==ch],key=lambda q:(complexity(q),q['kp'],q['title']))
  assert len(group)==QUOTAS[ch-1]
  diffs=[name for name,n in zip(['入门','简单','中等','困难'],DIFF[ch-1]) for _ in range(n)];assert len(diffs)==len(group)
  for q,d in zip(group,diffs):
   num+=1;q['difficulty']=d;qid=f'q{num:04}'
   if q.get('mc'):r=mc_record(q,qid);r['title']=KP[q['kp']]['title']+' · '+q['req'][:26]+('…' if len(q['req'])>26 else '')
   else:
    r=normal_record(q,qid)
    if r['type']=='代码填空':
     left,right=r['initialCode'].split('{{blank1}}');alt=r['referenceSolutions'][1]['mysqlSql'];forms=r['acceptedBlanks'][0]['acceptedForms']
     if alt.startswith(left) and alt.endswith(right) and len(alt)>=len(left)+len(right):
      middle=alt[len(left): len(alt)-len(right) if right else len(alt)]
      if middle not in forms:forms.append(middle)
     assert r['initialCode'].replace('{{blank1}}',r['acceptedBlanks'][0]['referenceText'])==q['sql']
     r['fillEvaluation']={'method':'插入后完整SQL真实执行；可接受形式不限制等价片段','referenceReassembled':True}
    if r['type']=='代码纠错' and q['wrong_attempts'][0]['match']:r['context']+=' 初始代码在公开样本可能碰巧正确，仍需按业务边界纠正。'
    r['validation']['independentPythonAssertions']=independent_small(q,r['validation']['fixtures'])
    for f in q['fixture_data']:write_mysql_small(f,r,mysql_dir)
   records.append(r)
  print(f'第{ch:02}章 {len(group)}题已编制、测试',flush=True)
 assert num==250;J(ROOT/'validation'/'small_records.tmp.json',records)
 with tempfile.TemporaryDirectory() as td:
  conns={}
  for i in range(1,51):
   d=D(BUS/f'BP{i:02}.json');dbn=d['datasetId'].removesuffix('-v2')
   if dbn not in conns:
    p=Path(td)/(dbn+'.sqlite');shutil.copyfile(ROOT/'databases'/dbn/(dbn+'.sqlite'),p);conns[dbn]=register(sqlite3.connect(p,isolation_level=None))
   try:r=business_record(i,d,conns[dbn]);records.append(r);print(f'BP{i:02} {r["title"]}: 数据变体、正确解、等价解、错误解通过',flush=True)
   except Exception as e:print('业务验证失败',i,e,flush=True);raise
   J(ROOT/'validation'/'business_records.tmp.json',records[250:])
  for c in conns.values():c.close()
 finish(records)

def finish(records):
 assert len(records)==300 and len({r['id'] for r in records})==300
 assert collections.Counter(r['type'] for r in records)==TYPES,collections.Counter(r['type'] for r in records)
 assert collections.Counter(r['difficulty'] for r in records)==DIFF_TOTAL,collections.Counter(r['difficulty'] for r in records)
 assert [sum(r['chapterId']==f'ch{i:02}' for r in records) for i in range(1,21)]==QUOTAS
 assert len({re.sub(r'\s+','',r['requirements']) for r in records})==300
 dup=collections.defaultdict(list)
 for r in records:
  if r['type']!='选择':dup[re.sub(r'\s+',' ',r['referenceSolutions'][0]['mysqlSql'].strip().rstrip(';')).lower()].append([r['id'],r['title']])
 duplicate_sql=[v for v in dup.values() if len(v)>1]
 links={k:[] for k in KP};primary={k:[] for k in KP}
 for r in records:
  assert len(set(r['knowledgePointIds']))==len(r['knowledgePointIds']) and all(k in KP for k in r['knowledgePointIds']),(r['id'],r['knowledgePointIds'])
  for k in r['knowledgePointIds']:links[k].append(r['id'])
  for k in r['primaryKnowledgePointIds']:primary[k].append(r['id'])
  assert len(r['hints'])==3
  for f in r['testFixtures']:assert sha(ROOT/f['path'])==f['sha256']
 for ch in range(1,21):
  group=[r for r in records if r['chapterId']==f'ch{ch:02}'];J(ROOT/'questions'/f'ch{ch:02}.json',{'chapterId':f'ch{ch:02}','title':CH[ch-1]['title'],'count':len(group),'schemaVersion':3,'contentVersion':'3.0.0','questions':group});md_write(ch,group)
  J(ROOT/'course_links'/f'ch{ch:02}.json',{'chapterId':f'ch{ch:02}','contentVersion':'3.0.0','knowledgePointUpdates':[{'id':k['id'],'relatedQuestionIds':links[k['id']],'primaryQuestionIds':primary[k['id']]} for k in CH[ch-1]['knowledgePoints']]})
 cov={'all':len(KP),'direct':sum(bool(v) for v in primary.values()),'related':sum(bool(v) for v in links.values()),'unlinked':[k for k,v in links.items() if not v]}
 J(ROOT/'course_links'/'knowledge_point_question_index.json',{'coverage':cov,'links':{k:{'relatedQuestionIds':v,'primaryQuestionIds':primary[k]} for k,v in links.items()}})
 # 教学夹具沿用课程字段定义；企业字段使用对应库的数据字典。
 J(ROOT/'fixtures'/'teaching_schema.json',{'version':'3.0.0','tables':{t:{'name':t,'caption':SQL_NAMES.get(t,t),'columns':f['columns'],'currency':'教学模拟元，与企业整数马币分隔离'} for t,f in FIX.items()}})
 J(ROOT/'databases'/'manifest.json',{'version':'2.0.0','note':'七份数据库与上一阶段逐文件哈希一致；开发时可复用。','datasets':[{'id':n+'-v2','path':f'databases/{n}/{n}.sqlite','sha256':DB_HASH[n],'bytes':(ROOT/'databases'/n/(n+'.sqlite')).stat().st_size} for n in DB_HASH]})
 val={'engine':'SQLite '+sqlite3.sqlite_version,'nativeMySQL':'未运行','browserWasm':'未运行','questionCount':300,'chapterCounts':QUOTAS,'typeCounts':dict(collections.Counter(r['type'] for r in records)),'difficultyCounts':dict(collections.Counter(r['difficulty'] for r in records)),'coverage':cov,'duplicateRequirementCount':0,'duplicateReferenceSql':duplicate_sql,'nonObjectiveCases':sum(len(r['validation']['fixtures']) for r in records if r['type']!='选择'),'choiceKeys':30,'equivalentKinds':dict(collections.Counter(r['validation'].get('equivalentKind') for r in records if r['type']!='选择')),'independentPythonAsserts':sum(len(r['validation'].get('independentPythonAssertions',[])) for r in records),'independentBusinessAsserts':sum(bool(f.get('independentAssertion')) for r in records if r['type']!='选择' for f in r['validation']['fixtures']),'questions':[{'id':r['id'],'type':r['type'],'chapterId':r['chapterId'],'validation':r['validation'],'requiredAstFeatures':r['requiredAstFeatures'],'mysqlVerification':r['mysqlVerification']} for r in records]}
 J(ROOT/'validation'/'execution.json',val)
 J(ROOT/'validation'/'distribution.json',{'chapterCounts':QUOTAS,'types':TYPES,'difficulty':DIFF_TOTAL,'matrix':[{'chapterId':f'ch{i:02}','count':QUOTAS[i-1],'typeCounts':dict(collections.Counter(r['type'] for r in records if r['chapterId']==f'ch{i:02}')),'difficultyCounts':dict(collections.Counter(r['difficulty'] for r in records if r['chapterId']==f'ch{i:02}'))} for i in range(1,21)]})
 J(ROOT/'manifest.json',{'schemaVersion':3,'contentVersion':'3.0.0','questionCount':300,'chapterCount':20,'questionTypeCounts':TYPES,'difficultyCounts':DIFF_TOTAL,'businessTasks':50,'knowledgePointCoverage':cov,'resourcePolicy':'内置只读版本资源，按需加载；课程编号不改。','chapters':[{'id':f'ch{i:02}','path':f'questions/ch{i:02}.json','count':QUOTAS[i-1],'sha256':sha(ROOT/'questions'/f'ch{i:02}.json')} for i in range(1,21)],'fixtures':{'root':'fixtures/','idToPathPattern':'fixtures/{questionId}/{fixtureId}.json','eachQuestionIdStable':True},'datasetsManifest':'databases/manifest.json','courseLinks':'course_links/knowledge_point_question_index.json','validationReport':'validation/REPORT.md','releaseGate':'仍需MySQL原生、手机WASM及专项AST验收'})
 print('SUMMARY',json.dumps({k:v for k,v in val.items() if k!='questions'},ensure_ascii=False),flush=True)
if __name__=='__main__':main()
