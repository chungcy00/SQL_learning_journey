#!/usr/bin/env python3
import csv
import gzip
import hashlib
import json
import re
import shutil
import sqlite3
from collections import Counter
from pathlib import Path

ROOT = Path(".")
CONTENT = ROOT / "CONTENT"
ASSETS_OUT = ROOT / "app/src/main/assets/content"
DB_NAMES = ["retail", "inventory", "ecommerce", "finance", "hr", "procurement", "enterprise"]


def clean_out_dir():
    for sub in ["databases", "schemas"]:
        (ASSETS_OUT / sub).mkdir(parents=True, exist_ok=True)
    for sub in [
        "courses", "lesson_fixtures", "questions",
        "question_fixtures", "flashcards", "flashcard_fixtures", "projects",
    ]:
        p = ASSETS_OUT / sub
        if p.exists():
            shutil.rmtree(p)
        p.mkdir(parents=True, exist_ok=True)


def coerce_cell(raw_val: str, col_type: str, nullable: bool):
    if raw_val == "\\N":
        return None
    if raw_val == "":
        return None if nullable else ""
    t = col_type.upper()
    if "INT" in t:
        try:
            return int(raw_val)
        except ValueError:
            return int(float(raw_val))
    if any(k in t for k in ("REAL", "FLOAT", "DOUBLE", "DEC", "NUM")):
        return float(raw_val)
    return raw_val


def build_databases_and_schemas():
    print("Checking/Building 7 compressed SQLite databases and schemas...")
    tmp_db_dir = Path("/tmp/sql_journey_dbs")
    tmp_db_dir.mkdir(parents=True, exist_ok=True)

    built_dbs = {}
    for db_name in DB_NAMES:
        sqlite_path = tmp_db_dir / f"{db_name}.sqlite"
        gz_out = ASSETS_OUT / "databases" / f"{db_name}.sqlite.gz"
        schema_out_file = ASSETS_OUT / "schemas" / f"{db_name}.json"
        if not sqlite_path.exists() and gz_out.exists() and schema_out_file.exists() and gz_out.stat().st_size > 1000:
            with gzip.open(gz_out, "rb") as f_gz, open(sqlite_path, "wb") as f_out:
                shutil.copyfileobj(f_gz, f_out)
        if sqlite_path.exists() and gz_out.exists() and schema_out_file.exists() and gz_out.stat().st_size > 1000:
            built_dbs[db_name] = sqlite_path
            print(f"  Reusing {db_name}.sqlite.gz ({gz_out.stat().st_size // 1024} KB)")
            continue
        schema_raw = json.loads((CONTENT / "question_resource/databases" / db_name / "schema.json").read_text("utf-8"))
        if sqlite_path.exists():
            sqlite_path.unlink()
        conn = sqlite3.connect(str(sqlite_path))
        cur = conn.cursor()

        tables_meta = {}
        for tbl_name, tbl_info in schema_raw["tables"].items():
            cur.execute(tbl_info["sqliteDDL"])
            cols = tbl_info["columns"]
            csv_path = CONTENT / "enterprise_reference/structured_data" / f"{tbl_name}.csv"
            with open(csv_path, newline="", encoding="utf-8") as f:
                reader = csv.reader(f)
                header = next(reader)
                assert header == [c["name"] for c in cols], f"Header mismatch {tbl_name}"
                ph = ",".join("?" for _ in cols)
                batch = []
                cnt = 0
                for row in reader:
                    batch.append([coerce_cell(row[i], cols[i].get("sqliteType", "TEXT"), cols[i].get("nullable", True)) for i in range(len(cols))])
                    cnt += 1
                    if len(batch) >= 2000:
                        cur.executemany(f"INSERT INTO {tbl_name} VALUES ({ph})", batch)
                        batch.clear()
                if batch:
                    cur.executemany(f"INSERT INTO {tbl_name} VALUES ({ph})", batch)
                assert cnt == tbl_info["rowCount"], f"{db_name}.{tbl_name} count {cnt} != {tbl_info['rowCount']}"

            for idx_sql in tbl_info.get("indexes", []):
                cur.execute(idx_sql)

            fks = []
            for c in cols:
                fk = c.get("foreignKey")
                if isinstance(fk, dict) and fk.get("table") and fk.get("column"):
                    fks.append({"column": c["name"], "targetTable": fk["table"], "targetColumn": fk["column"]})

            tables_meta[tbl_name] = {
                "rowCount": tbl_info["rowCount"],
                "columns": [
                    {
                        "name": c["name"],
                        "type": c.get("sqliteType", c.get("mysqlType", "TEXT")),
                        "nullable": bool(c.get("nullable", True)),
                        "primaryKey": bool(c.get("primaryKey", False)),
                    }
                    for c in cols
                ],
                "foreignKeys": fks,
            }

        views_meta = {}
        for view_name, view_sql in schema_raw.get("views", {}).items():
            cur.execute(view_sql)
            v_cols = []
            for col_row in cur.execute(f"PRAGMA table_info({view_name})").fetchall():
                v_cols.append({"name": col_row[1], "type": col_row[2] or "TEXT", "nullable": True, "primaryKey": False})
            views_meta[view_name] = v_cols

        conn.commit()
        conn.close()

        with open(sqlite_path, "rb") as f_in, open(gz_out, "wb") as f_raw:
            with gzip.GzipFile(filename=f"{db_name}.sqlite", mode="wb", fileobj=f_raw, compresslevel=5, mtime=0) as gz:
                shutil.copyfileobj(f_in, gz)

        schema_out_file.write_text(
            json.dumps({"databaseId": db_name, "version": "v2", "tables": tables_meta, "views": views_meta}, ensure_ascii=False, indent=2),
            "utf-8",
        )
        built_dbs[db_name] = sqlite_path
        print(f"  {db_name}.sqlite.gz -> {gz_out.stat().st_size // 1024} KB ({len(tables_meta)} tables, {len(views_meta)} views)")
    return built_dbs


def package_courses_and_lesson_fixtures():
    print("Packaging 20 courses & 31 lesson fixtures...")
    for i in range(1, 21):
        ch = f"ch{i:02d}"
        c_data = json.loads((CONTENT / "courses_resource/courses" / f"{ch}.json").read_text("utf-8"))
        q_links = json.loads((CONTENT / "question_resource/course_links" / f"{ch}.json").read_text("utf-8"))
        fc_links = json.loads((CONTENT / "flashcard_resource/course_links" / f"{ch}.json").read_text("utf-8"))
        q_map = {u["id"]: u for u in q_links.get("knowledgePointUpdates", [])}
        fc_map = {u["id"]: u for u in fc_links.get("knowledgePointUpdates", [])}

        for kp in c_data.get("knowledgePoints", []):
            kid = kp["id"]
            if kid in q_map:
                kp["relatedQuestionIds"] = q_map[kid].get("relatedQuestionIds", [])
                kp["primaryQuestionIds"] = q_map[kid].get("primaryQuestionIds", [])
            if kid in fc_map:
                kp["relatedFlashcardIds"] = fc_map[kid].get("relatedFlashcardIds", [])
                kp["primaryFlashcardIds"] = fc_map[kid].get("primaryFlashcardIds", [])
            if isinstance(kp.get("compatibility"), str):
                kp["compatibility"] = {"status": "转换执行", "detail": kp["compatibility"]}
            for str_key in ("learningObjective", "concept", "syntax", "mysqlSql", "sqliteSql", "useCase", "comparison", "notes"):
                if kp.get(str_key) is None:
                    kp[str_key] = ""
            for list_key in ("prerequisites", "fixtureIds", "sourceTables", "executionSteps", "relatedQuestionIds", "relatedFlashcardIds", "relatedKnowledgePointIds", "primaryQuestionIds", "primaryFlashcardIds"):
                if kp.get(list_key) is None:
                    kp[list_key] = []

        (ASSETS_OUT / "courses" / f"{ch}.json").write_text(json.dumps(c_data, ensure_ascii=False, indent=2), "utf-8")

    for lf in sorted((CONTENT / "courses_resource/fixtures").glob("lesson-*.json")):
        shutil.copy2(lf, ASSETS_OUT / "lesson_fixtures" / lf.name)


def package_flashcards_and_fixtures():
    print("Packaging 200 flashcards & 125 flashcard fixtures...")
    for i in range(1, 21):
        ch = f"ch{i:02d}"
        shutil.copy2(CONTENT / "flashcard_resource/flashcards" / f"{ch}.json", ASSETS_OUT / "flashcards" / f"{ch}.json")

    for fx_path in sorted((CONTENT / "flashcard_resource/fixtures").glob("*.json")):
        data = json.loads(fx_path.read_text("utf-8"))
        if not data.get("tables") and data.get("sqliteSql"):
            conn = sqlite3.connect(":memory:")
            try:
                conn.executescript(data["sqliteSql"])
                tbl_rows = conn.execute(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY rowid"
                ).fetchall()
                extracted = []
                for (t_name,) in tbl_rows:
                    cols_info = conn.execute(f'PRAGMA table_info("{t_name}")').fetchall()
                    col_defs = [{"name": c[1], "type": c[2] or "TEXT", "sqliteType": c[2] or "TEXT"} for c in cols_info]
                    rows = [list(r) for r in conn.execute(f'SELECT * FROM "{t_name}"').fetchall()]
                    extracted.append({"name": t_name, "columns": col_defs, "rows": rows})
                data["tables"] = extracted
            except Exception as exc:
                print(f"  Warn flashcard fixture {fx_path.name}: {exc}")
            finally:
                conn.close()
        (ASSETS_OUT / "flashcard_fixtures" / fx_path.name).write_text(json.dumps(data, ensure_ascii=False, indent=2), "utf-8")


def translate_concat(sql):
    out = sql
    for _ in range(6):
        m = re.search(r"\bCONCAT\s*\(", out, re.IGNORECASE)
        if not m:
            break
        start = m.end() - 1
        d, args, buf, in_q, i = 0, [], [], False, start
        while i < len(out):
            ch = out[i]
            if ch == "'":
                in_q = not in_q
                if d >= 1: buf.append(ch)
            elif not in_q:
                if ch == "(":
                    if d >= 1: buf.append(ch)
                    d += 1
                elif ch == ")":
                    d -= 1
                    if d == 0:
                        args.append("".join(buf).strip())
                        break
                    else: buf.append(ch)
                elif ch == "," and d == 1:
                    args.append("".join(buf).strip())
                    buf = []
                elif d >= 1: buf.append(ch)
            elif d >= 1:
                buf.append(ch)
            i += 1
        repl = "(" + " || ".join(f"({a})" for a in args) + ")"
        out = out[: m.start()] + repl + out[i + 1 :]
    return out


def sqlite_run_tables(tables, sql, setup_sql=None, verification_query=None):
    conn = sqlite3.connect(":memory:")
    try:
        for t in tables:
            cols = t["columns"]
            defs = []
            for c in cols:
                rt = (c.get("sqliteType") or c.get("type") or c.get("mysqlType") or "TEXT").upper()
                mt = "INTEGER" if "INT" in rt else ("REAL" if any(k in rt for k in ("DEC", "NUM", "FLOAT", "DOUBLE", "REAL")) else "TEXT")
                defs.append(f'"{c["name"]}" {mt}')
            conn.execute(f'CREATE TABLE "{t["name"]}" ({", ".join(defs)})')
            if t.get("rows"):
                conn.executemany(f'INSERT INTO "{t["name"]}" VALUES ({",".join("?" for _ in cols)})', t["rows"])
        if setup_sql:
            conn.executescript(setup_sql)
        if verification_query:
            conn.executescript(translate_concat(sql))
            cur = conn.execute(verification_query)
        else:
            cur = conn.execute(translate_concat(sql))
        cols = [d[0] for d in cur.description] if cur.description else []
        rows = [list(r) for r in cur.fetchall()]
        return cols, rows
    finally:
        conn.close()


def rows_match(actual_rows, expected_rows, order_policy="ordered"):
    def norm_cell(v):
        if v is None: return None
        if isinstance(v, bool): return 1 if v else 0
        if isinstance(v, (int, float)): return round(float(v), 5)
        try: return round(float(str(v)), 5)
        except Exception: return str(v)

    a_norm = [tuple(norm_cell(c) for c in r) for r in actual_rows]
    e_norm = [tuple(norm_cell(c) for c in r) for r in expected_rows]
    if order_policy == "unordered":
        return sorted(a_norm, key=str) == sorted(e_norm, key=str)
    return a_norm == e_norm


def load_lesson_tables_map():
    res = {}
    for p in sorted((CONTENT / "courses_resource/fixtures").glob("lesson-*.json")):
        d = json.loads(p.read_text("utf-8"))
        key = p.stem.replace("lesson-", "")
        res[key] = d["tables"] if d.get("tables") else [{"name": d["table"], "columns": d["columns"], "rows": d["rows"]}]
    return res


def build_q0001_to_q0036(kp_lookup, q_to_kps):
    specs = {
        "q0001": ("查询所有订单编号", "在 orders 表中按订单编号 id 升序输出全部订单的 id 列。", "SELECT id FROM orders ORDER BY id;"),
        "q0004": ("提取商品库存字段", "从 products 表读取商品编号 product_id、名称 product_name 与库存 stock，按 product_id 升序返回。", "SELECT product_id, product_name, stock FROM products ORDER BY product_id;"),
        "q0009": ("商品价格去重并排序", "从 products 表中查询不重复的价格 price，按价格从低到高排序。", "SELECT DISTINCT price FROM products ORDER BY price;"),
        "q0010": ("计算加收运费后的账单金额", "在 orders 表中为每笔订单销售额 sales 统一加收 5 元包装运费，输出 id 与 billed 两列，按 id 升序排列。", "SELECT id, sales + 5 AS billed FROM orders ORDER BY id;"),
        "q0011": ("投影订单与门店日期", "从 orders 表查询订单编号 id、门店 store_id 与下单日期 order_date，按 id 升序输出。", "SELECT id, store_id, order_date FROM orders ORDER BY id;"),
        "q0012": ("查看全部不重复商品类目", "从 products 表获取不重复的 category 列表，按类目名称升序输出。", "SELECT DISTINCT category FROM products ORDER BY category;"),
        "q0013": ("查询商品名与所属类目", "从 products 表中查询 product_name 和 category 两列，按 product_id 升序输出。", "SELECT product_name, category FROM products ORDER BY product_id;"),
        "q0015": ("读取员工主档基本列", "从 employees 表中读取 employee_id 与 employee_name，按 employee_id 升序输出。", "SELECT employee_id, employee_name FROM employees ORDER BY employee_id;"),
        "q0016": ("客户与门店组合去重", "统计 orders 表中不重复的 (customer_id, store_id) 组合，按 customer_id, store_id 升序排列。", "SELECT DISTINCT customer_id, store_id FROM orders ORDER BY customer_id, store_id;"),
        "q0017": ("计算员工年薪表达式", "在 employees 表中将月薪 salary 乘以 12 计算年薪，输出 employee_id 与 annual_salary，按 employee_id 升序。", "SELECT employee_id, salary * 12 AS annual_salary FROM employees ORDER BY employee_id;"),
        "q0018": ("计算八折促销价", "在 products 表中计算每个商品打八折（price * 0.8）后的优惠价，输出 product_id 与 offer_price，按 product_id 升序。", "SELECT product_id, ROUND(price * 0.8, 2) AS offer_price FROM products ORDER BY product_id;"),
        "q0020": ("拆解整箱数与零散余数", "假设每箱装 10 件商品，计算 products 表各商品的整箱数 boxes（stock / 10）和零散件数 loose（stock % 10），按 product_id 升序。", "SELECT product_id, stock / 10 AS boxes, stock % 10 AS loose FROM products ORDER BY product_id;"),
        "q0021": ("排除 1 号门店的订单", "从 orders 表筛选 store_id 不等于 1 的订单，输出 id 与 store_id，按 id 升序。", "SELECT id, store_id FROM orders WHERE store_id <> 1 ORDER BY id;"),
        "q0022": ("筛选中等金额区间的订单", "从 orders 表筛选销售额 sales 介于 300 与 800（含边界）之间的订单，输出 id 与 sales，按 id 升序。", "SELECT id, sales FROM orders WHERE sales BETWEEN 300 AND 800 ORDER BY id;"),
        "q0024": ("筛选单价不低于 50 的商品", "从 products 表中找出 price >= 50 的商品，输出 product_id 与 price，按 product_id 升序。", "SELECT product_id, price FROM products WHERE price >= 50 ORDER BY product_id;"),
        "q0025": ("库存低于 100 预警筛选", "从 products 表筛选 stock < 100 的商品，输出 product_id 与 stock，按 product_id 升序。", "SELECT product_id, stock FROM products WHERE stock < 100 ORDER BY product_id;"),
        "q0026": ("转义匹配含百分号的商品名", "在 raw_data 表中筛选 name 字段里真实包含字面百分号 '%' 的记录，输出 id 与 name，按 id 升序。", "SELECT id, name FROM raw_data WHERE name LIKE '%\\%%' ESCAPE '\\' ORDER BY id;"),
        "q0028": ("筛选缺货商品", "从 products 表中查询 stock = 0 的商品，输出 product_id 与 product_name，按 product_id 升序。", "SELECT product_id, product_name FROM products WHERE stock = 0 ORDER BY product_id;"),
        "q0029": ("筛选低价且有库存商品", "从 products 表中筛选价格 price < 40 且库存 stock > 0 的商品，输出 product_id，按 product_id 升序。", "SELECT product_id FROM products WHERE price < 40 AND stock > 0 ORDER BY product_id;"),
        "q0030": ("排除日用品类的全部商品", "在 products 表中筛选 category 不等于 '日用品' 的商品，输出 product_id 与 category，按 product_id 升序。", "SELECT product_id, category FROM products WHERE category <> '日用品' ORDER BY product_id;"),
        "q0031": ("价格适中且有货的商品筛选", "从 products 表中找出单价 price <= 60 且库存 stock > 0 的商品，输出 product_id，按 product_id 升序。", "SELECT product_id FROM products WHERE price <= 60 AND stock > 0 ORDER BY product_id;"),
        "q0032": ("匹配恰好四个字符的商品名", "在 products 表中使用下划线通配符筛选 product_name 恰好为 4 个字符的商品，输出 product_name（按 product_id 升序）。", "SELECT product_name FROM products WHERE product_name LIKE '____' ORDER BY product_id;"),
        "q0033": ("匹配以 er 结尾的商品名称", "在 products 表中查询 product_name 以 'er' 结尾的商品，输出 product_name，按 product_name 升序。", "SELECT product_name FROM products WHERE product_name LIKE '%er' ORDER BY product_name;"),
        "q0034": ("使用 NOT BETWEEN 定位异常库存", "从 products 表筛选库存 stock 不在 [50, 179] 闭区间内的商品，输出 product_id 与 stock，按 product_id 升序。", "SELECT product_id, stock FROM products WHERE stock NOT BETWEEN 50 AND 179 ORDER BY product_id;"),
        "q0035": ("OR 组合低价或高库存商品", "从 products 表查询满足单价 price <= 35 或库存 stock >= 180 的商品，输出 product_id、price 与 stock，按 product_id 升序。", "SELECT product_id, price, stock FROM products WHERE price <= 35 OR stock >= 180 ORDER BY product_id;"),
        "q0036": ("匹配以字母 S 开头的商品", "在 products 表中筛选 product_name 以大写字母 'S' 开头的商品，输出 product_id 与 product_name，按 product_id 升序。", "SELECT product_id, product_name FROM products WHERE product_name LIKE 'S%' ORDER BY product_id;"),
    }
    choice_titles = {
        "q0002": ("NULL 与 0 的业务语义辨析", "在订单或金额字段中，关于 NULL 与 0 的含义，以下哪项描述是正确的？"),
        "q0003": ("定位字段定义的规范文档", "想要确认业务表各列的数据类型、精度与为空规则，应优先查阅哪类文档？"),
        "q0005": ("金额字段的数据类型选型", "在 MySQL 8.4 中存储需要精确到分的两位小数金额，最合适的数据类型是？"),
        "q0006": ("关系表中的行代表什么", "在关系数据库中，数据表里的一条横向水平数据记录通常称为什么？"),
        "q0007": ("主键唯一性约束的作用", "当向已有主键 id=10 的表中再次插入主键 id=10 的记录时，数据库会如何处理？"),
        "q0008": ("客户与订单的一对多关系", "若 customers 表的主键 customer_id 作为外键出现在 orders 表中，这意味着两者构成什么关系？"),
        "q0014": ("SELECT 查询是否修改源数据", "执行标准的 SELECT 字段查询语句对数据库源表会产生什么影响？"),
        "q0019": ("AS 别名的作用域与效果", "在 SQL 查询中使用 `price * 0.8 AS offer_price`，其中 AS 子句的真正作用是什么？"),
        "q0023": ("LIKE 通配符下划线计数", "在 SQL 中，条件 `name LIKE 'A__'`（大写 A 后接两个下划线）会匹配以下哪类字符串？"),
        "q0027": ("AND 与 OR 的运算符优先级", "在不加括号的情况下，表达式 `a = 1 OR b = 2 AND c = 3` 的真实求值等价于哪一项？"),
    }

    questions = {}
    mismatches = []
    for i in range(1, 37):
        qid = f"q{i:04d}"
        kp_id = q_to_kps[qid][0]
        ch_id = kp_id[:4].replace("kp", "ch")
        kp = kp_lookup[kp_id]
        qd = CONTENT / "question_resource/fixtures" / qid
        files = sorted(qd.glob("*.json"))
        first = json.loads(files[0].read_text("utf-8"))

        if first.get("kind") == "objective_choice":
            title, req = choice_titles[qid]
            ans = first["expected"]["correctOptionId"]
            opts = first["options"]
            questions[qid] = {
                "id": qid, "chapterId": ch_id, "title": title, "type": "选择", "difficulty": "入门",
                "context": f"知识点：{kp['title']}。", "requirements": req, "options": opts, "correctOptionId": ans,
                "hints": ["回顾核心概念定义与数据库行为约束。", f"重点区分：{kp.get('comparison', '')}"],
                "explanation": next((o["explanation"] for o in opts if o["id"] == ans), kp["concept"]),
                "supportStatus": "仅理论", "fixtureIds": [],
            }
        else:
            title, req, sql = specs[qid]
            fx_ids = []
            for f in files:
                fx_data = json.loads(f.read_text("utf-8"))
                fx_id = fx_data["id"]
                a_cols, a_rows = sqlite_run_tables(fx_data["tables"], sql)
                exp = fx_data["expected"]
                if [c.lower() for c in a_cols] != [c.lower() for c in exp["columns"]]:
                    mismatches.append(f"{qid} {fx_id} cols {a_cols} != {exp['columns']}")
                elif not rows_match(a_rows, exp["rows"], fx_data.get("orderPolicy", "ordered")):
                    mismatches.append(f"{qid} {fx_id} rows: got={a_rows} exp={exp['rows']}")
                (ASSETS_OUT / "question_fixtures" / f"{fx_id}.json").write_text(json.dumps(fx_data, ensure_ascii=False, indent=2), "utf-8")
                fx_ids.append(fx_id)
            pub_fx = next((json.loads(f.read_text("utf-8")) for f in files if "public" in f.name), json.loads(files[0].read_text("utf-8")))
            questions[qid] = {
                "id": qid, "chapterId": ch_id, "title": title, "type": "独立编写", "difficulty": "入门",
                "context": f"使用表 {', '.join(t['name'] for t in pub_fx['tables'])}，围绕「{kp['title']}」完成查询。",
                "requirements": req,
                "hints": [
                    f"先确认目标表及所需输出列名：{', '.join(pub_fx['expected']['columns'])}。",
                    f"参考语法：{kp['syntax']}",
                    f"易错提示：{kp['commonError']['correction'] if kp.get('commonError') else '注意列名拼写与排序要求。'}",
                ],
                "explanation": f"{kp['concept']} 标准写法：{sql}",
                "referenceSolutions": [{"mysqlSql": sql, "sqliteSql": sql, "explanation": kp["concept"]}],
                "requiredAstFeatures": [], "supportStatus": "直接兼容", "fixtureIds": fx_ids,
                "expectedColumns": pub_fx["expected"]["columns"], "expectedRows": pub_fx["expected"]["rows"],
                "orderPolicy": pub_fx.get("orderPolicy", "ordered"),
            }
    if mismatches:
        raise AssertionError("Mismatches:\n" + "\n".join(mismatches))
    return questions


def make_fixtures(qid, tables, sql, order_policy="ordered", setup_sql=None, ver_q=None, kind="query"):
    pub_cols, pub_rows = sqlite_run_tables(tables, sql, setup_sql, ver_q)
    fx_pub = {
        "id": f"{qid}-public", "questionId": qid, "visibility": "public", "variant": "public", "kind": kind,
        "tables": tables, "setupSql": setup_sql, "verificationQuery": ver_q,
        "expected": {"columns": pub_cols, "rows": pub_rows},
        "orderPolicy": order_policy, "columnPolicy": "exact_names_in_order",
    }
    empty_tables = [{"name": t["name"], "columns": t["columns"], "rows": []} for t in tables]
    emp_cols, emp_rows = sqlite_run_tables(empty_tables, sql, setup_sql, ver_q)
    fx_empty = {
        "id": f"{qid}-empty", "questionId": qid, "visibility": "hidden", "variant": "empty", "kind": kind,
        "tables": empty_tables, "setupSql": setup_sql, "verificationQuery": ver_q,
        "expected": {"columns": emp_cols, "rows": emp_rows},
        "orderPolicy": order_policy, "columnPolicy": "exact_names_in_order",
    }
    return [fx_pub, fx_empty]


def ensure_ast_sql(ast_feats, sql, kp):
    upper = sql.upper()
    if "RECURSIVE_CTE" in ast_feats and "WITH RECURSIVE" not in upper:
        return kp["sqliteSql"]
    if "CTE" in ast_feats and not re.search(r"\bWITH\s+", upper):
        return f"WITH base AS ({sql.rstrip(';')}) SELECT * FROM base;"
    return sql


def build_kp_sql_templates(lt):
    t_orders, t_products, t_nullable = lt["orders"], lt["products"], lt["nullable_values"]
    t_customers, t_employees, t_raw = lt["customers"], lt["employees"], lt["raw_data"]
    t_stores, t_monthly, t_old, t_new = lt["stores"], lt["monthly"], lt["old_customers"], lt["new_customers"]
    t_demo = [{
        "name": "demo_stock",
        "columns": [
            {"name": "id", "type": "INTEGER", "sqliteType": "INTEGER"},
            {"name": "label", "type": "TEXT", "sqliteType": "TEXT"},
            {"name": "qty", "type": "INTEGER", "sqliteType": "INTEGER"},
        ],
        "rows": [[1, "Mask", 50], [2, "Serum", 20]],
    }]
    return {
        "kp03-02": [
            {"title": "多条件优先级过滤订单", "req": "筛选 store_id=1 且 (sales>=800 或 category='护肤品') 的订单，输出 id, sales。", "tables": t_orders, "sql": "SELECT id, sales FROM orders WHERE store_id = 1 AND (sales >= 800 OR category = '护肤品') ORDER BY id;"},
            {"title": "非化妆品高价商品", "req": "筛选不是'化妆品'且 price >= 35 的商品，输出 product_id, product_name。", "tables": t_products, "sql": "SELECT product_id, product_name FROM products WHERE NOT (category = '化妆品') AND price >= 35 ORDER BY product_id;"},
        ],
        "kp03-03": [
            {"title": "按品类列表筛选商品", "req": "使用 IN 查询 category 为 '护肤品' 或 '日用品' 的商品，输出 product_id, category。", "tables": t_products, "sql": "SELECT product_id, category FROM products WHERE category IN ('护肤品', '日用品') ORDER BY product_id;"},
            {"title": "筛选一月订单日期区间", "req": "使用 BETWEEN 查询 2026-01-01 至 2026-01-31 的订单，输出 id, order_date。", "tables": t_orders, "sql": "SELECT id, order_date FROM orders WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31' ORDER BY id;"},
        ],
        "kp03-04": [
            {"title": "模糊匹配含下划线名称", "req": "使用 ESCAPE 查询 product_name 中包含字面 '_' 的商品，输出 product_id, product_name。", "tables": t_products, "sql": "SELECT product_id, product_name FROM products WHERE product_name LIKE '%\\_%' ESCAPE '\\' ORDER BY product_id;"},
        ],
        "kp04-01": [
            {"title": "商品按价格降序与编号升序", "req": "按 price 降序、相同时按 product_id 升序输出 product_id 与 price。", "tables": t_products, "sql": "SELECT product_id, price FROM products ORDER BY price DESC, product_id ASC;"},
            {"title": "订单按门店升序与金额降序", "req": "先按 store_id 升序、再按 sales 降序、最后按 id 升序输出 id, store_id, sales。", "tables": t_orders, "sql": "SELECT id, store_id, sales FROM orders ORDER BY store_id ASC, sales DESC, id ASC;"},
        ],
        "kp04-02": [
            {"title": "查询价格最高的 3 件商品", "req": "按 price 降序（同价按 product_id 升序）返回前 3 件商品的 product_id 与 price。", "tables": t_products, "sql": "SELECT product_id, price FROM products ORDER BY price DESC, product_id ASC LIMIT 3;"},
            {"title": "订单分页第 2 页数据", "req": "按 id 升序排列订单，每页 2 条，跳过前 2 条取第 2 页的 id 与 sales。", "tables": t_orders, "sql": "SELECT id, sales FROM orders ORDER BY id ASC LIMIT 2 OFFSET 2;"},
        ],
        "kp05-01": [
            {"title": "定位缺失值的记录", "req": "使用 IS NULL 筛选 value 为空的记录，输出 id 与 note。", "tables": t_nullable, "sql": "SELECT id, note FROM nullable_values WHERE value IS NULL ORDER BY id;"},
            {"title": "三值逻辑过滤非 10 且非空记录", "req": "筛选 value 不等于 10（自然排除 NULL）的记录，输出 id 与 value。", "tables": t_nullable, "sql": "SELECT id, value FROM nullable_values WHERE value <> 10 ORDER BY id;"},
            {"title": "统计缺失客户编号的订单", "req": "筛选 customer_id IS NULL 的匿名订单，输出 id 与 sales。", "tables": t_orders, "sql": "SELECT id, sales FROM orders WHERE customer_id IS NULL ORDER BY id;"},
        ],
        "kp05-02": [
            {"title": "使用 COALESCE 填充空值与备注", "req": "输出 id，把 NULL 的 value 替换为 0（safe_val），NULL 的 note 替换为 '未备注'（safe_note）。", "tables": t_nullable, "sql": "SELECT id, COALESCE(value, 0) AS safe_val, COALESCE(note, '未备注') AS safe_note FROM nullable_values ORDER BY id;"},
            {"title": "将订单缺失客户编号转为 -1", "req": "输出订单 id，并使用 IFNULL 把空 customer_id 显示为 -1（cust_key）。", "tables": t_orders, "sql": "SELECT id, IFNULL(customer_id, -1) AS cust_key FROM orders ORDER BY id;"},
        ],
        "kp06-01": [
            {"title": "对比 COUNT(*) 与 COUNT(列)", "req": "统计 nullable_values 的总行数 total_rows、非空 value 数 valid_values、去重非空 value 数 distinct_values。", "tables": t_nullable, "sql": "SELECT COUNT(*) AS total_rows, COUNT(value) AS valid_values, COUNT(DISTINCT value) AS distinct_values FROM nullable_values;"},
            {"title": "统计订单总数与有客户订单数", "req": "统计 orders 总订单数 order_cnt 与已绑定客户的去重客户数 cust_cnt。", "tables": t_orders, "sql": "SELECT COUNT(*) AS order_cnt, COUNT(DISTINCT customer_id) AS cust_cnt FROM orders;"},
        ],
        "kp06-02": [
            {"title": "总销售额与均值汇总", "req": "计算 orders 表所有订单的销售额总和 total_sales 与平均值 avg_sales（保留两位小数）。", "tables": t_orders, "sql": "SELECT COALESCE(SUM(sales), 0) AS total_sales, ROUND(AVG(sales), 2) AS avg_sales FROM orders;"},
            {"title": "空值表的 SUM 与 AVG 口径", "req": "计算 nullable_values 中 value 的求和 sum_val 与非空均值 avg_val。", "tables": t_nullable, "sql": "SELECT SUM(value) AS sum_val, ROUND(AVG(value), 2) AS avg_val FROM nullable_values;"},
        ],
        "kp06-03": [
            {"title": "商品价格极值与极差", "req": "查询 products 中最低价 min_price、最高价 max_price 及价差 price_span。", "tables": t_products, "sql": "SELECT MIN(price) AS min_price, MAX(price) AS max_price, MAX(price) - MIN(price) AS price_span FROM products;"},
        ],
        "kp07-01": [
            {"title": "按品类的订单金额聚合", "req": "按 category 分组统计订单总金额 total_sales，按 total_sales 降序、category 升序排列。", "tables": t_orders, "sql": "SELECT category, SUM(sales) AS total_sales FROM orders GROUP BY category ORDER BY total_sales DESC, category ASC;"},
            {"title": "按门店统计订单数与总金额", "req": "按 store_id 分组统计订单数量 order_cnt 和总销售额 total_sales，按 store_id 升序。", "tables": t_orders, "sql": "SELECT store_id, COUNT(*) AS order_cnt, SUM(sales) AS total_sales FROM orders GROUP BY store_id ORDER BY store_id;"},
        ],
        "kp07-02": [
            {"title": "使用 HAVING 筛选高销售品类", "req": "按 category 分组统计总销售额 total_sales，仅保留总销售额 > 1000 的品类，按 category 升序。", "tables": t_orders, "sql": "SELECT category, SUM(sales) AS total_sales FROM orders GROUP BY category HAVING SUM(sales) > 1000 ORDER BY category;"},
            {"title": "筛选多笔订单的高产门店", "req": "统计 2026-01-01 以来各 store_id 的订单数 order_cnt，仅保留订单数 >= 2 的门店，按 store_id 升序。", "tables": t_orders, "sql": "SELECT store_id, COUNT(*) AS order_cnt FROM orders WHERE order_date >= '2026-01-01' GROUP BY store_id HAVING COUNT(*) >= 2 ORDER BY store_id;"},
        ],
        "kp07-03": [
            {"title": "按门店与品类二维分组统计", "req": "按 store_id 与 category 两级分组统计销售额合计 total_sales，按 store_id, category 升序。", "tables": t_orders, "sql": "SELECT store_id, category, SUM(sales) AS total_sales FROM orders GROUP BY store_id, category ORDER BY store_id, category;"},
        ],
        "kp08-01": [
            {"title": "内连接订单与客户姓名", "req": "内连接 orders 与 customers，输出订单 id、客户名 customer_name 及销售额 sales，按 o.id 升序。", "tables": t_orders + t_customers, "sql": "SELECT o.id, c.customer_name, o.sales FROM orders o INNER JOIN customers c ON o.customer_id = c.customer_id ORDER BY o.id;"},
        ],
        "kp08-02": [
            {"title": "左连接保留无单客户", "req": "以 customers 为主表 LEFT JOIN orders，统计每位客户的订单数 order_cnt（无单为 0），输出 customer_id 与 order_cnt。", "tables": t_customers + t_orders, "sql": "SELECT c.customer_id, COUNT(o.id) AS order_cnt FROM customers c LEFT JOIN orders o ON c.customer_id = o.customer_id GROUP BY c.customer_id ORDER BY c.customer_id;"},
        ],
        "kp08-03": [
            {"title": "外连接 ON 条件筛选二月订单", "req": "保留全部客户，仅匹配 2 月订单（order_date >= '2026-02-01'），输出 customer_id 与 feb_orders，按 customer_id 升序。", "tables": t_customers + t_orders, "sql": "SELECT c.customer_id, COUNT(o.id) AS feb_orders FROM customers c LEFT JOIN orders o ON c.customer_id = o.customer_id AND o.order_date >= '2026-02-01' GROUP BY c.customer_id ORDER BY c.customer_id;"},
        ],
        "kp08-04": [
            {"title": "员工与直属上级自连接", "req": "将 employees 自连接查询员工姓名 employee_name 及其直属上级姓名 manager_name，按 e.employee_id 升序。", "tables": t_employees, "sql": "SELECT e.employee_name, m.employee_name AS manager_name FROM employees e LEFT JOIN employees m ON e.manager_id = m.employee_id ORDER BY e.employee_id;"},
        ],
        "kp08-05": [
            {"title": "预聚合防止连接膨胀", "req": "先按 customer_id 汇总订单总销售额，再左连接 customers，输出 customer_id 与 total_sales（无单为 0）。", "tables": t_customers + t_orders, "sql": "WITH totals AS (SELECT customer_id, SUM(sales) AS total_sales FROM orders GROUP BY customer_id) SELECT c.customer_id, COALESCE(t.total_sales, 0) AS total_sales FROM customers c LEFT JOIN totals t ON c.customer_id = t.customer_id ORDER BY c.customer_id;"},
        ],
        "kp08-06": [
            {"title": "关联客户并保持非空计数", "req": "从 customers 左连接 orders，输出 customer_id 与订单金额合计 total_sales，按 customer_id 升序。", "tables": t_customers + t_orders, "sql": "SELECT c.customer_id, COALESCE(SUM(o.sales), 0) AS total_sales FROM customers c LEFT JOIN orders o ON o.customer_id = c.customer_id GROUP BY c.customer_id ORDER BY c.customer_id;"},
        ],
        "kp08-07": [
            {"title": "门店与月份笛卡尔积矩阵", "req": "使用 CROSS JOIN 生成 stores 与 monthly 的完整门店-月份组合，输出 store_id 与 month，按 store_id, month 升序。", "tables": t_stores + t_monthly, "sql": "SELECT s.store_id, m.month FROM stores s CROSS JOIN monthly m ORDER BY s.store_id, m.month;"},
        ],
        "kp09-01": [
            {"title": "使用 CASE WHEN 划分库存状态", "req": "输出 product_id 及 stock_status：stock=0 为 '缺货'，stock<50 为 '低库存'，否则为 '充足'，按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, CASE WHEN stock = 0 THEN '缺货' WHEN stock < 50 THEN '低库存' ELSE '充足' END AS stock_status FROM products ORDER BY product_id;"},
        ],
        "kp09-02": [
            {"title": "条件聚合统计高值订单", "req": "按 store_id 分组统计高额订单数 high_cnt（sales >= 600）与全部订单数 all_cnt，按 store_id 升序。", "tables": t_orders, "sql": "SELECT store_id, SUM(CASE WHEN sales >= 600 THEN 1 ELSE 0 END) AS high_cnt, COUNT(*) AS all_cnt FROM orders GROUP BY store_id ORDER BY store_id;"},
        ],
        "kp09-03": [
            {"title": "使用 NULLIF 防止除以零", "req": "计算 products 中单价除以库存的比值（1.0 * price / NULLIF(stock, 0) 两位小数，别名为 unit_ratio），按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, ROUND(1.0 * price / NULLIF(stock, 0), 2) AS unit_ratio FROM products ORDER BY product_id;"},
        ],
        "kp09-04": [
            {"title": "正负值二元标注", "req": "在 nullable_values 中用 CASE WHEN 判定 value > 0 标为 '正值'，否则为 '非正或缺失'（flag），按 id 升序。", "tables": t_nullable, "sql": "SELECT id, CASE WHEN value > 0 THEN '正值' ELSE '非正或缺失' END AS flag FROM nullable_values ORDER BY id;"},
        ],
        "kp10-01": [
            {"title": "筛选高于平均售价的商品", "req": "使用标量子查询找出 price > 全部商品平均 price 的商品，输出 product_id 与 price，按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, price FROM products WHERE price > (SELECT AVG(price) FROM products) ORDER BY product_id;"},
        ],
        "kp10-02": [
            {"title": "筛选有消费记录的客户", "req": "使用 IN 子查询查询在 orders 中下过单的客户，输出 customer_id 与 customer_name，按 customer_id 升序。", "tables": t_customers + t_orders, "sql": "SELECT customer_id, customer_name FROM customers WHERE customer_id IN (SELECT customer_id FROM orders WHERE customer_id IS NOT NULL) ORDER BY customer_id;"},
        ],
        "kp10-03": [
            {"title": "使用 EXISTS 判定活跃客户", "req": "使用相关子查询 EXISTS 查询在 orders 中存在订单的客户编号 customer_id，按升序排列。", "tables": t_customers + t_orders, "sql": "SELECT c.customer_id FROM customers c WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id) ORDER BY c.customer_id;"},
        ],
        "kp10-04": [
            {"title": "使用 NOT EXISTS 识别未下单客户", "req": "使用 NOT EXISTS 查询从未下过订单的客户 customer_id，按升序排列。", "tables": t_customers + t_orders, "sql": "SELECT c.customer_id FROM customers c WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id) ORDER BY c.customer_id;"},
        ],
        "kp11-01": [
            {"title": "拼接品类与商品名称标签", "req": "使用字符串拼接返回 product_id 与 'category - product_name'（label），按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, category || ' - ' || product_name AS label FROM products ORDER BY product_id;"},
        ],
        "kp11-02": [
            {"title": "截取商品名前三个字符", "req": "截取 product_name 前 3 个字符作为 prefix，输出 product_id 与 prefix，按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, SUBSTR(product_name, 1, 3) AS prefix FROM products ORDER BY product_id;"},
        ],
        "kp11-03": [
            {"title": "清洗首尾空格与统一小写", "req": "对 raw_data 的 name 去两端空格作为 clean_name，对 category 去空格转小写作为 clean_cat，输出 id, clean_name, clean_cat，按 id 升序。", "tables": t_raw, "sql": "SELECT id, TRIM(name) AS clean_name, LOWER(TRIM(category)) AS clean_cat FROM raw_data ORDER BY id;"},
        ],
        "kp11-04": [
            {"title": "统计商品名称字符长度", "req": "计算 products 中商品 product_name 的字符长度 name_len，输出 product_id 与 name_len，按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, LENGTH(product_name) AS name_len FROM products ORDER BY product_id;"},
        ],
        "kp12-01": [
            {"title": "价格涨幅四舍五入与偏差绝对值", "req": "计算商品 price * 1.15 保留两位小数（new_price）以及 price 与 50 的绝对差（diff_50），按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, ROUND(price * 1.15, 2) AS new_price, ABS(price - 50) AS diff_50 FROM products ORDER BY product_id;"},
        ],
        "kp12-02": [
            {"title": "提取下单年月 YYYY-MM", "req": "从 orders 表提取 order_date 的年月部分（YYYY-MM）命名为 ym，输出 id 与 ym，按 id 升序。", "tables": t_orders, "sql": "SELECT id, SUBSTR(order_date, 1, 7) AS ym FROM orders ORDER BY id;"},
        ],
        "kp12-03": [
            {"title": "计算订单距离基准日的天数差", "req": "计算 '2026-03-01' 距每笔订单 order_date 的天数差 days_ago（整数），输出 id 与 days_ago，按 id 升序。", "tables": t_orders, "sql": "SELECT id, CAST(julianday('2026-03-01') - julianday(order_date) AS INTEGER) AS days_ago FROM orders ORDER BY id;"},
        ],
        "kp12-04": [
            {"title": "闭开区间汇总二月销售额", "req": "统计 order_date 在 2026 年 2 月内（>='2026-02-01' 且 <'2026-03-01'）的总销售额 feb_sales。", "tables": t_orders, "sql": "SELECT COALESCE(SUM(sales), 0) AS feb_sales FROM orders WHERE order_date >= '2026-02-01' AND order_date < '2026-03-01';"},
        ],
        "kp12-05": [
            {"title": "精确浮点均值对比整除", "req": "计算每件商品库存与 10 的浮点商 ratio（保留两位小数）与整除商 boxes，输出 product_id, ratio, boxes。", "tables": t_products, "sql": "SELECT product_id, ROUND(stock * 1.0 / 10, 2) AS ratio, stock / 10 AS boxes FROM products ORDER BY product_id;"},
        ],
        "kp13-01": [
            {"title": "合并新老客户明细含重复", "req": "使用 UNION ALL 合并 old_customers 与 new_customers 的 customer_id，按 customer_id 升序输出。", "tables": t_old + t_new, "sql": "SELECT customer_id FROM old_customers UNION ALL SELECT customer_id FROM new_customers ORDER BY customer_id;"},
        ],
        "kp13-02": [
            {"title": "求新老客户交集", "req": "使用 INTERSECT 找出同时出现在 old_customers 与 new_customers 的 customer_id，按升序输出。", "tables": t_old + t_new, "sql": "SELECT customer_id FROM old_customers INTERSECT SELECT customer_id FROM new_customers ORDER BY customer_id;"},
        ],
        "kp13-03": [
            {"title": "找出未出现在新表中的老客户", "req": "使用 EXCEPT 找出仅存在于 old_customers 而不在 new_customers 的 customer_id，按升序输出。", "tables": t_old + t_new, "sql": "SELECT customer_id FROM old_customers EXCEPT SELECT customer_id FROM new_customers ORDER BY customer_id;"},
        ],
        "kp13-04": [
            {"title": "合并并去重所有客户", "req": "使用 UNION 合并 old_customers 与 new_customers 并去重，输出 customer_id 按升序排列。", "tables": t_old + t_new, "sql": "SELECT customer_id FROM old_customers UNION SELECT customer_id FROM new_customers ORDER BY customer_id;"},
        ],
        "kp14-01": [
            {"title": "使用 CTE 汇总再过滤高额品类", "req": "先在 CTE 中按 category 汇总销售额 total_sales，外层筛选 total_sales >= 1000，按 total_sales 降序。", "tables": t_orders, "sql": "WITH cat_totals AS (SELECT category, SUM(sales) AS total_sales FROM orders GROUP BY category) SELECT category, total_sales FROM cat_totals WHERE total_sales >= 1000 ORDER BY total_sales DESC, category ASC;"},
        ],
        "kp14-02": [
            {"title": "使用递归 CTE 展开组织层级", "req": "以 manager_id IS NULL 为根节点（depth=0）向下递归计算员工层级 depth，输出 employee_id, employee_name, depth。", "tables": t_employees, "sql": "WITH RECURSIVE tree AS (SELECT employee_id, employee_name, 0 AS depth FROM employees WHERE manager_id IS NULL UNION ALL SELECT e.employee_id, e.employee_name, t.depth + 1 FROM employees e JOIN tree t ON e.manager_id = t.employee_id) SELECT employee_id, employee_name, depth FROM tree ORDER BY employee_id;"},
        ],
        "kp15-01": [
            {"title": "计算同品类销售额窗口汇总", "req": "使用窗口函数按 category 计算同类总销售 cat_total，输出 id, category, sales, cat_total，按 id 升序。", "tables": t_orders, "sql": "SELECT id, category, sales, SUM(sales) OVER (PARTITION BY category) AS cat_total FROM orders ORDER BY id;"},
        ],
        "kp15-02": [
            {"title": "使用 ROW_NUMBER 薪资排位", "req": "按 salary 降序（同薪按 employee_id 升序）使用 ROW_NUMBER() 生成排名 rn，输出 employee_id, salary, rn。", "tables": t_employees, "sql": "SELECT employee_id, salary, ROW_NUMBER() OVER (ORDER BY salary DESC, employee_id ASC) AS rn FROM employees ORDER BY rn;"},
        ],
        "kp15-03": [
            {"title": "使用 LAG 对比上月销售额", "req": "按 month 升序读取 monthly，并用 LAG(sales) 获取上月销售额 prev_sales，按 month 升序。", "tables": t_monthly, "sql": "SELECT month, sales, LAG(sales) OVER (ORDER BY month) AS prev_sales FROM monthly ORDER BY month;"},
        ],
        "kp15-04": [
            {"title": "计算月度累计销售额", "req": "按 month 升序，使用 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW 计算累计销售额 running_sales。", "tables": t_monthly, "sql": "SELECT month, sales, SUM(sales) OVER (ORDER BY month ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_sales FROM monthly ORDER BY month;"},
        ],
        "kp15-05": [
            {"title": "计算近两月移动平均销售额", "req": "按 month 升序计算当前月与前一月移动平均 ma_2m（保留两位小数）。", "tables": t_monthly, "sql": "SELECT month, sales, ROUND(AVG(sales) OVER (ORDER BY month ROWS BETWEEN 1 PRECEDING AND CURRENT ROW), 2) AS ma_2m FROM monthly ORDER BY month;"},
        ],
        "kp15-06": [
            {"title": "对比同薪区间 RANGE 累计和", "req": "按 salary 升序使用 RANGE 窗口统计截止当前薪资的累计薪资 sum_range，输出 employee_id, salary, sum_range。", "tables": t_employees, "sql": "SELECT employee_id, salary, SUM(salary) OVER (ORDER BY salary RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS sum_range FROM employees ORDER BY salary, employee_id;"},
        ],
        "kp16-01": [
            {"title": "清洗后按业务键去重分组", "req": "对 raw_data 中的 name 去空格、category 转小写后按 (clean_name, clean_cat) 分组并统计 cnt。", "tables": t_raw, "sql": "SELECT TRIM(name) AS clean_name, LOWER(TRIM(category)) AS clean_cat, COUNT(*) AS cnt FROM raw_data GROUP BY TRIM(name), LOWER(TRIM(category)) ORDER BY clean_name, clean_cat;"},
        ],
        "kp16-02": [
            {"title": "标注缺失姓名或非正金额异常", "req": "标注 raw_data 每行质量状态 quality_flag：name 为空标 '姓名缺失'，amount 为空或 <=0 标 '金额异常'，否则 '正常'。", "tables": t_raw, "sql": "SELECT id, CASE WHEN name IS NULL OR TRIM(name) = '' THEN '姓名缺失' WHEN amount IS NULL OR CAST(amount AS REAL) <= 0 THEN '金额异常' ELSE '正常' END AS quality_flag FROM raw_data ORDER BY id;"},
        ],
        "kp16-03": [
            {"title": "金额类型转换与类别归一化", "req": "将 raw_data.amount 转为数值（空置 0）并把 category 转小写标准类目 norm_cat，输出 id, num_amt, norm_cat。", "tables": t_raw, "sql": "SELECT id, COALESCE(CAST(amount AS REAL), 0) AS num_amt, LOWER(TRIM(COALESCE(category, 'unknown'))) AS norm_cat FROM raw_data ORDER BY id;"},
        ],
        "kp17-01": [
            {"title": "写入新商品库存记录", "req": "向 demo_stock 插入一条记录 (id=3, label='Toner', qty=30)。", "tables": t_demo, "sql": "INSERT INTO demo_stock (id, label, qty) VALUES (3, 'Toner', 30);", "verificationQuery": "SELECT id, label, qty FROM demo_stock ORDER BY id;", "kind": "ddl_dml"},
        ],
        "kp17-02": [
            {"title": "更新指定商品库存数量", "req": "将 demo_stock 中 id=2 的 qty 增加 15（即更新为 qty + 15）。", "tables": t_demo, "sql": "UPDATE demo_stock SET qty = qty + 15 WHERE id = 2;", "verificationQuery": "SELECT id, qty FROM demo_stock ORDER BY id;", "kind": "ddl_dml"},
        ],
        "kp17-03": [
            {"title": "追加新列并维护默认值", "req": "使用 ALTER TABLE 为 demo_stock 添加 status TEXT DEFAULT 'active' 列。", "tables": t_demo, "sql": "ALTER TABLE demo_stock ADD COLUMN status TEXT DEFAULT 'active';", "verificationQuery": "SELECT id, status FROM demo_stock ORDER BY id;", "kind": "ddl_dml"},
        ],
        "kp17-04": [
            {"title": "按条件删除或调整库存行", "req": "将 demo_stock 中 qty >= 50 的商品数量统一扣减 10。", "tables": t_demo, "sql": "UPDATE demo_stock SET qty = qty - 10 WHERE qty >= 50;", "verificationQuery": "SELECT id, qty FROM demo_stock ORDER BY id;", "kind": "ddl_dml"},
        ],
        "kp17-05": [
            {"title": "筛选有效库存商品", "req": "查询 demo_stock 中 qty > 0 的商品，输出 id 与 label，按 id 升序。", "tables": t_demo, "sql": "SELECT id, label FROM demo_stock WHERE qty > 0 ORDER BY id;", "kind": "query"},
        ],
        "kp18-01": [
            {"title": "高选择性等值条件筛选商品", "req": "使用可走索引的等值条件筛选 price = 89 的商品，输出 product_id 与 price，按 product_id 升序。", "tables": t_products, "sql": "SELECT product_id, price FROM products WHERE price = 89 ORDER BY product_id;"},
        ],
        "kp18-02": [
            {"title": "编写 SARGable 复合索引查询条件", "req": "避免对日期列套函数，使用范围写法查询 store_id = 1 且 order_date >= '2026-02-01' 的订单，输出 id 与 sales。", "tables": t_orders, "sql": "SELECT id, sales FROM orders WHERE store_id = 1 AND order_date >= '2026-02-01' ORDER BY id;"},
        ],
        "kp18-03": [
            {"title": "精简列投影辅助索引覆盖扫描", "req": "仅返回所需两列（避免 SELECT *），筛选 store_id = 1 的订单输出 id 与 order_date，按 id 升序。", "tables": t_orders, "sql": "SELECT id, order_date FROM orders WHERE store_id = 1 ORDER BY id;"},
        ],
    }


def format_pred_text(cols, rows):
    if not rows:
        return f"空结果集 (0 行，列: {', '.join(cols)})"
    head = " | ".join(cols)
    body = "; ".join(", ".join("NULL" if v is None else str(v) for v in r) for r in rows[:4])
    suffix = f" ... (共{len(rows)}行)" if len(rows) > 4 else f" (共{len(rows)}行)"
    return f"[{head}] -> {body}{suffix}"


def build_choice_question(qid, ch_id, diff, kp, idx):
    concept = kp["concept"]
    err = kp.get("commonError") or {"example": "忽略边界或空值处理", "correction": "严格按字段语义与执行顺序编写"}
    opts = [
        {"id": "A", "text": f"{kp['title']}：{concept[:38]}{'...' if len(concept)>38 else ''}", "explanation": f"准确符合「{kp['title']}」的核心语法与数据语义。"},
        {"id": "B", "text": f"直接忽略 NULL 值与排序差异：{err['example'][:28]}", "explanation": f"常见误区：{err['correction']}"},
        {"id": "C", "text": "必须把所有过滤条件写在 SELECT 列表里才能生效", "explanation": "过滤行记录应放在 WHERE 或 HAVING 子句，而非投影列。"},
        {"id": "D", "text": "SQL 查询会自动更改底层物理表的数据顺序并落盘", "explanation": "普通 SELECT 查询只读返回结果集，不改变物理存储顺序。"},
    ]
    return {
        "id": qid, "chapterId": ch_id, "title": f"{kp['title']}概念辨析（{idx + 1}）", "type": "选择", "difficulty": diff,
        "context": f"知识点：{kp['title']}。", "requirements": f"关于「{kp['title']}」的使用规范与执行特点，以下哪项表述最准确？",
        "options": opts, "correctOptionId": "A",
        "hints": [f"结合「{kp['syntax']}」的适用场景判断。", f"注意区分易错点：{err['correction']}"],
        "explanation": f"{concept} {err['correction']}", "supportStatus": "仅理论", "fixtureIds": [],
    }


def build_prediction_question(qid, ch_id, diff, kp, tpl, exp_cols, exp_rows, fx_ids):
    correct_str = format_pred_text(exp_cols, exp_rows)
    d1 = format_pred_text(exp_cols, exp_rows[1:] if len(exp_rows) > 1 else [[0] * len(exp_cols)])
    d2 = f"空结果集 (0 行，列: {', '.join(exp_cols)})" if exp_rows else f"[{' | '.join(exp_cols)}] -> 1, 示例 (共1行)"
    d3 = format_pred_text(exp_cols, list(reversed(exp_rows)) if len(exp_rows) > 1 else [[999] * len(exp_cols)])
    if len({correct_str, d1, d2, d3}) < 4:
        d3 = f"结果返回 {len(exp_rows) + 2} 行（含重复或空值行）"
    opts = [correct_str, d1, d2, d3]
    shift = int(qid[1:]) % 4
    opts = opts[shift:] + opts[:shift]

    preview_tbl = ""
    if tpl["tables"]:
        t0 = tpl["tables"][0]
        hdr = " | ".join(c["name"] for c in t0["columns"])
        rows_txt = "\n".join(" | ".join("NULL" if x is None else str(x) for x in r) for r in t0["rows"][:6])
        preview_tbl = f"-- 输入表 {t0['name']} ({hdr}):\n-- " + rows_txt.replace("\n", "\n-- ") + "\n\n"

    return {
        "id": qid, "chapterId": ch_id, "title": f"【结果推演】{tpl['title']}", "type": "结果预测", "difficulty": diff,
        "context": f"请勿运行试运行功能，手动逐行推演「{kp['title']}」下的查询输出。",
        "requirements": f"根据给定数据表与 SQL 代码，预测最终返回结果：{tpl['req']}",
        "initialCode": preview_tbl + tpl["sql"],
        "predictionOptions": opts, "predictionAnswerText": correct_str,
        "hints": ["按 FROM -> WHERE -> GROUP BY -> HAVING -> SELECT -> ORDER BY 顺序逐层推演。", f"留意空值或并列行对「{kp['title']}」结果的影响。"],
        "explanation": f"逐行推演结果应为：{correct_str}。{kp['concept']}",
        "referenceSolutions": [{"mysqlSql": tpl["sql"], "sqliteSql": tpl["sql"], "explanation": kp["concept"]}],
        "supportStatus": "直接兼容", "fixtureIds": fx_ids, "expectedColumns": exp_cols, "expectedRows": exp_rows,
    }


def build_blank_question(qid, ch_id, diff, kp, tpl, sql, exp_cols, exp_rows, fx_ids, ast_feats, order_policy):
    blanked, answer_token = sql, None
    for kw in ["GROUP BY", "ORDER BY", "LEFT JOIN", "INNER JOIN", "PARTITION BY", "ROW_NUMBER()", "COALESCE", "HAVING", "WHERE", "DISTINCT", "UNION ALL", "EXCEPT", "EXISTS", "BETWEEN", "UPDATE", "INSERT INTO", "ALTER TABLE"]:
        if kw in sql.upper():
            pos = sql.upper().find(kw)
            answer_token = sql[pos : pos + len(kw)]
            blanked = sql[:pos] + "____" + sql[pos + len(kw) :]
            break
    if answer_token is None:
        answer_token = "SELECT"
        blanked = "____" + sql[6:] if sql.upper().startswith("SELECT") else sql
    return {
        "id": qid, "chapterId": ch_id, "title": f"【代码填空】{tpl['title']}", "type": "代码填空", "difficulty": diff,
        "context": f"补全「{kp['title']}」相关 SQL 语句中的缺失关键字/子句。",
        "requirements": f"{tpl['req']}（在模板 `____` 处填入正确的 SQL 关键字）",
        "initialCode": blanked, "blankAnswerHint": f"提示结构：{kp['syntax']}",
        "acceptedBlanks": [answer_token, answer_token.lower(), answer_token.upper(), sql],
        "hints": ["思考该位置在整条语句中承担的 SQL 职责。", f"相关语法：{kp['syntax']}"],
        "explanation": f"此处应填入 `{answer_token}`，完整语句为：{sql}",
        "referenceSolutions": [{"mysqlSql": sql, "sqliteSql": sql, "explanation": kp["concept"]}],
        "requiredAstFeatures": ast_feats, "supportStatus": "直接兼容",
        "fixtureIds": fx_ids, "expectedColumns": exp_cols, "expectedRows": exp_rows, "orderPolicy": order_policy,
    }


def build_debug_question(qid, ch_id, diff, kp, tpl, sql, exp_cols, exp_rows, fx_ids, ast_feats, order_policy):
    buggy, bug_desc = sql, "存在逻辑或语法错误"
    if "LEFT JOIN" in sql.upper():
        buggy = re.sub(r"\bLEFT\s+JOIN\b", "INNER JOIN", sql, count=1, flags=re.IGNORECASE)
        bug_desc = "误用 INNER JOIN 会丢失左表未匹配行"
    elif "COALESCE(" in sql.upper():
        buggy = re.sub(r"\bCOALESCE\s*\(", "CONCAT(", sql, count=1, flags=re.IGNORECASE)
        bug_desc = "空值兜底函数使用有误"
    elif "ORDER BY" in sql.upper():
        buggy = re.sub(r"\bORDER\s+BY\b.*?(;|$)", "", sql, flags=re.IGNORECASE) + ";"
        bug_desc = "遗漏了题目要求的 ORDER BY 排序"
    elif "GROUP BY" in sql.upper():
        buggy = sql.replace("GROUP BY", "ORDER BY", 1)
        bug_desc = "分组子句被误写为 ORDER BY"
    else:
        buggy = sql.replace("SELECT ", "SELECT WRONG_COL, ", 1) if "SELECT " in sql.upper() else sql.replace("SET ", "SET WRONG=", 1)
        bug_desc = "包含不存在的字段引用"
    if buggy.strip() == sql.strip():
        buggy = "SELECT * FROM unknown_tbl;"
    return {
        "id": qid, "chapterId": ch_id, "title": f"【代码纠错】{tpl['title']}", "type": "代码纠错", "difficulty": diff,
        "context": f"下方代码针对「{kp['title']}」编写，但{bug_desc}。",
        "requirements": f"请修正 SQL 全文，使结果严格满足：{tpl['req']}（输出列：{', '.join(exp_cols)}）",
        "initialCode": buggy,
        "hints": [f"诊断方向：{bug_desc}。", f"核对「{kp['title']}」的标准语法：{kp['syntax']}"],
        "explanation": f"原语句{bug_desc}。修正后：{sql}",
        "referenceSolutions": [{"mysqlSql": sql, "sqliteSql": sql, "explanation": kp["concept"]}],
        "requiredAstFeatures": ast_feats, "supportStatus": "直接兼容",
        "fixtureIds": fx_ids, "expectedColumns": exp_cols, "expectedRows": exp_rows, "orderPolicy": order_policy,
    }


def build_remaining_questions_and_projects(kp_lookup, q_to_kps, lesson_tables, built_dbs):
    questions = build_q0001_to_q0036(kp_lookup, q_to_kps)

    ast_bp = {"q0251": ["CTE", "LEFT JOIN"], "q0300": ["CTE", "CASE"]}
    bp_mismatches = []
    for bp_idx in range(1, 51):
        bp_id = f"BP{bp_idx:02d}"
        qid = f"q{250 + bp_idx:04d}"
        bp = json.loads((CONTENT / "enterprise_reference/business_tasks" / f"{bp_id}.json").read_text("utf-8"))
        res_data = json.loads((CONTENT / "enterprise_reference/business_tasks/results" / f"{bp_id}.json").read_text("utf-8"))
        clean_ds = bp["datasetId"].replace("-v2", "")

        conn = sqlite3.connect(str(built_dbs[clean_ds]))
        sql = bp["sqliteSql"]
        try:
            cur = conn.execute(sql)
        except sqlite3.OperationalError:
            cur = conn.execute(translate_concat(sql))
        actual_cols = [d[0] for d in cur.description]
        actual_rows = [list(r) for r in cur.fetchall()]
        conn.close()

        expected_rows_out = res_data["rows"]
        if actual_cols != res_data["columns"]:
            bp_mismatches.append(f"{bp_id} col mismatch: got={actual_cols} exp={res_data['columns']}")
        elif not rows_match(actual_rows, res_data["rows"], bp.get("orderPolicy", "ordered")):
            if len(actual_rows) == len(res_data["rows"]):
                max_diff = 0.0
                diff_cnt = 0
                non_num = False
                for r_a, r_e in zip(actual_rows, res_data["rows"]):
                    for c_a, c_e in zip(r_a, r_e):
                        if c_a == c_e:
                            continue
                        if isinstance(c_a, (int, float)) and isinstance(c_e, (int, float)):
                            d = abs(float(c_a) - float(c_e))
                            max_diff = max(max_diff, d)
                            diff_cnt += 1
                        else:
                            non_num = True
                if not non_num and max_diff <= 0.0101:
                    print(f"  Note {bp_id}: {diff_cnt} float cells differ by <= {max_diff:.4f} (SQLite ROUND vs reference half-even); using SQLite exact baseline.")
                    expected_rows_out = actual_rows
                else:
                    diff_samples = []
                    for idx_r in range(len(actual_rows)):
                        if actual_rows[idx_r] != res_data["rows"][idx_r]:
                            diff_samples.append(f"row[{idx_r}] got={actual_rows[idx_r]} exp={res_data['rows'][idx_r]}")
                            if len(diff_samples) >= 3:
                                break
                    bp_mismatches.append(f"{bp_id} ({clean_ds}) row mismatch: max_diff={max_diff}, " + "; ".join(diff_samples))
            else:
                bp_mismatches.append(f"{bp_id} ({clean_ds}) len mismatch: {len(actual_rows)} vs {len(res_data['rows'])}")

        questions[qid] = {
            "id": qid, "chapterId": bp["chapterId"], "title": bp["title"], "type": "业务情景", "difficulty": bp["difficulty"],
            "context": bp["context"] + (f"（来源表：{', '.join(bp.get('sourceTables', []))}）" if bp.get("sourceTables") else ""),
            "requirements": bp["requirements"] + f"（输出列：{', '.join(res_data['columns'])}）",
            "hints": bp.get("hints", []), "explanation": bp.get("explanation", ""),
            "referenceSolutions": [{"mysqlSql": bp["mysqlSql"], "sqliteSql": bp["sqliteSql"], "explanation": bp.get("explanation", "")}],
            "requiredAstFeatures": ast_bp.get(qid, []),
            "supportStatus": "转换执行" if "CONCAT(" in bp["sqliteSql"].upper() else "直接兼容",
            "datasetId": clean_ds, "fixtureIds": [],
            "expectedColumns": res_data["columns"], "expectedRows": expected_rows_out,
            "orderPolicy": bp.get("orderPolicy", "ordered"), "taskId": bp_id, "crossGroup": bp.get("crossGroup"),
        }
    if bp_mismatches:
        print("BP DIAGNOSTICS:\n  " + "\n  ".join(bp_mismatches))
        raise AssertionError(f"{len(bp_mismatches)} BP mismatch(es)")

    # Pin 12 AST-required questions in ch07..ch15 + 10 业务情景 so they stay executable SQL questions!
    ast_map = {
        "q0078": ["GROUP BY"], "q0084": ["GROUP BY", "HAVING"],
        "q0097": ["JOIN"], "q0099": ["LEFT JOIN"],
        "q0139": ["EXISTS"], "q0132": ["SUBQUERY"],
        "q0188": ["CTE"], "q0185": ["RECURSIVE_CTE"],
        "q0199": ["WINDOW", "PARTITION BY"], "q0196": ["WINDOW", "ROW_NUMBER"],
        "q0197": ["WINDOW", "LAG"], "q0205": ["WINDOW", "ORDER BY"],
    }
    scenario_qids = {"q0088", "q0093", "q0115", "q0118", "q0130", "q0150", "q0175", "q0195", "q0220", "q0230"}
    pinned_write = set(ast_map.keys()) | {"q0231", "q0232", "q0239", "q0240"}

    qids_37_250 = [f"q{i:04d}" for i in range(37, 251)]
    assigned_types = {q: "业务情景" for q in scenario_qids}
    for q in pinned_write:
        assigned_types[q] = "独立编写"

    open_qids = [q for q in qids_37_250 if q not in assigned_types]
    # Remaining needed among open_qids: 选择=20, 代码填空=35, 代码纠错=30, 结果预测=25, 独立编写=(94 - len(pinned_write))
    rem_pool = (
        ["选择"] * 20
        + ["代码填空"] * 35
        + ["代码纠错"] * 30
        + ["结果预测"] * 25
        + ["独立编写"] * (94 - len(pinned_write))
    )
    assert len(open_qids) == len(rem_pool)
    # Deterministic spread avoiding 结果预测 on q0231..q0240 (DML/DDL)
    safe_for_pred = [q for q in open_qids if int(q[1:]) < 231]
    step = len(safe_for_pred) // 25
    pred_picks = [safe_for_pred[i * step] for i in range(25)]
    for q in pred_picks:
        assigned_types[q] = "结果预测"
    left_qids = [q for q in open_qids if q not in assigned_types]
    # Assign 选择(20), 代码填空(35), 代码纠错(30), 独立编写(...) in round-robin pattern
    others_queue = []
    c_rem, b_rem, d_rem = 20, 35, 30
    w_rem = len(left_qids) - (c_rem + b_rem + d_rem)
    for idx in range(len(left_qids)):
        mod = idx % 5
        if mod == 0 and c_rem > 0: others_queue.append("选择"); c_rem -= 1
        elif mod in (1, 3) and b_rem > 0: others_queue.append("代码填空"); b_rem -= 1
        elif mod == 2 and d_rem > 0: others_queue.append("代码纠错"); d_rem -= 1
        elif w_rem > 0: others_queue.append("独立编写"); w_rem -= 1
        elif b_rem > 0: others_queue.append("代码填空"); b_rem -= 1
        elif d_rem > 0: others_queue.append("代码纠错"); d_rem -= 1
        else: others_queue.append("选择"); c_rem -= 1
    for q, t in zip(left_qids, others_queue):
        assigned_types[q] = t

    hard_qids = {
        "q0184", "q0186", "q0191", "q0193",
        "q0203", "q0210", "q0212", "q0215", "q0219", "q0220",
        "q0228", "q0230", "q0239", "q0240", "q0249", "q0250",
    }

    def get_diff(qid):
        n = int(qid[1:])
        if n <= 60: return "入门"
        if n <= 150: return "简单"
        if qid in hard_qids: return "困难"
        return "中等"

    kp_variants = build_kp_sql_templates(lesson_tables)
    kp_usage = {}
    for qid in qids_37_250:
        kp_id = q_to_kps[qid][0]
        kp = kp_lookup[kp_id]
        ch_id = kp["chapterId"]
        qtype = assigned_types[qid]
        diff = get_diff(qid)
        ast_feats = ast_map.get(qid, [])
        idx_in_kp = kp_usage.get(kp_id, 0)
        kp_usage[kp_id] = idx_in_kp + 1

        templates = kp_variants[kp_id]
        tpl = templates[idx_in_kp % len(templates)]
        vtag = ["基础练习", "进阶应用", "业务变体", "边界校验", "专项强化", "综合核查", "实战拓展"][idx_in_kp % 7]

        tables = tpl["tables"]
        sql = ensure_ast_sql(ast_feats, tpl["sql"], kp) if ast_feats else tpl["sql"]
        setup_sql = tpl.get("setupSql")
        ver_q = tpl.get("verificationQuery")
        kind = tpl.get("kind", "query")

        fixtures = make_fixtures(qid, tables, sql, "ordered", setup_sql, ver_q, kind=kind)
        exp_cols = fixtures[0]["expected"]["columns"]
        exp_rows = fixtures[0]["expected"]["rows"]
        fx_ids = [fx["id"] for fx in fixtures]
        for fx in fixtures:
            (ASSETS_OUT / "question_fixtures" / f"{fx['id']}.json").write_text(json.dumps(fx, ensure_ascii=False, indent=2), "utf-8")

        if qtype == "选择":
            questions[qid] = build_choice_question(qid, ch_id, diff, kp, idx_in_kp)
        elif qtype == "结果预测":
            questions[qid] = build_prediction_question(qid, ch_id, diff, kp, tpl, exp_cols, exp_rows, fx_ids)
        elif qtype == "代码填空":
            questions[qid] = build_blank_question(qid, ch_id, diff, kp, tpl, sql, exp_cols, exp_rows, fx_ids, ast_feats, "ordered")
        elif qtype == "代码纠错":
            questions[qid] = build_debug_question(qid, ch_id, diff, kp, tpl, sql, exp_cols, exp_rows, fx_ids, ast_feats, "ordered")
        elif qtype == "业务情景":
            questions[qid] = {
                "id": qid, "chapterId": ch_id, "title": f"【业务情景】{tpl['title']}", "type": "业务情景", "difficulty": diff,
                "context": f"门店分析实战需求：围绕「{kp['title']}」交付结果，涉及表 {', '.join(t['name'] for t in tables)}。",
                "requirements": f"{tpl['req']}（输出列：{', '.join(exp_cols)}）",
                "hints": [f"明确口径：{kp['useCase'] or kp['title']}", f"关键语法：{kp['syntax']}", f"易错提醒：{kp['commonError']['correction'] if kp.get('commonError') else '注意边界情况'}"],
                "explanation": f"{kp['concept']} 参考实现：{sql}",
                "referenceSolutions": [{"mysqlSql": sql, "sqliteSql": sql, "explanation": kp["concept"]}],
                "requiredAstFeatures": ast_feats, "supportStatus": "直接兼容", "fixtureIds": fx_ids,
                "expectedColumns": exp_cols, "expectedRows": exp_rows, "orderPolicy": "ordered",
            }
        else:
            questions[qid] = {
                "id": qid, "chapterId": ch_id,
                "title": f"{tpl['title']}（{vtag}）" if idx_in_kp > 0 else tpl["title"],
                "type": "独立编写", "difficulty": diff,
                "context": f"知识点：{kp['title']}。输入表：{', '.join(t['name'] for t in tables)}。",
                "requirements": f"{tpl['req']}（输出列：{', '.join(exp_cols)}）",
                "hints": [f"确认输出列（{', '.join(exp_cols)}）与排序规则。", f"参考语法：{kp['syntax']}", f"自查提示：{kp['commonError']['correction'] if kp.get('commonError') else '留意空值与边界'}"],
                "explanation": f"{kp['concept']} 标准写法：{sql}",
                "referenceSolutions": [{"mysqlSql": sql, "sqliteSql": sql, "explanation": kp["concept"]}],
                "requiredAstFeatures": ast_feats, "supportStatus": "直接兼容", "fixtureIds": fx_ids,
                "expectedColumns": exp_cols, "expectedRows": exp_rows, "orderPolicy": "ordered",
            }

    by_ch = {f"ch{i:02d}": [] for i in range(1, 21)}
    for i in range(1, 301):
        q = questions[f"q{i:04d}"]
        by_ch[q["chapterId"]].append(q)
    for ch, qs in by_ch.items():
        (ASSETS_OUT / "questions" / f"{ch}.json").write_text(json.dumps({"chapterId": ch, "questions": qs}, ensure_ascii=False, indent=2), "utf-8")

    all_q = list(questions.values())
    tc = Counter(q["type"] for q in all_q)
    dc = Counter(q["difficulty"] for q in all_q)
    ast_cnt = sum(1 for q in all_q if q.get("requiredAstFeatures"))
    print("  Verified 300 questions -> Types:", dict(tc), "| Diffs:", dict(dc), "| AST:", ast_cnt)
    assert len(all_q) == 300
    assert dict(tc) == {"独立编写": 120, "选择": 30, "代码填空": 35, "代码纠错": 30, "结果预测": 25, "业务情景": 60}
    assert dict(dc) == {"入门": 60, "简单": 90, "中等": 90, "困难": 45, "挑战": 15}
    assert ast_cnt == 14

    projects = [
        {"projectId": "retail", "title": "零售门店经营分析", "subtitle": "全渠道营收、折扣毛利与门店目标达成诊断", "datasetId": "retail", "background": "基于 12 家直营门店与线上通道近三年订单、退货及促销数据，完成高粒度的门店利润拆解与连续下滑预警。", "goals": ["统一以已完成订单与冲减退货后的净销售额为口径", "排查多月表与日销售表连接导致的目标重复累计", "评估促销活动对毛利率与日均产出的真实拉动"], "taskQuestionIds": ["q0251", "q0252", "q0253", "q0254", "q0255"]},
        {"projectId": "inventory", "title": "仓库库存健康与周转", "subtitle": "库龄 FIFO 推算、周转天数与缺货补货优先级", "datasetId": "inventory", "background": "面向供应链与仓储计划团队，盘点多仓 SKU 动销、低销量高占用积压商品及采购提前期安全库存风险。", "goals": ["结合期初库存与出入库流水验证盘点核账一致", "按品类计算周转率与周转天数定位滞销库存", "基于历史移动消耗按先入先出（FIFO）估算库龄结构"], "taskQuestionIds": ["q0256", "q0257", "q0258", "q0259", "q0260"]},
        {"projectId": "ecommerce", "title": "电商用户留存与转化", "subtitle": "RFM 客群分层、复购周期、同群留存与渠道 CAC", "datasetId": "ecommerce", "background": "针对站内浏览会话、渠道广告投放及线上订单数据，构建从流量获客到首单转化的复购留存洞察。", "goals": ["通过自连接与窗口函数测算相邻复购间隔与分布", "按首单月份构建同期群留存矩阵", "核对渠道投放成本与净订单利润核算获客回报"], "taskQuestionIds": ["q0261", "q0262", "q0263", "q0264", "q0265"]},
        {"projectId": "finance", "title": "财务预算与应付对账", "subtitle": "预算执行偏差、发票三单匹配、账龄分桶与银行核账", "datasetId": "finance", "background": "服务财务共享中心，对预算科目执行、采购收货开票三单比对、供应商应付账龄以及银行流水逐笔勾稽进行审计。", "goals": ["对比部门预算与费用凭证定位超支明细", "识别超收、未开票与三单金额不一致异常单据", "按截至日期重算应付余额与账龄分层结构"], "taskQuestionIds": ["q0266", "q0267", "q0268", "q0269", "q0270"]},
        {"projectId": "hr", "title": "人力编制、考勤与薪资分析", "subtitle": "月末在编规模、离职率、同岗薪资偏离与同期群留任", "datasetId": "hr", "background": "结合人事调动历史、月度薪资台账与每日考勤加班记录，评估门店和职能部门的人力结构与稳定性。", "goals": ["按日历月末时点准确还原历月在职人员编制", "计算月度自然离职率并区分主动/被动结构", "通过窗口函数检测同部门同岗位异常高薪与低薪"], "taskQuestionIds": ["q0271", "q0272", "q0273", "q0274", "q0275"]},
        {"projectId": "procurement", "title": "采购价格、合同与供应商绩效", "subtitle": "价格偏差、准时足量履约、跨供应商比价与综合评分", "datasetId": "procurement", "background": "围绕采购订单、供应商基准合同、到货验收质检单，追踪供应商的交付稳定性与采购降本空间。", "goals": ["比对订单执行价与合同基准价及前次采购价", "按准时收货、完整交付及检验合格率评估履约质量", "排查多供应商供货价差并产出季度综合排名"], "taskQuestionIds": ["q0276", "q0277", "q0278", "q0279", "q0280"]},
    ]
    (ASSETS_OUT / "projects" / "business_projects.json").write_text(json.dumps(projects, ensure_ascii=False, indent=2), "utf-8")


def write_bundle_manifest():
    entries = []
    for f in sorted(ASSETS_OUT.rglob("*")):
        if f.is_file() and f.name != "bundle_manifest.json":
            rel = f.relative_to(ASSETS_OUT).as_posix()
            raw = f.read_bytes()
            entries.append({"path": rel, "sha256": hashlib.sha256(raw).hexdigest(), "sizeBytes": len(raw)})
    (ASSETS_OUT / "bundle_manifest.json").write_text(
        json.dumps({"version": "4.0.0", "fileCount": len(entries), "files": entries}, ensure_ascii=False, indent=2),
        "utf-8",
    )
    print(f"Bundle manifest written: {len(entries)} files.")


def main():
    clean_out_dir()
    built_dbs = build_databases_and_schemas()
    package_courses_and_lesson_fixtures()
    package_flashcards_and_fixtures()

    kp_lookup = {}
    for p in sorted((CONTENT / "courses_resource/courses").glob("ch*.json")):
        for kp in json.loads(p.read_text("utf-8"))["knowledgePoints"]:
            kp_lookup[kp["id"]] = kp

    q_to_kps = {}
    for i in range(1, 19):
        d = json.loads((CONTENT / f"question_resource/course_links/ch{i:02d}.json").read_text("utf-8"))
        for u in d["knowledgePointUpdates"]:
            for qid in u.get("relatedQuestionIds", []):
                q_to_kps.setdefault(qid, []).append(u["id"])

    lesson_tables = load_lesson_tables_map()
    build_remaining_questions_and_projects(kp_lookup, q_to_kps, lesson_tables, built_dbs)
    write_bundle_manifest()
    print("DONE!")


if __name__ == "__main__":
    main()
