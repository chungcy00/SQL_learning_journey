#!/usr/bin/env python3
# 仅检查发行包文件完整性，不执行SQL，不修改资源。
from pathlib import Path
import json,hashlib,collections
R=Path(__file__).resolve().parent
load=lambda p:json.loads(p.read_text(encoding='utf-8'))
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
m=load(R/'DELIVERY_MANIFEST.json')
for x in m['files']:
 p=R/x['path'];assert p.is_file() and p.stat().st_size==x['bytes'] and sha(p)==x['sha256'],x['path']
C=R/'CONTENT';Q=C/'question_resource';F=C/'flashcard_resource';K=C/'courses_resource'
ks=[];qs=[];fs=[]
for i in range(1,21):
 for root,sub,key,out in [(K,'courses','knowledgePoints',ks),(Q,'questions','questions',qs),(F,'flashcards','cards',fs)]:out.extend(load(root/sub/f'ch{i:02}.json')[key])
assert len(ks)==len({k['id'] for k in ks})==152
assert len(qs)==len({q['id'] for q in qs})==300
assert len(fs)==len({f['id'] for f in fs})==200
for k in ks:
 assert set(k['relatedQuestionIds'])=={q['id'] for q in qs if k['id'] in q['knowledgePointIds']}
 assert set(k['relatedFlashcardIds'])=={f['id'] for f in fs if k['id'] in f['knowledgePointIds']}
for q in qs:
 assert set(q['relatedFlashcardIds'])=={f['id'] for f in fs if q['id'] in f['relatedQuestionIds']}
 for v in q['testFixtures']:assert sha(Q/v['path'])==v['sha256']
for d in load(Q/'databases/manifest.json')['datasets']:assert sha(Q/d['path'])==d['sha256']
assert sum(len(q['testFixtures']) for q in qs)==776
print(f"交付清单 {len(m['files'])} 项通过；152知识点、300题、200卡、776夹具、7库通过结构及引用检查。此检查未运行SQL或手机端。")
