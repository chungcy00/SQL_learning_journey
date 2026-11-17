from pathlib import Path
import json, sqlite3, math, datetime, hashlib, shutil, tempfile, collections, sys
ROOT=Path(__file__).resolve().parents[1]
D=lambda p:json.loads(Path(p).read_text(encoding="utf-8"))
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
 def row_equal(x,y):return len(x)==len(y) and all(closeval(u,v) for u,v in zip(x,y))
 if ordered:return all(row_equal(x,y) for x,y in zip(a['rows'],b['rows']))
 # Exact multiset fast path. Otherwise use one-to-one matching within nonnumeric buckets.
 def exactkey(row):return tuple(('n',v) if isinstance(v,(int,float)) else ('null',None) if v is None else ('s',v) for v in row)
 if collections.Counter(map(exactkey,a['rows']))==collections.Counter(map(exactkey,b['rows'])):return True
 def bucket(row):return tuple(('n',None) if isinstance(v,(int,float)) else ('null',None) if v is None else ('s',v) for v in row)
 aa=collections.defaultdict(list);bb=collections.defaultdict(list)
 for row in a['rows']:aa[bucket(row)].append(row)
 for row in b['rows']:bb[bucket(row)].append(row)
 if aa.keys()!=bb.keys():return False
 for key,xs in aa.items():
  ys=bb[key]
  if len(xs)!=len(ys):return False
  adj=[[j for j,y in enumerate(ys) if row_equal(x,y)] for x in xs]
  match={}
  def augment(i,seen):
   for j in adj[i]:
    if j in seen:continue
    seen.add(j)
    if j not in match or augment(match[j],seen):match[j]=i;return True
   return False
  if any(not augment(i,set()) for i in sorted(range(len(xs)),key=lambda i:len(adj[i]))):return False
 return True

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

def main():
 m=D(ROOT/'manifest.json'); count=collections.Counter(); qs=[]
 for ch in m['chapters']:
  p=ROOT/ch['path'];assert sha(p)==ch['sha256'];data=D(p);assert len(data['questions'])==ch['count'];qs+=data['questions']
 assert len(qs)==300 and len({q['id'] for q in qs})==300
 assert dict(collections.Counter(q['type'] for q in qs))==m['questionTypeCounts']
 assert dict(collections.Counter(q['difficulty'] for q in qs))==m['difficultyCounts']
 for d in D(ROOT/'databases/manifest.json')['datasets']:assert sha(ROOT/d['path'])==d['sha256'];count['databaseHashes']+=1
 with tempfile.TemporaryDirectory() as td:
  dbs={}
  for q in qs:
   assert len(q['hints'])==3 and q['requirements'] and q['explanation'] and q['commonErrors']
   rejected=False; pub=None; changed=False
   for meta in q['testFixtures']:
    p=ROOT/meta['path'];assert sha(p)==meta['sha256']; f=D(p);assert f['questionId']==q['id'];count['fixtureHashes']+=1
    if f['kind']=='objective_choice':
     ids=[o['id'] for o in q['options']];a=f['expected']['correctOptionId'];assert len(set(ids))==4 and a in ids and a==q['correctOptionId'] and a==q['referenceSolutions'][0]['optionId'];assert len({o['text'] for o in q['options']})==4;count['objectiveKeys']+=1;continue
    state=f['kind']=='database_state'; ordered=f['orderPolicy']=='ordered'; expected=f['expected']
    def matches(x):return x==expected if state else equal(x,expected,ordered)
    if f['visibility']=='public':pub=expected
    elif pub is not None and not (pub==expected if state else equal(pub,expected,ordered)):changed=True
    if f['kind']=='database_delta':
     dbkey=f['baseDatabase']
     if dbkey not in dbs:
      dest=Path(td)/Path(dbkey).name;shutil.copyfile(ROOT/dbkey,dest);dbs[dbkey]=register(sqlite3.connect(dest,isolation_level=None))
     c=dbs[dbkey];apply_patch(c,f['patchSql']);runner=lambda s:run(c,s)
    else:runner=lambda s:run_small(f,s,state)
    try:
     for sol in q['referenceSolutions'][:2]:assert matches(runner(sol['sqliteSql'])),(q['id'],meta['id'],sol['kind']);count['solutionRuns']+=1
     bad=next(x['sql'] for x in q['commonErrors'] if x.get('sql'))
     try:badmatch=matches(runner(bad))
     except sqlite3.Error:badmatch=False
     if not badmatch:rejected=True;count['wrongRejectedCases']+=1
     count['wrongRuns']+=1;count['executedFixtures']+=1
    finally:
     if f['kind']=='database_delta':revert_patch(c)
   if q['type']!='选择':assert rejected,q['id'];count['questionsRejectingWrong']+=1;count['questionsWithChangedHiddenResult']+=int(changed)
   count['questions']+=1
   if count['questions']%50==0:print('复验题数',count['questions'],flush=True)
  for c in dbs.values():c.close()
 result={'engine':'SQLite '+sqlite3.sqlite_version,'passed':True,**count};print(json.dumps(result,ensure_ascii=False),flush=True);(ROOT/'validation'/'recheck.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
