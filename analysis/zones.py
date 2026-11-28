"""이태원역 중심 6×6(250m) 구역의 추천도 계산.

추천도 = 야간 인구 0.5 + 휴지통 공백 0.3 + 야간 매출 0.2 (각 36칸 안에서 0~1 최소-최대 정규화, 가중치는 가정)
- 야간 인구: 서울 생활인구(250m 격자), 금·토 19~23시(시간 19·20·21·22) 평균
- 휴지통 공백: 칸 중심에서 가장 가까운 가로휴지통까지 거리(geocode_bins.py 결과)
- 야간 매출: 상권 추정매출 21~24시(2025년 분기 평균)를 상권 영역과 겹친 면적 비율로 나눔
시간대별 추천도(score_19 … score_22): 야간 인구만 그 한 시간 값으로 바꿔 같은 식으로 계산(휴지통·매출은 시간 구분 없음)
결과: data/zones_itaewon.csv, data/pop_hourly_itaewon.csv(36칸 합계, 시간대별 금·토·평일 평균)
"""
import csv
import glob
import math
import pathlib
import statistics
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from geo import CENTRAL, UTMK, from_tm, grid_origin, inside, read_shapefile, to_tm  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
RAW = ROOT / "data" / "raw"
OUT = ROOT / "data" / "zones_itaewon.csv"
OUT_HOURLY = ROOT / "data" / "pop_hourly_itaewon.csv"

STATION = (37.5345, 126.9944)  # 이태원역
CELL, COLS, ROWS = 250, 6, 6
WEIGHTS = {"pop": 0.5, "bin_gap": 0.3, "sales": 0.2}
NIGHT_HOURS = ("19", "20", "21", "22")
WEEKEND = ("20261113", "20261114")  # 금·토
WEEKDAY = ("20261111", "20261112", "20261116", "20261117")  # 수·목·월·화(비교용)


def build_cells():
    """역이 있는 칸을 왼쪽에서 3번째, 위에서 4번째에 둠(동쪽 한남·북쪽 해방촌 쪽으로 넓게)."""
    sx, sy = to_tm(*STATION, UTMK)
    x0 = int(sx // CELL) * CELL - 2 * CELL
    ytop = int(sy // CELL) * CELL + 3 * CELL
    cells = []
    for r in range(ROWS):
        for c in range(COLS):
            x, y = x0 + c * CELL, ytop - r * CELL
            code = f"다사{(x - 900_000) // 10:04d}{(y - 1_900_000) // 10:04d}"
            assert grid_origin(code) == (x, y)
            lat, lng = from_tm(x + CELL / 2, y + CELL / 2, UTMK)
            cells.append(dict(id=f"G{r * COLS + c:02d}", row=r, col=c, grid_code=code, lat=lat, lng=lng, x=x, y=y))
    return cells


def population(cells):
    codes = {c["grid_code"]: c for c in cells}
    for c in cells:
        c.update(pop=0.0, pop_weekday=0.0, hourly={}, dongs={})
    days = {d: (WEEKEND + WEEKDAY).index(d) for d in WEEKEND + WEEKDAY}
    for day in days:
        path = RAW / f"250_LOCAL_RESD_{day}.csv"
        with path.open(encoding="cp949") as f:
            rows = csv.reader(f)
            next(rows)
            for row in rows:
                cell = codes.get(row[3])
                if not cell or row[4] in ("", "*"):
                    continue
                v = float(row[4])
                key = (day, row[1])
                cell["hourly"][key] = cell["hourly"].get(key, 0.0) + v
                dong = row[2].strip()
                cell["dongs"][dong] = cell["dongs"].get(dong, 0.0) + v
    for c in cells:
        def night_avg(ds):
            return statistics.mean(sum(c["hourly"].get((d, h), 0.0) for d in ds) / len(ds) for h in NIGHT_HOURS)
        c["pop"], c["pop_weekday"] = night_avg(WEEKEND), night_avg(WEEKDAY)
        for h in NIGHT_HOURS:
            c[f"pop_{h}"] = sum(c["hourly"].get((d, h), 0.0) for d in WEEKEND) / len(WEEKEND)
            c[f"pop_weekday_{h}"] = sum(c["hourly"].get((d, h), 0.0) for d in WEEKDAY) / len(WEEKDAY)


def sales(cells):
    """상권 21~24시 매출(분기 평균) → 칸과 겹치는 면적 비율만큼. 칸마다 10×10 점 표본."""
    totals, quarters = {}, set()
    path = next(RAW.glob("*추정매출-상권*.csv"))
    with path.open(encoding="cp949") as f:
        for r in csv.DictReader(f):
            totals[r["상권_코드"]] = totals.get(r["상권_코드"], 0) + int(r["시간대_21~24_매출_금액"] or 0)
            quarters.add(r["기준_년분기_코드"])
    base = glob.glob(str(RAW / "*영역-상권*.shp"))[0][:-4]
    areas = read_shapefile(base)
    # 칸 범위 근처 상권만(EPSG:5181 경계 상자)
    corners = [to_tm(*from_tm(x, y, UTMK), CENTRAL) for x, y in (
        (cells[0]["x"], cells[0]["y"] + CELL), (cells[-1]["x"] + CELL, cells[-1]["y"]))]
    (minx, maxy), (maxx, miny) = corners
    near = []
    for attr, rings in areas:
        xs = [p[0] for ring in rings for p in ring]
        ys = [p[1] for ring in rings for p in ring]
        if rings and max(xs) > minx - 300 and min(xs) < maxx + 300 and max(ys) > miny - 300 and min(ys) < maxy + 300:
            near.append((attr, rings, (min(xs), max(xs), min(ys), max(ys))))
    n = 10
    for c in cells:
        c["sales"], c["areas"] = 0.0, {}
        for i in range(n):
            for j in range(n):
                px, py = to_tm(*from_tm(c["x"] + (i + 0.5) * CELL / n, c["y"] + (j + 0.5) * CELL / n, UTMK), CENTRAL)
                for attr, rings, (bx0, bx1, by0, by1) in near:
                    if bx0 <= px <= bx1 and by0 <= py <= by1 and inside(px, py, rings):
                        code = attr["TRDAR_CD"]
                        share = (CELL / n) ** 2 / float(attr["RELM_AR"])
                        c["sales"] += totals.get(code, 0) / len(quarters) * share
                        c["areas"][attr["TRDAR_CD_N"]] = c["areas"].get(attr["TRDAR_CD_N"], 0) + 1
    return {a["ADSTRD_CD"]: a["ADSTRD_CD_"] for a, _ in areas}


def bin_gap(cells):
    path = RAW / "bins_yongsan_geocoded.csv"
    if not path.exists():
        return False
    with path.open(encoding="utf-8") as f:
        bins = [to_tm(float(r["lat"]), float(r["lng"]), UTMK) for r in csv.DictReader(f) if r["lat"]]
    for c in cells:
        cx, cy = c["x"] + CELL / 2, c["y"] + CELL / 2
        c["bin_dist"] = min(math.hypot(bx - cx, by - cy) for bx, by in bins)
        c["bins_in"] = sum(c["x"] <= bx < c["x"] + CELL and c["y"] <= by < c["y"] + CELL for bx, by in bins)
    return True


def normalize(cells, key, out):
    lo, hi = min(c[key] for c in cells), max(c[key] for c in cells)
    for c in cells:
        c[out] = (c[key] - lo) / (hi - lo) if hi > lo else 0.0


def name_cells(cells, dong_names):
    """칸의 대부분을 덮는 상권 이름, 없으면 인구가 가장 많은 행정동. 같은 이름은 번호를 붙임."""
    seen = {}
    for c in cells:
        label = max(c["areas"], key=c["areas"].get) if c["areas"] and max(c["areas"].values()) >= 30 else None
        if not label:
            dong = max(c["dongs"], key=c["dongs"].get) if c["dongs"] else ""
            label = dong_names.get(dong, "용산구")
        seen[label] = seen.get(label, 0) + 1
        c["base_name"] = label
    count = {}
    for c in cells:
        if seen[c["base_name"]] > 1:
            count[c["base_name"]] = count.get(c["base_name"], 0) + 1
            c["name"] = f"{c['base_name']} {count[c['base_name']]}"
        else:
            c["name"] = c["base_name"]


def main():
    cells = build_cells()
    population(cells)
    dong_names = sales(cells)
    has_bins = bin_gap(cells)
    normalize(cells, "pop", "pop_n")
    normalize(cells, "sales", "sales_n")
    if has_bins:
        normalize(cells, "bin_dist", "bin_gap_n")
    else:
        print("휴지통 좌표 없음: 인구 0.5·매출 0.2만으로 계산(임시)")
    for h in NIGHT_HOURS:
        normalize(cells, f"pop_{h}", f"pop_n_{h}")

    def score(c, pop_n):
        s = (WEIGHTS["pop"] * pop_n + WEIGHTS["sales"] * c["sales_n"]
             + (WEIGHTS["bin_gap"] * c["bin_gap_n"] if has_bins else 0.0))
        return s if has_bins else s / (WEIGHTS["pop"] + WEIGHTS["sales"])
    for c in cells:
        c["score"] = score(c, c["pop_n"])
        for h in NIGHT_HOURS:
            c[f"score_{h}"] = score(c, c[f"pop_n_{h}"])
    name_cells(cells, dong_names)
    cols = ["id", "row", "col", "grid_code", "name", "lat", "lng", "pop", "pop_weekday", "bin_dist", "bins_in",
            "sales", "pop_n", "bin_gap_n", "sales_n", "score"]
    cols += [f"{k}_{h}" for k in ("pop", "pop_weekday", "score") for h in NIGHT_HOURS]
    with OUT.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(cols)
        for c in cells:
            w.writerow([round(c[k], 4 if k in ("pop_n", "bin_gap_n", "sales_n", "score") or k.startswith("score_") else 6 if k in ("lat", "lng") else 1)
                        if isinstance(c.get(k), float) else c.get(k, "") for k in cols])
    with OUT_HOURLY.open("w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["hour", "weekend", "weekday"])
        for h in range(24):
            hh = f"{h:02d}"
            def avg(ds):
                return sum(c["hourly"].get((d, hh), 0.0) for c in cells for d in ds) / len(ds)
            w.writerow([h, round(avg(WEEKEND)), round(avg(WEEKDAY))])
    print(f"{OUT.name}, {OUT_HOURLY.name} 저장")


if __name__ == "__main__":
    main()
