"""좌표 변환(GRS80 횡메르카토르)과 원본 파일 읽기. 외부 라이브러리 없이 numpy도 쓰지 않음."""
import math
import re
import struct
import zipfile
import xml.etree.ElementTree as ET

A = 6378137.0
F = 1 / 298.257222101
E2 = F * (2 - F)
EP2 = E2 / (1 - E2)

# (중앙 경선, 원점 위도, 축척, 가산 동, 가산 북)
UTMK = (127.5, 38.0, 0.9996, 1_000_000.0, 2_000_000.0)  # EPSG:5179, 생활인구 격자
CENTRAL = (127.0, 38.0, 1.0, 200_000.0, 500_000.0)       # EPSG:5181, 상권 영역 .prj


def _meridian(phi):
    e4, e6 = E2 * E2, E2 ** 3
    return A * ((1 - E2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * phi
                - (3 * E2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * math.sin(2 * phi)
                + (15 * e4 / 256 + 45 * e6 / 1024) * math.sin(4 * phi)
                - (35 * e6 / 3072) * math.sin(6 * phi))


def to_tm(lat, lng, proj):
    """위경도(도) → 평면 좌표(m). Snyder 급수."""
    lon0, lat0, k0, fe, fn = proj
    phi, lam = math.radians(lat), math.radians(lng - lon0)
    s, c = math.sin(phi), math.cos(phi)
    n = A / math.sqrt(1 - E2 * s * s)
    t = math.tan(phi) ** 2
    cc = EP2 * c * c
    a = lam * c
    x = fe + k0 * n * (a + (1 - t + cc) * a ** 3 / 6 + (5 - 18 * t + t * t + 72 * cc - 58 * EP2) * a ** 5 / 120)
    y = fn + k0 * (_meridian(phi) - _meridian(math.radians(lat0)) + n * math.tan(phi) * (
        a * a / 2 + (5 - t + 9 * cc + 4 * cc * cc) * a ** 4 / 24
        + (61 - 58 * t + t * t + 600 * cc - 330 * EP2) * a ** 6 / 720))
    return x, y


def from_tm(x, y, proj):
    """평면 좌표 → 위경도. to_tm의 뉴턴 반복 역산(서울 범위에서 1mm 이내)."""
    lat, lng = proj[1] + (y - proj[4]) / 111_000, proj[0] + (x - proj[3]) / 88_000
    for _ in range(10):
        x0, y0 = to_tm(lat, lng, proj)
        dx, dy = x - x0, y - y0
        if abs(dx) < 1e-4 and abs(dy) < 1e-4:
            break
        x1, y1 = to_tm(lat + 1e-6, lng, proj)
        x2, y2 = to_tm(lat, lng + 1e-6, proj)
        j = ((x1 - x0) / 1e-6, (x2 - x0) / 1e-6, (y1 - y0) / 1e-6, (y2 - y0) / 1e-6)
        det = j[0] * j[3] - j[1] * j[2]
        lat += (j[3] * dx - j[1] * dy) / det
        lng += (-j[2] * dx + j[0] * dy) / det
    return lat, lng


def grid_origin(code):
    """국가지점번호(예: 다사52255325) → 격자 왼쪽 아래 UTM-K 좌표. 다=X 900km, 사=Y 1900km."""
    letters = "가나다라마바사아자차카타파하"
    gx, gy = letters.index(code[0]), letters.index(code[1])
    return 700_000 + gx * 100_000 + int(code[2:6]) * 10, 1_300_000 + gy * 100_000 + int(code[6:10]) * 10


def read_xlsx(path):
    """첫 시트를 행 목록으로. 공유 문자열·인라인 문자열 지원."""
    ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    with zipfile.ZipFile(path) as z:
        shared = []
        if "xl/sharedStrings.xml" in z.namelist():
            for si in ET.fromstring(z.read("xl/sharedStrings.xml")).findall("m:si", ns):
                shared.append("".join(t.text or "" for t in si.iter(f"{{{ns['m']}}}t")))
        sheet = ET.fromstring(z.read("xl/worksheets/sheet1.xml"))
    rows = []
    for r in sheet.iter(f"{{{ns['m']}}}row"):
        row = {}
        for c in r.findall("m:c", ns):
            col = re.match(r"[A-Z]+", c.get("r")).group()
            idx = 0
            for ch in col:
                idx = idx * 26 + ord(ch) - 64
            v = c.find("m:v", ns)
            if c.get("t") == "s":
                val = shared[int(v.text)]
            elif c.get("t") == "inlineStr":
                val = "".join(t.text or "" for t in c.iter(f"{{{ns['m']}}}t"))
            else:
                val = v.text if v is not None else ""
            row[idx - 1] = val
        rows.append([row.get(i, "") for i in range(max(row) + 1)] if row else [])
    return rows


def read_shapefile(base, encoding="utf-8"):
    """다각형 .shp + .dbf → [(속성 dict, [고리 좌표 목록])]."""
    d = open(base + ".dbf", "rb").read()
    n, hl, rl = struct.unpack("<IHH", d[4:12])
    fields, p = [], 32
    while d[p] != 0x0D:
        fields.append((d[p:p + 11].split(b"\0")[0].decode(), d[p + 16]))
        p += 32
    attrs = []
    for i in range(n):
        r, q, rec = d[hl + i * rl + 1:hl + (i + 1) * rl], 0, {}
        for name, ln in fields:
            rec[name] = r[q:q + ln].decode(encoding, "replace").strip()
            q += ln
        attrs.append(rec)
    s = open(base + ".shp", "rb").read()
    shapes, p = [], 100
    while p < len(s):
        _, length = struct.unpack(">ii", s[p:p + 8])
        body = s[p + 8:p + 8 + length * 2]
        p += 8 + length * 2
        if struct.unpack("<i", body[:4])[0] != 5:
            shapes.append([])
            continue
        nparts, npts = struct.unpack("<ii", body[36:44])
        parts = list(struct.unpack(f"<{nparts}i", body[44:44 + 4 * nparts])) + [npts]
        pts = struct.unpack(f"<{2 * npts}d", body[44 + 4 * nparts:44 + 4 * nparts + 16 * npts])
        shapes.append([[(pts[2 * k], pts[2 * k + 1]) for k in range(parts[i], parts[i + 1])] for i in range(nparts)])
    return list(zip(attrs, shapes))


def inside(x, y, rings):
    """짝홀 규칙(구멍 고리 포함)."""
    hit = False
    for ring in rings:
        for (x1, y1), (x2, y2) in zip(ring, ring[1:] + ring[:1]):
            if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
                hit = not hit
    return hit


if __name__ == "__main__":
    for proj in (UTMK, CENTRAL):
        x, y = to_tm(37.5345, 126.9944, proj)
        lat, lng = from_tm(x, y, proj)
        assert abs(lat - 37.5345) < 1e-8 and abs(lng - 126.9944) < 1e-8
    # UTM-K 원점(127.5E, 38N)은 정확히 가산값
    assert all(abs(a - b) < 1e-6 for a, b in zip(to_tm(38.0, 127.5, UTMK), (1_000_000, 2_000_000)))
    assert grid_origin("다사52255325") == (952_250, 1_953_250)
    assert inside(1, 1, [[(0, 0), (2, 0), (2, 2), (0, 2)]]) and not inside(3, 1, [[(0, 0), (2, 0), (2, 2), (0, 2)]])
    print("이태원역 EPSG:5181", [round(v) for v in to_tm(37.5345, 126.9944, CENTRAL)])
    print("이태원역 UTM-K", [round(v) for v in to_tm(37.5345, 126.9944, UTMK)])
    print("ok")
