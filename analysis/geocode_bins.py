"""용산구 가로휴지통 주소 → 위경도(카카오 로컬 API). 결과는 data/raw/(git 제외)에 캐시.

키: local.properties의 kakao.key. 이미 좌표가 있는 행은 다시 묻지 않음.
"""
import csv
import json
import pathlib
import sys
import time
import urllib.parse
import urllib.request

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from geo import read_xlsx  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "data" / "raw"
OUT = RAW / "bins_yongsan_geocoded.csv"
# shortcut: 용산구만 씀, 분석 범위가 구 경계를 넘으면 이웃 구(중구 등)도 추가


def kakao_key():
    for line in (ROOT / "local.properties").read_text(encoding="utf-8").splitlines():
        if line.startswith("kakao.key="):
            return line.split("=", 1)[1].strip()
    sys.exit("local.properties에 kakao.key 없음")


def search(kind, query, key):
    url = f"https://dapi.kakao.com/v2/local/search/{kind}.json?" + urllib.parse.urlencode({"query": query, "size": 1})
    req = urllib.request.Request(url, headers={"Authorization": f"KakaoAK {key}"})
    with urllib.request.urlopen(req, timeout=10) as r:
        docs = json.load(r)["documents"]
    return (float(docs[0]["y"]), float(docs[0]["x"])) if docs else None


def main():
    key = kakao_key()
    xlsx = next(RAW.glob("*가로쓰레기통*.xlsx"))
    rows = read_xlsx(xlsx)
    h = next(i for i, r in enumerate(rows) if "연번" in r)  # 위에 제목 행이 있음
    head = [c.strip() for c in rows[h]]
    body = [r for r in rows[h + 1:] if len(r) > 2 and r[1] == "용산구"]
    done = {}
    if OUT.exists():
        with OUT.open(encoding="utf-8") as f:
            done = {r["연번"]: r for r in csv.DictReader(f) if r["lat"]}
    out, misses = [], 0
    for r in body:
        rec = dict(zip(head, r))
        if rec["연번"] in done:
            out.append(done[rec["연번"]])
            continue
        hit, how = search("address", f"서울 용산구 {rec['설치위치']}", key), "address"
        if not hit and rec.get("세부 위치"):
            # 주소가 없거나 틀린 행은 세부 위치(정류장·건물 이름)로 다시 찾음
            hit, how = search("keyword", f"용산구 {rec['세부 위치'].strip()}", key), "keyword"
        misses += not hit
        rec.update(lat=hit[0] if hit else "", lng=hit[1] if hit else "", match=how if hit else "")
        out.append(rec)
        time.sleep(0.05)
    with OUT.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=head + ["lat", "lng", "match"])
        w.writeheader()
        w.writerows(out)
    by = {}
    for r in out:
        by[r["match"] or "실패"] = by.get(r["match"] or "실패", 0) + 1
    print(len(out), by)


if __name__ == "__main__":
    main()
