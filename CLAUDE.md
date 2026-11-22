# 야간 클린 워커 (NightClean)

2026 서울 야간경제 활성화 해커톤(AIFactory 9320) 출품작. 상권 골목 야간 청소 인력이 가게 광고를 달고 다니며 손님을 안내한다. 비용은 가게 광고비와 서울시 수거량 보상으로 나눈다. 시범 운영은 이태원 5개 가게.

## 백엔드
- Supabase(PostgreSQL + PostgREST)를 쓴다.
- `public.zones(grid_id text pk, name text, score real 0~1, night_pop·weekday_pop·bin_dist_m int, night_sales bigint)`: RLS로 anon/authenticated 읽기만 허용. 이태원역 중심 6×6칸(250m)의 공공데이터 추천도(아래 '공공데이터 분석'). id `G00`~`G35`는 북→남, 서→동 순이라 `grid_id` 정렬이 곧 지도 배치다. 이름은 칸을 덮는 상권 이름(없으면 행정동) + 번호. 지표 열은 구역 카드용(금·토·평일 19~23시 인구, 가장 가까운 휴지통 거리, 21~24시 매출 분기 평균 원).
- `public.ad_slots(id, shop_name, start_hour, end_hour, placement)`: 근무 중 광고 슬롯. 자정 이후 시각은 24~30. 읽기 전용 RLS, 전 구역 공통. 지금은 `[가게명 N]` 예시 5행(19~24시)이다.
- `public.profiles(user_id uuid pk → auth.users, role 'worker'|'shop', display_name, shop_id → shops)`: 본인 행만 읽기(`auth.uid()`). 상인만 shop_id가 있다(check 제약). 데모 상인은 `NC-SHOP-001` 가게. 계정은 관리자가 대시보드에서 만들고 역할을 이 테이블에 넣는다. 공개 가입은 꺼 두었다. 데모 계정은 워커 1, 상인 1(이메일은 `CLAUDE.local.md`).
- `public.shifts(id uuid pk, worker_id 기본값 auth.uid(), zone_id → zones, started_at, ended_at null 가능)`, `public.collections(shift_id pk → shifts, bags_by_hour smallint[4], weighed, general_kg·recycle_kg·butts_g(weighed=false면 모두 null, check 제약), note, photo_paths text[] 0~3, before_photo_paths·after_photo_paths text[] 각 0~2)`: 근무 종료 때 저장. 워커 역할인 본인만 쓰고 읽는다. 근무 id는 앱이 근무 시작 때 만든 UUID이고, 저장은 `Prefer: resolution=ignore-duplicates`라 재시도해도 중복되지 않는다. 값 상한(봉투 50, kg 500, g 100000)은 DB check와 앱 검증이 같다. 근무 행은 첫 체크인 때 종료 시각 없이 생기고, 근무 종료 때 upsert(`merge-duplicates`)로 종료 시각을 채운다.
- Storage `collection-photos`(비공개, JPEG 5MB): 근무 종료 사진(저울을 쓰면 저울 눈금, 아니면 채운 봉투). 경로 `{user_id}/{shift_id}/photo-{n}.jpg`, 청소 전후는 `before-{n}.jpg`·`after-{n}.jpg`, 워커 본인 폴더만 쓰기·읽기·덮어쓰기. 상인은 자기 가게에 체크인한 근무의 청소 전후 사진만 읽는다(봉투·저울 사진 제외, 판정은 API에 노출되지 않는 `private.shop_can_read_photo`). 앱은 `/storage/v1/object/authenticated/...`로 로그인 토큰을 실어 내려받는다. 근무 종료 때 EXIF 방향을 반영해(`image/decodeUpright`, 썸네일도 같이 씀) 긴 변 1600px JPEG로 줄이고 `x-upsert`로 올리므로 재시도해도 같은 파일. 앱은 1장 이상을 요구하고, 시연용 예시 행은 비어 있다. Storage 파일은 SQL이 아니라 대시보드나 Storage API로 지운다.
- 상인 리포트의 kg은 저울로 잰 행만 더한다(null은 sum에서 빠짐). 저울 없는 근무의 추정 무게는 저장하지 않는다.
- `public.shops(id, name, lat, lng, qr_code unique, intro, menu, benefit)`: anon/authenticated 읽기. 지금은 이태원역 주변 예시 5곳(`NC-SHOP-001`~`005`), 소개·메뉴·혜택은 `[대괄호]` 자리표시. 가게 QR 값은 QR 랜딩 주소(`…/?s=NC-SHOP-001`)이고, 앱은 `s` 값(그 밖의 URL이면 마지막 경로)을 가게 코드로 쓴다.
- `public.qr_scans(id uuid, shop_id, scanned_at, score 1~5)`: 시민의 QR 랜딩 방문과 청결도 평가. 익명은 테이블에 접근하지 못하고 `rpc/qr_visit`(가게 정보 + 스캔 기록, 모르는 코드면 null)과 `rpc/qr_rate`(스캔당 1번, 하루 안)만 쓴다.
- `public.checkins(shift_id → shifts, shop_id → shops, checked_at, lat, lng, distance_m 0~50, unique(shift_id, shop_id))`: 직접 쓰기 권한 없음. `rpc/check_in`(security definer)만 쓴다. 이 함수가 워커 확인 → 근무 행 보장 → QR로 가게 조회 → 하버사인 거리 50m 이하면 기록까지 한다. 모르는 QR이면 빈 결과.
- `public.track_points(shift_id → shifts, recorded_at, lat, lng, accuracy_m)`: 근무 중 GPS. 워커 본인 근무에만 쓰고 읽는다. 앱의 `data/tracking/LocationTrackingService`(위치 타입 포그라운드 서비스)가 30초마다 받아 5개씩(약 2분 30초마다) 올리고, 올리기 전에 근무 행이 없으면 종료 시각 없이 만든다(ignore-duplicates). 근무 화면의 ViewModel이 사라질 때(근무 종료 저장 또는 화면 이탈) 멈춘다.
- `public.guest_visits(shop_id, created_at)`: "클린 보고 왔어요" 손님 +1. 상인 본인 가게만 쓰고 읽는다.
- `rpc/shop_report`(GET, security definer): 호출한 상인 가게의 이번 달 집계 JSON(청소한 밤, 들른 근무의 수거량 kg, 손님 수, QR 스캔 수, 청결도 평균, 최근 5밤과 밤마다 청소 전·후 사진 경로 1장씩). 서울 시각 기준이고 밤 날짜는 '서울 시각 − 6시간'. 상인이 아니면 null. 상인에게 워커 테이블을 열지 않으려고 함수로 둔다.
- 시연용 예시 데이터: 11/13·14·20·21(금·토) 19~23시 근무 4건, 수거량 4건(특이사항 `[시연용 예시]`), 체크인 10건, 데모 상인 가게 손님 7건. 근무 id는 `00000000-0000-4000-8000-` 접두. 실제 시범 데이터와 섞이지 않게 본선 뒤나 실측 데이터를 넣기 전에 지운다: `delete from public.shifts where id::text like '00000000-0000-4000-8000-%';`(수거량·체크인은 cascade) 그리고 `delete from public.guest_visits where created_at < '2026-11-22';`
- 쓰기가 필요한 테이블은 `authenticated` + `auth.uid()` 기준 RLS로 연다. anon 쓰기는 열지 않는다(시민 QR 웹 제외).
- 접속 정보는 `local.properties`의 `supabase.url`, `supabase.key`(publishable key)에서 BuildConfig로 들어간다. git에 올리지 않는다.

## 앱 구조와 규칙
- Kotlin, Compose, AGP 9.5.2, minSdk 24 / target·compileSdk 37.
- 라이브러리: Hilt + KSP, Play 서비스(code-scanner 16.1.0, location 21.4.0, kotlinx-coroutines-play-services), navigation-compose(type-safe `AppRoute`), Ktor Client 3.5.2(OkHttp), kotlinx-serialization, supabase-kt `auth-kt` 3.6.0(3.7 이상은 Kotlin 2.4 필요).
- 계층: `data/remote`(DTO는 `@SerialName` snake_case, DataSource) → `data/repository`(Kotlin `Result`, `CancellationException`은 다시 던짐) → `domain`(model, repository 인터페이스). UseCase는 두지 않는다.
- 화면: `XxxRoute`(상태 보유, `hiltViewModel()`) + `XxxScreen`(상태 없음, Preview). 상태는 `StateFlow<XxxUiState>`(sealed interface).
- `HttpClient`는 `di/NetworkModule`의 싱글턴 하나를 쓴다(baseUrl `/rest/v1/`, apikey 헤더, `expectSuccess`). 로그인 중이면 사용자 토큰을 Bearer로 붙인다. 인증(세션 저장·갱신)만 `SupabaseClient`가 맡는다.
- 테마는 다크 하나(`NightCleanTheme`, 다이내믹 컬러 없음). 상태바와 내비바는 `SystemBarStyle.dark`로 둔다.
- 사용자에게 보이는 오류 문구는 한글 고정 문구를 쓴다. 예외 원문은 보여주지 않는다.
- 과한 설계를 피한다. 임시 처리에는 `// shortcut: <한계>, <바꿀 시점>` 주석을 단다.

## 현재 상태 (MVP 화면 시안 기준)
시안은 보드 6개다(링크는 `CLAUDE.local.md`). 화면을 구현하거나 바꾸면 이 표와 README의 주요 화면 표를 함께 고친다.

| 시안 보드 | 앱 화면 | 구현됨 | 남음 |
|---|---|---|---|
| (시안 없음) 로그인 | Login | 이메일 로그인, 세션 복원, 역할별 첫 화면(워커 → 지도, 상인 → 리포트), 로그아웃 | - |
| 1. 구역 추천 지도 | ZoneMap | 36칸 격자(zones, 색은 실제 점수 폭으로 펼침), 상위 3칸 순위, 이태원역 표시(`G20` 고정), 범례, 구역 카드(추천도·순위, 금·토/평일 밤 인구, 가장 가까운 휴지통, 21~24시 매출), 공공데이터 출처, 근무 시작 | 시간대 칩(시간대별 점수 필요), 워커 이름 |
| 2. 근무 중 | Shift | 근무 시간 타이머, 위치 기록 동의 → GPS 기록(상단 '근무 중 · GPS 기록 중', 알림), 현재·다음 광고 슬롯(ad_slots), 청소 전 사진(선택 0~2장), 이번 근무 체크인 수(화면 복귀 때 갱신), 채운 봉투 +1·되돌리기(누른 시각의 시간대로, 근무 종료 폼에 넘김), 긴급 연락(112·119 전화 앱 열기), 하단 고정 버튼 | 기록 경로 표시·구역 이탈 비율, 긴급 연락에 운영팀 번호 |
| 3. 가게 체크인 | Checkin | QR 스캔(Google Code Scanner), 정확한 위치 권한, 서버 50m 판정, 완료·거리 초과·모르는 QR 안내 | 이번 슬롯 체크인 목록 |
| 4. 근무 종료·수거량 입력 | Collection | 시간대별 봉투 수(근무 중 기록을 기본값으로), 저울 사용 여부(기본 '저울 없음' → 봉투 수 × 6kg 추정 표시), 저울 사용 시 종류별 무게, 사진(필수 1~3장, 시스템 카메라, EXIF 방향 반영), 청소 후 사진(선택 0~2장), 특이사항, 예상 수입(시급·꽁초 보상은 저울 사용 시만), shifts·collections·Storage 저장 | 앱 프로세스가 재생성되면 입력 중인 폼(무게·봉투 수·사진) 초기화 |
| 5. 상인 리포트 | ShopReport | 이번 달 청소한 밤, 치운 쓰레기(들른 근무 중 저울로 잰 양), QR 스캔 수, 거리 청결도(랜딩 평가 평균), 손님 +1 기록, 최근 청소 기록(밤마다 청소 전·후 썸네일), 로그아웃 | 광고 노출 시간(ad_slots가 가게·근무와 연결되면), 사진 크게 보기 |
| 6. QR 랜딩 웹(시민) | `web/index.html` | 가게 이름·소개·메뉴, 오늘의 혜택과 암호, 카카오맵 길찾기, 청결도 평가 1~5, 스캔 기록. GitHub Pages(`.github/workflows/pages.yml`) | 가게 대표 사진, 현재 위치에서의 거리 |

- 사진 촬영과 썸네일은 `ui/common/PhotoRow` 하나를 쓴다(시스템 카메라, 캐시 photos/, EXIF 방향 반영). 업로드는 근무 종료 저장 때 한 번에.
- 화면 이동: 로그인 → 지도(워커) 또는 상인 리포트(상인). 로그아웃은 스택을 비우고 로그인으로. 지도 → 근무 → 체크인(뒤로) / 근무 종료 → 지도(근무 기록은 스택에서 지움). 실제 기기에서 확인했다.

## 기획 문서에서 정한 것
출처는 검토 메모(Claude Docs, 링크는 `CLAUDE.local.md`)의 '대회 요건', '본선 Working MVP 범위', '안드로이드 설계', '데이터 모델', '공공데이터 분석 계획', '디자인 방향'.

- 대회: 예선은 기획안 PDF(12/7 마감)로만 평가한다. 앱은 본선(12/18~19 무박 2일)에서 Working MVP로 시연한다. 심사위원이 직접 쓸 수 있게 APK 배포 링크와 시연 기기를 준비한다. 서울 열린데이터광장 데이터 1종 이상과 출처 표기는 필수다.
- 범위: 앱 하나에 워커·상인 두 역할. 시민 QR 랜딩은 앱이 아닌 웹 1장. 시연 동선은 "배정 확인 → 근무 → 상인 리포트" 한 바퀴를 3분 안에.
- 본선에서 빼는 것: 회원가입·권한 관리(데모 계정 2개로 대신), 결제·정산(예상 금액 표시만), 푸시, 오프라인 동기화, 서브 프로젝트(꽁초 리어카).
- 아직 안 붙인 기능의 기술:
  - QR 스캔: Google Code Scanner(Play 서비스). 카메라 권한과 스캔 화면을 직접 만들지 않는다.
  - GPS 동선: FusedLocationProvider + `location` 타입 Foreground Service. 근무 중에만 수집하고 백그라운드 위치 권한은 받지 않는다. 위치 수집 동의 화면 1개.
  - 사진: `TakePicture` ActivityResult. 갤러리 업로드는 막는다. 이미지 표시는 Coil.
  - 지도: 네이버 지도 SDK에 250m 격자를 폴리곤으로 그린다.
- 체크인은 스캔 시점 GPS가 가게 반경 50m 안일 때만 인정한다(QR 사진 위조 방지).
- 근무 종료 화면은 시범 운영의 '워커 근무 기록' 폼 문항을 그대로 옮긴다. 시간대별 봉투 수(19~20/20~21/21~22/22~23시), 일반·재활용 무게(kg), 꽁초 무게(g), 저울 사진 최대 3장, 청소 전후 사진 최대 4장, 특이사항. 서울시 보상은 아직 없는 제도라 '예상 보상'으로만 표시한다.
- 보상·예상 수입 계산은 순수 함수로 두고 테스트한다. 원가표 가정: 2인 1조, 19~23시, 워커 시급 12,000원, 플랫폼 수수료 15%, 광고 판매 단위는 '구역 × 1시간 슬롯 × 가게 1곳'.
- 목표 데이터 모델(7개): `zones`(경계, 시간대별 추천도), `shops`(이름, 위치, 홍보물 이미지, QR), `shifts`(워커, 구역, 광고 가게, 시작·종료), `track_points`, `checkins`(shift, 가게, 시각, 위치), `collections`(shift, 종류, 봉투 수, kg, 저울 사진), `qr_scans`(가게, 시각).
- 추천도 = 야간 인구 0.5 + 휴지통 공백 0.3 + 야간 매출 0.2. 세 지표를 이태원 범위에서 0~1로 정규화하고, 가중치는 가정이다. 데이터는 OA-23016(생활인구 250m), OA-15069(가로휴지통), OA-15572(상권 추정매출). (실제로는 격자 단위인 OA-22784와 상권 영역 OA-15560을 썼다. 아래 '공공데이터 분석') 열린데이터 인증키는 APK에 넣지 않고, 미리 받아 Supabase에 적재한다.
- 디자인: 짙은 남색 배경 + 형광 연두 포인트 1개. 본문 글꼴 Pretendard. 모서리 16dp 이상. 장갑 낀 한 손 조작을 위해 터치 영역 56dp 이상.

**메모와 다르게 간 것**
- Koin 대신 Hilt. Domain 생략 대신 `domain`(model, repository 인터페이스). DTO = 모델 대신 DTO 분리. 위 '앱 구조와 규칙'이 기준이다.
- 지도는 네이버 지도 SDK 대신 6×6 격자 그리드. 격자 경계 데이터가 들어오면 바꾼다.
- 청소 전후 사진: 메모의 근무 기록 폼은 필수였지만, 시작 때 잊으면 근무를 마칠 수 없어 선택(각 최대 2장)으로 두고 '같은 자리, 같은 각도'를 안내한다.
- 근무 종료 무게: 메모는 휴대용 저울 계량과 저울 사진을 필수로 했지만, 저울이 없을 수 있고 손 입력 무게를 믿기 어려워 봉투 수와 사진을 필수로, 무게는 선택으로 바꿨다. 저울이 없으면 '봉투 수 × 봉투당 평균 무게(`KG_PER_BAG_ESTIMATE` 6kg, 가정값)'를 화면에만 보여 주고 저장하지 않는다. 평균 무게는 이태원 시범 실측(봉투 수와 kg)으로 바꾼다. 수거량 보상은 저울로 잰 무게로만 계산한다.
- `ad_slots`는 메모 모델에 없는 임시 테이블이다. 구역·가게 id 없이 가게명만 둔다. `shops`와 구역별 슬롯이 생기면 대체한다.

## 정하지 않은 것
- 사전 개발 허용 여부: 규칙은 개발을 '본선 무박 2일' 활동으로 적고, 메모는 허용될 때만 코드를 쓰기로 했다. 운영팀 답을 아직 못 받았다(11/21 기준).
- 서비스 이름: 시안은 "클린나이트", PRD·메모·앱은 "야간 클린 워커".

## 공공데이터 분석
- 스크립트 `analysis/`(파이썬 표준 라이브러리만): `geo.py`(GRS80 횡메르카토르 변환, xlsx·shapefile 읽기, `python analysis/geo.py`로 자체 점검), `geocode_bins.py`(휴지통 주소 → 좌표, 카카오 로컬 API, 키는 `local.properties`의 `kakao.key`), `zones.py`(추천도 → `data/zones_itaewon.csv`, 시간대별 36칸 인구 합계 → `data/pop_hourly_itaewon.csv`), `figures.py`(기획안용 그림 3장 → `docs/figures/`, Pillow + 맑은 고딕, 밝은 배경, 출처 문구 포함).
- 원본은 `data/raw/`(git 제외: 생활인구 원본 재배포 금지, 카카오 변환 결과 저장 약관 불확실). 칸별 합계인 `data/zones_itaewon.csv`만 올린다.
- 원본: 서울 생활인구 250m 격자(OA-22784, 일별 `250_LOCAL_RESD_YYYYMMDD`, CP949, 격자 id는 국가지점번호라 경계 파일 없이 UTM-K 좌표 계산), 가로쓰레기통 설치정보(OA-15069, 202511, 주소만), 상권 추정매출(OA-15572, 2025년), 상권 영역(OA-15560, shapefile, EPSG:5181).
- 지표: 야간 인구 = 금·토(11/13·14) 19~23시 평균, 휴지통 공백 = 칸 중심에서 가장 가까운 휴지통까지 거리, 야간 매출 = 21~24시 매출(분기 평균)을 상권 영역과 겹친 면적 비율로 나눔. 36칸 안에서 최소-최대 정규화.
- 용산구 휴지통 359개 중 354개 변환(실패 5개는 한강로·청파동, 분석 범위 밖), 범위 안 95개. 이웃 구 휴지통은 넣지 않았다(36칸 모두 용산구).
- 휴지통 공백이 가산이라 인구가 거의 없는 칸(용산공원 G30 등)도 점수를 받는다. 가중치와 함께 기획안에서 가정으로 밝힌다.
- 출처 표기: "서울특별시 열린데이터광장(서울 생활인구, 가로쓰레기통 설치정보, 상권분석서비스), 서울특별시·KT".

## QR 랜딩 웹
- 정적 HTML 1장(`web/index.html`), 라이브러리 없음. 가게 정보는 `textContent`로만 넣는다.
- Supabase 접속 정보는 `web/config.js`(git 제외, 예시는 `config.example.js`). 배포는 `main`에 `web/`가 바뀌면 GitHub Actions가 Secrets(`SUPABASE_URL`, `SUPABASE_KEY`)로 `config.js`를 만들어 Pages에 올린다.
- 로컬 확인: `web/config.js`를 만든 뒤 `web/index.html?s=NC-SHOP-001`을 브라우저로 연다.
- 인쇄용 QR: `web/print.html`(배포 주소의 `print.html`). shops 전체를 읽어 가게마다 랜딩 주소 QR(한 변 10cm, 오류 정정 Q)과 캠페인 문구 카드를 A4에 2장씩. QR 생성은 cdnjs의 qrcodejs 1.0.0.
- 저장소는 Pages 배포를 위해 공개한다. 커밋 작성자 이메일은 GitHub noreply 주소를 쓴다(저장소 로컬 `user.email`).

## 빌드·실행
- `./gradlew.bat assembleDebug testDebugUnitTest`
- 기기 설치: `./gradlew.bat installDebug`
- 실행: `adb shell am start -n com.example.nightclean/.MainActivity`
- 개인 링크, Supabase 프로젝트 id, 기기 정보는 `CLAUDE.local.md`(git 제외)에 둔다.
