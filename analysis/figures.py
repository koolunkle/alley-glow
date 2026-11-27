"""분석 그림 3장 → docs/figures/. zones.py 결과와 휴지통 좌표(data/raw)를 씀.

1. zones_score.png: 구역 추천도 격자
2. zones_pop_bins.png: 금·토 야간 인구 격자 + 가로휴지통 위치
3. pop_hourly.png: 36칸 생활인구 합계, 금·토와 평일 시간대별
"""
import csv
import pathlib
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from geo import UTMK, grid_origin, to_tm  # noqa: E402
from zones import CELL, COLS, ROWS, STATION  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "figures"
S = 2  # 2배로 그린 뒤 줄여 가장자리를 매끄럽게
W = 1600

SURFACE = "#fcfcfb"
INK, INK2, MUTED, GRID = "#0b0b0b", "#52514e", "#8a8984", "#e4e3df"
BLUE_RAMP = ["#cde2fb", "#b7d3f6", "#9ec5f4", "#86b6ef", "#6da7ec", "#5598e7", "#3987e5",
             "#2a78d6", "#256abf", "#1c5cab", "#184f95", "#104281", "#0d366b"]
SERIES_1, SERIES_2 = "#2a78d6", "#eb6834"
SOURCE = "출처: 서울특별시 열린데이터광장(서울 생활인구 250m 2026.11, 가로쓰레기통 설치정보 2025.11, 상권분석서비스 2025), 서울특별시·KT"


def font(size, bold=False):
    return ImageFont.truetype(f"C:/Windows/Fonts/{'malgunbd' if bold else 'malgun'}.ttf", size * S)


def ramp(t):
    t = min(max(t, 0.0), 1.0)
    pos = t * (len(BLUE_RAMP) - 1)
    i = min(int(pos), len(BLUE_RAMP) - 2)
    a, b = BLUE_RAMP[i], BLUE_RAMP[i + 1]
    f = pos - i
    return "#" + "".join(f"{round(int(a[k:k + 2], 16) * (1 - f) + int(b[k:k + 2], 16) * f):02x}" for k in (1, 3, 5))


def luminance(hex_color):
    r, g, b = (int(hex_color[k:k + 2], 16) / 255 for k in (1, 3, 5))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def canvas(h):
    img = Image.new("RGB", (W * S, h * S), SURFACE)
    return img, ImageDraw.Draw(img)


def save(img, name):
    OUT.mkdir(parents=True, exist_ok=True)
    img.resize((img.width // S, img.height // S), Image.LANCZOS).save(OUT / name, optimize=True)
    print(OUT / name)


def text(d, xy, s, size, fill=INK, bold=False, anchor="la"):
    d.text((xy[0] * S, xy[1] * S), s, font=font(size, bold), fill=fill, anchor=anchor)


def header(d, title, subtitle):
    text(d, (64, 56), title, 40, bold=True)
    text(d, (64, 116), subtitle, 22, INK2)


def footer(d, h):
    text(d, (64, h - 48), SOURCE, 16, MUTED)


def load_cells():
    with (ROOT / "data" / "zones_itaewon.csv").open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def load_bins():
    path = ROOT / "data" / "raw" / "bins_yongsan_geocoded.csv"
    with path.open(encoding="utf-8") as f:
        return [to_tm(float(r["lat"]), float(r["lng"]), UTMK) for r in csv.DictReader(f) if r["lat"]]


class Grid:
    """격자 그림 좌표. 칸 G00의 왼쪽 위가 (left, top)."""

    def __init__(self, cells, left=64, top=180, size=176, gap=4):
        self.left, self.top, self.size, self.gap = left, top, size, gap
        x0, y0 = grid_origin(cells[0]["grid_code"])
        self.x0, self.ytop = x0, y0 + CELL

    def cell_box(self, row, col):
        x = self.left + col * (self.size + self.gap)
        y = self.top + row * (self.size + self.gap)
        return x, y, x + self.size, y + self.size

    def to_px(self, x, y):
        pitch = self.size + self.gap
        return self.left + (x - self.x0) / CELL * pitch, self.top + (self.ytop - y) / CELL * pitch

    @property
    def right(self):
        return self.left + COLS * (self.size + self.gap) - self.gap

    @property
    def bottom(self):
        return self.top + ROWS * (self.size + self.gap) - self.gap


def draw_cells(d, g, cells, value):
    for c in cells:
        d.rounded_rectangle([v * S for v in g.cell_box(int(c["row"]), int(c["col"]))], radius=16 * S, fill=ramp(value(c)))


def draw_labels(d, g, cells, value, label):
    """칸 번호와 값. 점 위에 그려도 읽히게 칸 색 테두리를 두름."""
    for c in cells:
        box = g.cell_box(int(c["row"]), int(c["col"]))
        fill = ramp(value(c))
        ink = "#ffffff" if luminance(fill) < 0.45 else INK
        text(d, (box[0] + 14, box[1] + 12), c["id"], 16, ink)
        d.text(((box[0] + box[2]) / 2 * S, ((box[1] + box[3]) / 2 + 6) * S), label(c), font=font(30, True),
               fill=ink, anchor="mm", stroke_width=4 * S, stroke_fill=fill)


def draw_station(d, g):
    px, py = g.to_px(*to_tm(*STATION, UTMK))
    r = 14
    d.ellipse([(px - r - 3) * S, (py - r - 3) * S, (px + r + 3) * S, (py + r + 3) * S], fill="#ffffff")
    d.ellipse([(px - r) * S, (py - r) * S, (px + r) * S, (py + r) * S], fill=INK)
    text(d, (px, py), "역", 14, "#ffffff", bold=True, anchor="mm")


def draw_side(d, g, title, lo, hi, extra=()):
    """오른쪽 범례: 색 막대, 축척, 방위, 그 밖의 항목."""
    x = g.right + 64
    text(d, (x, g.top), title, 22, bold=True)
    bar_top, bar_h, bar_w = g.top + 48, 320, 32
    for i in range(bar_h):
        d.rectangle([x * S, (bar_top + i) * S, (x + bar_w) * S, (bar_top + i + 1) * S], fill=ramp(1 - i / bar_h))
    text(d, (x + bar_w + 14, bar_top), hi, 18, INK2, anchor="lm")
    text(d, (x + bar_w + 14, bar_top + bar_h), lo, 18, INK2, anchor="lm")
    y = bar_top + bar_h + 56
    r = 14
    d.ellipse([x * S, y * S, (x + 2 * r) * S, (y + 2 * r) * S], fill=INK)
    text(d, (x + r, y + r), "역", 14, "#ffffff", bold=True, anchor="mm")
    text(d, (x + 2 * r + 14, y + r), "이태원역", 18, INK2, anchor="lm")
    y += 56
    for draw_icon, label in extra:
        draw_icon(x, y)
        text(d, (x + 2 * r + 14, y + r), label, 18, INK2, anchor="lm")
        y += 56
    pitch = g.size + g.gap
    d.line([x * S, (y + 14) * S, (x + pitch) * S, (y + 14) * S], fill=INK2, width=2 * S)
    for xx in (x, x + pitch):
        d.line([xx * S, (y + 6) * S, xx * S, (y + 22) * S], fill=INK2, width=2 * S)
    text(d, (x + pitch + 14, y + 14), "250m", 18, INK2, anchor="lm")
    y += 56
    d.polygon([((x + 14) * S, y * S), ((x + 4) * S, (y + 28) * S), ((x + 24) * S, (y + 28) * S)], fill=INK2)
    text(d, (x + 42, y + 14), "북", 18, INK2, anchor="lm")


def fig_score(cells):
    h = 1480
    img, d = canvas(h)
    header(d, "이태원 야간 청소 구역 추천도", "250m 격자 36칸 · 추천도 = 야간 인구 0.5 + 휴지통 공백 0.3 + 야간 매출 0.2 (가중치는 가정)")
    g = Grid(cells)
    draw_cells(d, g, cells, lambda c: float(c["score"]))
    draw_labels(d, g, cells, lambda c: float(c["score"]), lambda c: f"{float(c['score']):.2f}")
    top3 = sorted(cells, key=lambda c: -float(c["score"]))[:3]
    for rank, c in enumerate(top3, 1):
        box = g.cell_box(int(c["row"]), int(c["col"]))
        cx = box[2] - 26
        d.ellipse([(cx - 16) * S, (box[1] + 10) * S, (cx + 16) * S, (box[1] + 42) * S], fill="#ffffff")
        text(d, (cx, box[1] + 26), str(rank), 18, INK, bold=True, anchor="mm")
    draw_station(d, g)
    draw_side(d, g, "추천도", "0 (낮음)", "1 (높음)")
    y = g.bottom + 40
    for rank, c in enumerate(top3, 1):
        text(d, (64, y), f"{rank}. {c['id']} {c['name']}  추천도 {float(c['score']):.2f} · 금·토 야간 인구 {float(c['pop']):,.0f}명 · 가장 가까운 휴지통 {float(c['bin_dist']):.0f}m", 20, INK2)
        y += 36
    footer(d, h)
    save(img, "zones_score.png")


def fig_pop_bins(cells, bins):
    h = 1480
    img, d = canvas(h)
    inside = [(x, y) for x, y in bins if 0 <= (x - Grid(cells).x0) < COLS * CELL and 0 <= (Grid(cells).ytop - y) < ROWS * CELL]
    header(d, "금·토 야간 인구와 가로휴지통 위치", f"칸 색: 금·토(11/13·14) 19~23시 평균 생활인구 · 점: 가로휴지통 {len(inside)}개(용산구 설치정보, 주소를 좌표로 변환)")
    g = Grid(cells)
    top = max(float(c["pop"]) for c in cells)
    draw_cells(d, g, cells, lambda c: float(c["pop"]) / top)
    r = 7

    def dot(px, py):
        d.ellipse([(px - r - 2) * S, (py - r - 2) * S, (px + r + 2) * S, (py + r + 2) * S], fill="#ffffff")
        d.ellipse([(px - r) * S, (py - r) * S, (px + r) * S, (py + r) * S], fill=SERIES_2)

    for x, y in inside:
        dot(*g.to_px(x, y))
    draw_labels(d, g, cells, lambda c: float(c["pop"]) / top, lambda c: f"{float(c['pop']):,.0f}")
    draw_station(d, g)
    draw_side(d, g, "야간 인구(명)", "0", f"{top:,.0f}", extra=[(lambda x, y: dot(x + 14, y + 14), "가로휴지통")])
    top2 = sorted(cells, key=lambda c: -float(c["pop"]))[:2]
    y = g.bottom + 40
    for c in top2:
        text(d, (64, y), f"{c['id']} {c['name']}: 야간 인구 {float(c['pop']):,.0f}명, 칸 안 휴지통 {c['bins_in']}개", 20, INK2)
        y += 36
    text(d, (64, y), "가장 붐비는 칸에도 휴지통은 있다 → 문제는 휴지통 수보다 주말 밤의 수거 인력이라는 가설", 20, INK, bold=True)
    footer(d, h)
    save(img, "zones_pop_bins.png")


def fig_hourly():
    with (ROOT / "data" / "pop_hourly_itaewon.csv").open(encoding="utf-8") as f:
        rows = [(int(r["hour"]), int(r["weekend"]), int(r["weekday"])) for r in csv.DictReader(f)]
    h = 900
    img, d = canvas(h)
    at22 = next(r for r in rows if r[0] == 22)
    header(d, "주말 밤, 이태원 인구는 평일보다 늦게까지 남는다",
           f"이태원역 주변 36칸 생활인구 합계 · 22시 금·토 {at22[1]:,}명, 평일 {at22[2]:,}명 (+{at22[1] / at22[2] - 1:.0%})")
    left, right, top, bottom = 140, W - 200, 200, h - 150
    ymax = 60_000

    def px(hour, v):
        return left + (right - left) * hour / 23, bottom - (bottom - top) * v / ymax

    x19, x23 = px(19, 0)[0], px(23, 0)[0]
    d.rectangle([x19 * S, top * S, x23 * S, bottom * S], fill="#f0efec")
    text(d, ((x19 + x23) / 2, top + 20), "근무 19~23시", 18, INK2, bold=True, anchor="mm")
    for v in range(0, ymax + 1, 10_000):
        y = px(0, v)[1]
        d.line([left * S, y * S, right * S, y * S], fill=GRID if v else INK2, width=S)
        text(d, (left - 16, y), f"{v // 1000}천" if v else "0", 18, INK2, anchor="rm")
    for hour in range(0, 24, 3):
        text(d, (px(hour, 0)[0], bottom + 24), f"{hour}시", 18, INK2, anchor="mt")
    for idx, color, name in ((1, SERIES_1, "금·토"), (2, SERIES_2, "평일(수·목·월·화)")):
        pts = [px(r[0], r[idx]) for r in rows]
        d.line([(x * S, y * S) for x, y in pts], fill=color, width=3 * S, joint="curve")
        for r in rows:
            if 19 <= r[0] <= 23:
                x, y = px(r[0], r[idx])
                d.ellipse([(x - 7) * S, (y - 7) * S, (x + 7) * S, (y + 7) * S], fill=color, outline=SURFACE, width=2 * S)
        x, y = pts[-1]
        text(d, (x + 16, y), name, 20, INK, bold=True, anchor="lm")
    footer(d, h)
    save(img, "pop_hourly.png")


def main():
    cells = load_cells()
    fig_score(cells)
    fig_pop_bins(cells, load_bins())
    fig_hourly()


if __name__ == "__main__":
    main()
