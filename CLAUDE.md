# Alley Glow (앨리 글로우)

개인 토이 프로젝트. 2026 서울 야간경제 활성화 해커톤(AIFactory 9320) 출품을 준비하다 참가하지 않게 되어(11/27) 개인 프로젝트로 이어 간다. 상권 골목 야간 청소 인력이 가게 광고를 달고 다니며 손님을 안내한다. 비용은 가게 광고비와 서울시 수거량 보상으로 나눈다. 이태원 5개 가게 시범 운영을 가정한다.

## 백엔드
- Supabase(PostgreSQL + PostgREST)를 쓴다.
- `public.zones(grid_id text pk, name text, score real 0~1, night_pop·weekday_pop·bin_dist_m int, night_sales bigint)`: RLS로 anon/authenticated 읽기만 허용. 이태원역 중심 6×6칸(250m)의 공공데이터 추천도(아래 '공공데이터 분석'). id `G00`~`G35`는 북→남, 서→동 순이라 `grid_id` 정렬이 곧 지도 배치다. 이름은 칸을 덮는 상권 이름(없으면 행정동) + 번호. 지표 열은 구역 카드용(금·토·평일 19~23시 인구, 가장 가까운 휴지통 거리, 21~24시 매출 분기 평균 원). 시간대별 열 `score_by_hour real[4]`, `night_pop_by_hour`·`weekday_pop_by_hour int[4]`(19~20 … 22~23시, 마이그레이션 `zones_hourly_scores`)은 지도 시간대 칩용. 칸 중심 좌표 `lat`·`lng`(마이그레이션 `shift_zone_share`, 경계는 중심 ±125m).
- `public.ad_slots(id, shop_name, start_hour, end_hour, placement, shop_id → shops)`: 근무 중 광고 슬롯(`shop_id`는 마이그레이션 `ad_slots_shop_ad_minutes`, 예시 행은 같은 이름의 가게에 연결). 자정 이후 시각은 24~30. 읽기 전용 RLS, 전 구역 공통. 지금은 `[가게명 N]` 예시 5행(19~24시)이다.
- `public.profiles(user_id uuid pk → auth.users, role 'worker'|'shop', display_name, shop_id → shops)`: 본인 행만 읽기(`auth.uid()`). 상인만 shop_id가 있다(check 제약). 데모 상인은 `NC-SHOP-001` 가게. 계정은 관리자가 대시보드에서 만들고 역할을 이 테이블에 넣는다. 공개 가입은 꺼 두었다. 데모 계정은 워커 1, 상인 1(이메일은 `CLAUDE.local.md`).
- `public.shifts(id uuid pk, worker_id 기본값 auth.uid(), zone_id → zones, started_at, ended_at null 가능)`, `public.collections(shift_id pk → shifts, bags_by_hour smallint[4], weighed, general_kg·recycle_kg·butts_g(weighed=false면 모두 null, check 제약), note, photo_paths text[] 0~3, before_photo_paths·after_photo_paths text[] 각 0~2)`: 근무 종료 때 저장. 워커 역할인 본인만 쓰고 읽는다. 근무 id는 앱이 근무 시작 때 만든 UUID이고, 저장은 `Prefer: resolution=ignore-duplicates`라 재시도해도 중복되지 않는다. 값 상한(봉투 50, kg 500, g 100000)은 DB check와 앱 검증이 같다. 근무 행은 첫 체크인 때 종료 시각 없이 생기고, 근무 종료 때 upsert(`merge-duplicates`)로 종료 시각을 채운다.
- Storage `collection-photos`(비공개, JPEG 5MB): 근무 종료 사진(채운 봉투, 저울을 썼으면 눈금도 함께). 경로 `{user_id}/{shift_id}/photo-{n}.jpg`, 청소 전후는 `before-{n}.jpg`·`after-{n}.jpg`, 워커 본인 폴더만 쓰기·읽기·덮어쓰기. 상인은 자기 가게에 체크인한 근무의 청소 전후 사진만 읽는다(봉투·저울 사진 제외, 판정은 API에 노출되지 않는 `private.shop_can_read_photo`). 앱은 `/storage/v1/object/authenticated/...`로 로그인 토큰을 실어 내려받는다. 근무 종료 때 EXIF 방향을 반영해(`image/decodeUpright`, 썸네일도 같이 씀) 긴 변 1600px JPEG로 줄이고 `x-upsert`로 올리므로 재시도해도 같은 파일. 앱은 1장 이상을 요구한다. 시연용 예시 행은 봉투·저울 사진 없이 청소 전·후 각 1장(`before-1.jpg`·`after-1.jpg`, "시연용 예시" 표기 그림, 실사 아님)만 있다. Storage 파일은 SQL이 아니라 대시보드나 Storage API로 지운다.
- 상인 리포트의 "치운 쓰레기"는 들른 근무들의 채운 봉투 합계(`collected_bags`)다. 무게가 선택 입력이라 kg 합계(`collected_kg`, 저울로 잰 행만)는 앱이 쓰지 않고, v1.0 APK가 필수로 읽어 함수에만 남겨 둔다. 앱은 꽁초 칸을 두지 않아 저울을 쓴 근무도 `butts_g`는 0으로 저장한다.
- `public.shops(id, name, lat, lng, qr_code unique, intro, menu, benefit)`: anon/authenticated 읽기. 지금은 이태원역 주변 예시 5곳(`NC-SHOP-001`~`005`), 소개·메뉴·혜택은 `[대괄호]` 자리표시. 가게 QR 값은 QR 랜딩 주소(`…/?s=NC-SHOP-001`)이고, 앱은 `s` 값(그 밖의 URL이면 마지막 경로)을 가게 코드로 쓴다.
- `public.qr_scans(id uuid, shop_id, scanned_at, score 1~5, client_id)`: 시민의 QR 랜딩 방문과 청결도 평가. `client_id`는 랜딩 웹이 브라우저 `localStorage`에 만든 무작위 id(개인정보 아님)로, `qr_visit(p_code, p_client)`는 같은 기기가 같은 가게를 30분 안에 다시 열면 이전 스캔을 돌려줘 새로고침으로 스캔 수·평가가 늘지 않는다(마이그레이션 `qr_visit_client_dedupe`, 저장소를 지우거나 시크릿 창이면 피할 수 있는 수준). 익명은 테이블에 접근하지 못하고 `rpc/qr_visit`(가게 정보 + 스캔 기록, 모르는 코드면 null)과 `rpc/qr_rate`(스캔당 1번, 하루 안)만 쓴다.
- `public.checkins(shift_id → shifts, shop_id → shops, checked_at, lat, lng, distance_m 0~50, unique(shift_id, shop_id))`: 직접 쓰기 권한 없음. `rpc/check_in`(security definer)만 쓴다. 이 함수가 워커 확인 → 근무 행 보장 → QR로 가게 조회 → 하버사인 거리 50m 이하면 기록까지 한다. 모르는 QR이면 빈 결과.
- `public.track_points(shift_id → shifts, recorded_at, lat, lng, accuracy_m)`: 근무 중 GPS. 워커 본인 근무에만 쓰고 읽는다. 앱의 `data/tracking/LocationTrackingService`(위치 타입 포그라운드 서비스)가 30초마다 받아 5개씩(약 2분 30초마다) 올리고, 올리기 전에 근무 행이 없으면 종료 시각 없이 만든다(ignore-duplicates). 근무 화면의 ViewModel이 사라질 때(근무 종료 저장 또는 화면 이탈) 멈춘다.
- `public.guest_visits(shop_id, created_at)`: 광고 보고 온 손님 +1(캠페인 문구 "클린 워커 보고 왔어요", 랜딩 웹과 같은 문구). 상인 본인 가게만 쓰고 읽는다.
- `rpc/shift_zone_share(p_shift_id)`(GET, security invoker): 근무의 GPS 기록 수와 배정 구역 칸(중심 ±125m 사각형) 안에 든 수. RLS 그대로라 워커 본인 근무만. 실증 지표 '부정 의심 비율'을 서버에서 집계할 때 쓰려고 남겨 둔 것이고, 앱은 부르지 않는다(앱은 받아 온 경로로 `ShiftTrack.zoneShare()`가 같은 규칙으로 셈).
- `rpc/shop_report`(GET, security definer): 호출한 상인 가게의 이번 달 집계 JSON(청소한 밤, 들른 근무의 채운 봉투 수(와 v1.0용 kg), 손님 수, QR 스캔 수, 청결도 평균, 광고 노출 시간 `ad_minutes`(이번 달 끝난 근무마다 근무 시간과 이 가게 슬롯이 겹친 분의 합, 근무자 기준이라 2인 1조면 2배), 최근 5밤과 밤마다 청소 전·후 사진 경로 1장씩). 서울 시각 기준이고 밤 날짜는 '서울 시각 − 6시간'. 상인이 아니면 null. 상인에게 워커 테이블을 열지 않으려고 함수로 둔다.
- 시연 가이드(동선·대본, 사전 SQL: 예시 데이터 날짜 갱신과 데모 가게를 시연 장소로 옮기기, 시작 전 체크리스트)는 `docs/demo.md`.
- 시연용 예시 데이터: 금·토 두 주(처음 넣은 날짜는 11/13·14·20·21, 리포트가 이번 달만 세므로 `docs/demo.md`의 SQL로 날짜를 최근 주말 기준으로 갱신한다) 19~23시 근무 4건, 수거량 4건(특이사항 `[시연용 예시]`), 체크인 10건, 데모 상인 가게 손님 7건, 같은 네 밤 데모 상인 가게 QR 스캔 23건(평가 9건, 평균 4.2점), 근무마다 청소 전·후 예시 그림 1장씩. 근무 id와 QR 스캔 id는 `00000000-0000-4000-8000-` 접두, 손님 id는 6~12. 실제 데이터와 섞이지 않게 실측 데이터를 넣기 전에 지운다: `delete from public.shifts where id::text like '00000000-0000-4000-8000-%';`(수거량·체크인은 cascade) 그리고 `delete from public.guest_visits where id between 6 and 12;`, `delete from public.qr_scans where id::text like '00000000-0000-4000-8000-%';`
- 쓰기가 필요한 테이블은 `authenticated` + `auth.uid()` 기준 RLS로 연다. anon 쓰기는 열지 않는다(시민 QR 웹 제외).
- 접속 정보는 `local.properties`의 `supabase.url`, `supabase.key`(publishable key)에서 BuildConfig로 들어간다. git에 올리지 않는다.
- 무료 플랜이라 7일간 요청이 없으면 프로젝트가 일시 정지된다(앱·랜딩 웹 모두 멈춤). `.github/workflows/keep-alive.yml`이 사흘마다 `zones` 1행을 읽어 막는다(Secrets는 Pages 배포와 같은 것). 공개 저장소에 60일간 커밋이 없으면 GitHub가 이 예약 실행을 끄므로 Actions 탭에서 다시 켠다. 이미 멈췄으면 대시보드에서 Restore.

## 앱 구조와 규칙
- Kotlin, Compose, AGP 9.5.3, minSdk 24 / target·compileSdk 37.
- 라이브러리: Hilt + KSP, Play 서비스(code-scanner 16.1.0, location 21.4.0, kotlinx-coroutines-play-services), navigation-compose(type-safe `AppRoute`), Ktor Client 3.5.2(OkHttp), kotlinx-serialization, supabase-kt `auth-kt` 3.6.0(3.7 이상은 Kotlin 2.4 필요).
- 계층: `data/remote`(DTO는 `@SerialName` snake_case, DataSource) → `data/repository`(Kotlin `Result`, 공통 `result { }`로 감싸고 `CancellationException`은 다시 던짐) → `domain`(model, repository 인터페이스). UseCase는 두지 않는다.
- 화면: `XxxRoute`(상태 보유, `hiltViewModel()`) + `XxxScreen`(상태 없음, Preview). 상태는 `StateFlow<XxxUiState>`(sealed interface).
- `HttpClient`는 `di/NetworkModule`의 싱글턴 하나를 쓴다(baseUrl `/rest/v1/`, apikey 헤더, `expectSuccess`). 로그인 중이면 사용자 토큰을 Bearer로 붙인다. 인증(세션 저장·갱신)만 `SupabaseClient`가 맡는다.
- 테마는 다크 하나(`AlleyGlowTheme`, 다이내믹 컬러 없음). 상태바와 내비바는 `SystemBarStyle.dark`로 둔다.
- 디자인 토큰: 글꼴 Pretendard(`res/font`, Regular·Bold·Black만, OFL 사본 `res/raw/pretendard_license.txt`), 글자 크기는 `ui/theme/Type.kt`의 단계 이름(`headlineSmall` 화면 제목, `titleLarge` 카드 제목·큰 숫자, `displayMedium` 타이머 등)으로만, 간격은 `Spacing`(4·8·12·16·24), 모서리는 `MaterialTheme.shapes`(12·20·24), 주 버튼 높이 `TouchHeight` 56dp. 정보 카드 바탕은 `Modifier.cardSurface()`(안쪽 여백은 카드마다).
- 역할 첫 화면(지도, 상인 리포트)은 `ui/common/AppTopBar`(서비스 이름 + 계정 아이콘 → 이름·역할·로그아웃 메뉴)를 쓴다.
- 연두(primary)는 "지금 누를 것"(주 버튼, +1)과 작은 라벨에만 쓴다. 정보 카드에 연두 테두리·연두 바탕을 쓰지 않는다. 숫자와 그 숫자를 바꾸는 버튼은 한 카드에 둔다. 아이콘은 `res/drawable/ic_*.xml`(Material Icons 경로, 라이브러리 없이).
- 화면 문구는 `res/values/strings.xml`(`stringResource`, 서비스는 `getString`)에 둔다. 제목·버튼은 짧은 명사형, 안내는 한 문장. ViewModel은 문구 대신 오류 종류(enum·object)를 넘기고 화면이 리소스로 바꾼다. 단위가 붙는 숫자 포맷(`formatWon`·`formatDistance` 등, 테스트 있음)만 코드에 둔다.
- 사용자에게 보이는 오류 문구는 한글 고정 문구를 쓴다. 예외 원문은 보여주지 않는다. 화면 안 오류·실패 안내는 `ui/common/InlineNotice`(본문색 글자 + 주황 경고 아이콘, 남색 줄)로 보인다. 불러오기 실패는 다시 시도 버튼을 붙인 `LoadFailedNotice`.
- 밤 시간대 칸(19~20 … 22~23시)은 `domain/model/NightSlot.kt`의 `NIGHT_SLOT_HOURS` 하나를 봉투 기록·근무 종료 폼·구역 지도 칩이 함께 쓴다(봉투 칸 이름은 `slot_hours` "19–20시", 지도 칩은 시작 시각만 `zonemap_slot` "19시").
- 과한 설계를 피한다. 임시 처리에는 `// shortcut: <한계>, <바꿀 시점>` 주석을 단다.

## 현재 상태 (MVP 화면 시안 기준)
시안은 보드 6개다(링크는 `CLAUDE.local.md`). 화면을 구현하거나 바꾸면 이 표와 README의 주요 화면 표를 함께 고친다.

| 시안 보드 | 앱 화면 | 구현됨 | 남음 |
|---|---|---|---|
| (시안 없음) 로그인 | Login | 이메일 로그인, 세션 복원(서버에 닿지 못하면 입력 폼 대신 다시 시도 안내, 세션은 유지), 역할별 첫 화면(워커 → 지도, 상인 → 리포트), 로그아웃 | - |
| 1. 구역 추천 지도 | ZoneMap | 상단바(계정 아이콘 → 이름(profiles.display_name, 데모 워커는 "이태원 1조")·역할·로그아웃), 36칸 격자 카드(색은 실제 점수 폭으로 펼침, 이태원역 핀(`G20` 고정)은 칸 정중앙, 상위 3칸 순위 숫자는 오른쪽 위, 범례는 칸 모양 견본 5단계, 출처 한 줄 → 누르면 전체), 1위 구역 미리 선택, 구역 카드(추천 순위, 추천 지수 0~100과 ⓘ 누르면 계산 설명, 지표 3칸: 밤 인구와 평일 대비, 가까운 휴지통, 심야 매출) 안의 근무 시작, 시간대 칩(평균 / 19시 … 22시, 칩 이름은 시작 시각만이라 다섯 개가 한 줄에 들어감, 처음엔 지금 시각의 칸·19~23시 밖이면 평균과 그 시간 1위 구역, 바꾸면 색·순위·추천 지수·밤 인구가 그 시간 값으로(밤 인구 칸 이름에 '· 21시'처럼 시각 표시), 고른 구역은 유지, 시간대 값이 없으면 칩 숨김) | - |
| 2. 근무 중 | Shift | 상태 칩(연두 점, GPS 기록 중 표시)·긴급 연락 칩(112·119 전화 앱 열기), 근무 시간 타이머와 시작 시각·구역 이름(지도에서 넘김), 위치 기록 꺼짐 배너 → 동의 → GPS 기록(알림), 같은 높이 카드 2개: 체크인 수 + QR 체크인(화면 복귀 때 갱신, 체크인이 있으면 아래에 '이번 근무에 체크인한 가게' 카드: 가게 이름·시각, `checkins`에 `shops(name)` 임베드) / 봉투 수 + −1·+1(누른 시각의 시간대로, 근무 종료 폼에 넘김), 광고 카드(지금 광고 연두 라벨 + 다음 광고 한 줄, ad_slots), 청소 전 사진(선택 0~2장), 하단 고정 근무 종료(연두), 뒤로 가기는 '근무를 그만둘까요?' 확인(계속 근무 / 그만두기) 뒤에만 지도로 | 긴급 연락에 운영팀 번호 |
| 3. 가게 체크인 | Checkin | 상단 뒤로 가기 바(`ui/common/SubTopBar`), 들어오면 스캐너 바로 열기(Google Code Scanner, 스캐너에서 뒤로 가기는 오류가 아니라 근무 화면으로: 이 기기는 취소를 `MlKitException` 13으로 알림), 정확한 위치 권한, 서버 50m 판정, 결과는 모두 같은 틀(중앙보다 조금 위, 큰 원 아이콘 + 제목 + 한 줄 설명: 완료는 연두 원·"체크인 완료" 라벨·가게명 제목·이번 근무 몇 번째 체크인, 거리 초과·모르는 QR·오류는 주황), 성공 뒤 주 버튼은 근무 화면으로·실패 뒤는 다시 스캔(보조 버튼은 회색 글자) | - |
| 4. 근무 종료·수거량 입력 | Collection | 상단 뒤로 가기 바, 근무 요약 카드(근무 시간, 시급제 예상 수당), 입력 카드(`ui/common/SectionCard`, 필수/선택 칩): 시간대별 봉투 수(근무 중 기록이 기본값, 합계)·채운 봉투 사진(필수 1~3장)·청소 후 사진(선택 0~2장)·무게(선택, 일반·재활용 kg 칸, 하나라도 적으면 저울 사용·빈 칸은 0, 둘 다 비우면 저울 없음)·특이사항, 하단 고정 근무 종료하기, shifts·collections·Storage 저장, GPS를 기록한 근무는 요약 카드에 '구역 안 기록' 비율(경로에서 계산, 판정 기준 `CELL_HALF_M` 125m는 `domain/model/ShiftRecord.kt` 한 곳)과 그 아래 근무 경로 카드(`ui/collection/TrackCard`: 배정 칸을 가운데 둔 3×3칸에 GPS 기록을 순서대로 이은 선, 지도 SDK 없이 Canvas, 범위 밖 기록은 개수만), 입력 중인 폼은 저장 상태(`SavedStateHandle`)에 보관(카메라를 띄운 사이 프로세스가 재생성돼도 유지), 봉투 0장이면 다른 검증 통과 뒤 확인 대화상자(그대로 저장 / 봉투 수 고치기) | - |
| 5. 상인 리포트 | ShopReport | 상단바(계정 메뉴에 가게 이름), 이번 달 지표 2×2 같은 높이 칸(청소한 밤, 치운 쓰레기(들른 근무의 채운 봉투 수), QR 스캔 수, 거리 청결도(랜딩 평가 평균)), 광고 노출 시간(한 줄 전체 칸), 손님 카드("광고 보고 온 손님", 남색 카드 + 연두 +1), 최근 청소 기록 한 카드(구분선, 밤마다 날짜·시각·체크인 수와 청소 전·후 썸네일, 누르면 그 밤의 전·후 사진을 전체 화면에 위아래로 크게, 사진 칸은 사진 비율대로), 화면 복귀 때 새로 받기 | - |
| 6. QR 랜딩 웹(시민) | `web/index.html` | 앱과 같은 디자인(상단 "Alley Glow" + 연두 점 상태 칩 "이 골목은 지금 청소 중", 가게 카드, 혜택 카드: 연두 라벨 "오늘의 혜택" + 혜택 + 캠페인 문구, 연두 길찾기 버튼, 평가 카드 1~5 → 감사 표시, 오류는 주황 아이콘 안내 줄), 카카오맵 길찾기, 스캔 기록. GitHub Pages(`.github/workflows/pages.yml`) | 가게 대표 사진, 현재 위치에서의 거리 |

- 사진 촬영과 썸네일은 `ui/common/PhotoRow` 하나를 쓴다(시스템 카메라, 캐시 photos/, EXIF 방향 반영, 카메라 아이콘 + 장수 칸, 썸네일 오른쪽 위 ×로 삭제). 업로드는 근무 종료 저장 때 한 번에.
- 화면 이동: 로그인 → 지도(워커) 또는 상인 리포트(상인). 로그아웃은 스택을 비우고 로그인으로. 지도 → 근무 → 체크인(뒤로) / 근무 종료 → 지도(근무 기록은 스택에서 지움). 실제 기기에서 확인했다.

## 기획 문서에서 정한 것
출처는 검토 메모(Claude Docs, 링크는 `CLAUDE.local.md`)의 '대회 요건', '본선 Working MVP 범위', '안드로이드 설계', '데이터 모델', '공공데이터 분석 계획', '디자인 방향'.

- 대회: 해커톤에 참가하지 않아 대회 일정(기획안 12/7, 본선 12/18~19)과 요건은 더 따르지 않는다. 대회 요건이던 서울 열린데이터광장 출처 표기는 데이터 이용 조건이라 그대로 둔다.
- 범위: 앱 하나에 워커·상인 두 역할. 시민 QR 랜딩은 앱이 아닌 웹 1장. 시연 동선은 "배정 확인 → 근무 → 상인 리포트" 한 바퀴를 3분 안에.
- MVP에서 빼는 것: 회원가입·권한 관리(데모 계정 2개로 대신), 결제·정산(예상 금액 표시만), 푸시, 오프라인 동기화, 서브 프로젝트(꽁초 리어카).
- 기능별 기술(모두 구현됨, 지도만 다르게 감):
  - QR 스캔: Google Code Scanner(Play 서비스). 카메라 권한과 스캔 화면을 직접 만들지 않는다.
  - GPS 동선: FusedLocationProvider + `location` 타입 Foreground Service. 근무 중에만 수집하고 백그라운드 위치 권한은 받지 않는다. 위치 수집 동의 화면 1개.
  - 사진: `TakePicture` ActivityResult. 갤러리 업로드는 막는다. 메모는 이미지 표시에 Coil을 정했지만 라이브러리 없이 `BitmapFactory`(+ EXIF 방향 반영)로 디코딩한다.
  - 지도: 메모는 네이버 지도 SDK였지만 6×6 격자 그리드로 갔다(아래 '메모와 다르게 간 것').
- 체크인은 스캔 시점 GPS가 가게 반경 50m 안일 때만 인정한다(QR 사진 위조 방지).
- 근무 종료 화면의 출발점은 시범 운영의 '워커 근무 기록' 폼이다(시간대별 봉투 수, 일반·재활용 무게 kg, 꽁초 무게 g, 저울 사진 최대 3장, 청소 전후 사진 최대 4장, 특이사항). 실제 구현에서 바뀐 점은 아래 '메모와 다르게 간 것'(무게 선택, 꽁초 칸 없음, 서울시 보상은 워커 화면에 표시하지 않음).
- 예상 수당 계산은 순수 함수(`estimateWage`, 분 단위)로 두고 테스트한다. 원가표 가정: 2인 1조, 19~23시, 워커 시급 12,000원, 플랫폼 수수료 15%, 광고 판매 단위는 '구역 × 1시간 슬롯 × 가게 1곳'.
- 메모의 목표 데이터 모델(7개, 실제 테이블은 위 '백엔드'): `zones`(경계, 시간대별 추천도), `shops`(이름, 위치, 홍보물 이미지, QR), `shifts`(워커, 구역, 광고 가게, 시작·종료), `track_points`, `checkins`(shift, 가게, 시각, 위치), `collections`(shift, 종류, 봉투 수, kg, 저울 사진), `qr_scans`(가게, 시각).
- 추천도 = 야간 인구 0.5 + 휴지통 공백 0.3 + 야간 매출 0.2. 세 지표를 이태원 범위에서 0~1로 정규화하고, 가중치는 가정이다. 데이터는 OA-23016(생활인구 250m), OA-15069(가로휴지통), OA-15572(상권 추정매출). (실제로는 격자 단위인 OA-22784와 상권 영역 OA-15560을 썼다. 아래 '공공데이터 분석') 열린데이터 인증키는 APK에 넣지 않고, 미리 받아 Supabase에 적재한다.
- 디자인: 짙은 남색 배경 + 형광 연두 포인트 1개. 본문 글꼴 Pretendard. 모서리 16dp 이상. 장갑 낀 한 손 조작을 위해 터치 영역 56dp 이상.

**메모와 다르게 간 것**
- 서비스 이름: PRD·메모의 "야간 클린 워커", 시안의 가칭 "클린나이트", 11/22에 정한 "Night Clean"을 거쳐 **Alley Glow**(11/27 결정). 밤에 빛나는 골목, 야간경제 주제를 담는 이름. 문서에서 처음 쓸 때는 "Alley Glow(앨리 글로우)". 패키지는 `io.github.koolunkle.alleyglow`(Play 스토어가 `com.example`을 받지 않아 11/27에 바꿈). 저장소·Pages 주소도 `alley-glow`로 바꿨다(11/27, 예전 Pages 주소 `…/night-clean/`는 넘겨주지 않아 그 주소로 인쇄한 QR은 더 열리지 않음). 이전 이름이 남은 곳: 가게 코드 `NC-SHOP-*`(DB 값), 서명 키 별칭 `nightclean`(키스토어 안 이름), v1.3까지의 Release 파일 `night-clean.apk`, 로컬 작업 폴더 이름.
- Koin 대신 Hilt. Domain 생략 대신 `domain`(model, repository 인터페이스). DTO = 모델 대신 DTO 분리. 위 '앱 구조와 규칙'이 기준이다.
- 지도는 네이버 지도 SDK 대신 6×6 격자 그리드. 격자 경계 데이터가 들어오면 바꾼다.
- 청소 전후 사진: 메모의 근무 기록 폼은 필수였지만, 시작 때 잊으면 근무를 마칠 수 없어 선택(각 최대 2장)으로 두고 '같은 자리, 같은 각도'를 안내한다.
- 워커 수당: 수거량·꽁초 보상 없이 정해진 시급제(11/22 결정). 근무 종료 화면은 근무 시간 × 시급만 보여 준다. 서울시 수거량 보상은 플랫폼 비용 분담 쪽 이야기라 워커 화면에 두지 않는다.
- 근무 종료 무게: 메모는 휴대용 저울 계량과 저울 사진을 필수로 했지만, 저울이 없을 수 있고 수당과도 무관해 봉투 수와 봉투 사진을 필수로, 무게는 선택 입력(일반·재활용 kg, 비우면 저울 없음)으로 바꿨다. 꽁초 무게 칸과 봉투 수 기반 추정 무게 표시는 없앴다.
- `ad_slots`는 메모 모델에 없는 임시 테이블이다. 가게(`shop_id`)와는 연결했지만 구역 구분은 없다(전 구역 공통). 구역별 슬롯이 생기면 대체한다.

## 공공데이터 분석
- 스크립트 `analysis/`(파이썬 표준 라이브러리만): `geo.py`(GRS80 횡메르카토르 변환, xlsx·shapefile 읽기, `python analysis/geo.py`로 자체 점검), `geocode_bins.py`(휴지통 주소 → 좌표, 카카오 로컬 API, 키는 `local.properties`의 `kakao.key`), `zones.py`(추천도 → `data/zones_itaewon.csv`, 시간대별 36칸 인구 합계 → `data/pop_hourly_itaewon.csv`), `figures.py`(분석 그림 3장 → `docs/figures/`, Pillow + 맑은 고딕, 밝은 배경, 출처 문구 포함).
- 원본은 `data/raw/`(git 제외: 생활인구 원본 재배포 금지, 카카오 변환 결과 저장 약관 불확실). 칸별 합계인 `data/zones_itaewon.csv`만 올린다.
- 원본: 서울 생활인구 250m 격자(OA-22784, 일별 `250_LOCAL_RESD_YYYYMMDD`, CP949, 격자 id는 국가지점번호라 경계 파일 없이 UTM-K 좌표 계산), 가로쓰레기통 설치정보(OA-15069, 202511, 주소만), 상권 추정매출(OA-15572, 2025년), 상권 영역(OA-15560, shapefile, EPSG:5181).
- 시간대별 추천도(`score_19`~`score_22`): 야간 인구만 그 한 시간의 금·토 평균으로 바꿔 같은 식·같은 정규화로 계산(휴지통·매출은 시간 구분 없음). 값은 CSV에서 SQL로 `zones`에 넣는다.
- 지표: 야간 인구 = 금·토(11/13·14) 19~23시 평균, 휴지통 공백 = 칸 중심에서 가장 가까운 휴지통까지 거리, 야간 매출 = 21~24시 매출(분기 평균)을 상권 영역과 겹친 면적 비율로 나눔. 36칸 안에서 최소-최대 정규화.
- 용산구 휴지통 359개 중 354개 변환(실패 5개는 한강로·청파동, 분석 범위 밖), 범위 안 95개. 이웃 구 휴지통은 넣지 않았다(36칸 모두 용산구).
- 휴지통 공백이 가산이라 인구가 거의 없는 칸(용산공원 G30 등)도 점수를 받는다. 가중치와 함께 가정으로 밝힌다.
- 출처 표기: "서울특별시 열린데이터광장(서울 생활인구, 가로쓰레기통 설치정보, 상권분석서비스), 서울특별시·KT".

## QR 랜딩 웹
- 정적 HTML 1장(`web/index.html`), 라이브러리 없음(글꼴만 jsDelivr의 Pretendard 1.3.9 CSS, 못 받으면 시스템 글꼴). 가게 정보는 `textContent`로만 넣는다. 한글은 `word-break: keep-all`.
- 화면 확인은 DB에 스캔이 남지 않게 가짜 `config.js`(fetch를 가로채 예시 가게를 돌려줌)를 둔 사본으로 한다. 헤드리스 Chrome은 창이 500px보다 좁아지지 않으니 390px iframe에 넣어 캡처한다.
- Supabase 접속 정보는 `web/config.js`(git 제외, 예시는 `config.example.js`). 배포는 `main`에 `web/`가 바뀌면 GitHub Actions가 Secrets(`SUPABASE_URL`, `SUPABASE_KEY`)로 `config.js`를 만들어 Pages에 올린다. 워크플로는 build(업로드)와 deploy(배포) job으로 나눠 둔다. 한 job에서 이어 하면 방금 올린 아티팩트를 API가 못 찾아 실패할 때가 있었다.
- 로컬 확인: `web/config.js`를 만든 뒤 `web/index.html?s=NC-SHOP-001`을 브라우저로 연다.
- 인쇄용 QR: `web/print.html`(배포 주소의 `print.html`). shops 전체를 읽어 가게마다 랜딩 주소 QR(한 변 10cm, 오류 정정 Q)과 캠페인 문구 카드를 A4에 2장씩. QR 생성은 cdnjs의 qrcodejs 1.0.0.
- 저장소는 Pages 배포를 위해 공개한다. 커밋 작성자 이메일은 GitHub noreply 주소를 쓴다(저장소 로컬 `user.email`).

## 빌드·실행
- `./gradlew.bat assembleDebug testDebugUnitTest`
- 기기 설치: `./gradlew.bat installDebug`
- 배포 APK: `./gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk`. 서명은 `local.properties`의 `release.storeFile`·`release.storePassword`(별칭 `nightclean`, 키스토어는 저장소 밖 `~/.android/`, 분실하면 같은 앱으로 업데이트 불가). R8은 꺼 둠(켜면 시작 즉시 `IllegalAccessError`, 다시 켤 때 `src/main/keepRules/`에 keep 규칙을 둔다). GitHub Release에 `alley-glow.apk` 이름으로 올리면 README 링크(`releases/latest/download/alley-glow.apk`)가 최신을 가리킨다. debug와 서명이 달라 기기에서 바꿔 설치하려면 먼저 지워야 한다(로그인 풀림).
- Release는 기기에서 확인한 기능이 몇 개 모였을 때나 바로 필요한 버그 수정이 있을 때만 올린다(받는 사람이 사실상 본인뿐이고 빌드가 오래 걸림). 평소에는 커밋·푸시만 하고 기기 확인은 직접 설치로 한다. 리팩토링만 있으면 올리지 않는다. 올릴 때는 버전 커밋(`feat: 앱 버전 x.y(versionCode n)`) → clean 빌드·테스트(오래 걸리면 백그라운드) → 푸시 → `gh release create vX.Y`에 `alley-glow.apk` → README 링크로 받아 버전 확인.
- 실행: `adb shell am start -n io.github.koolunkle.alleyglow/.MainActivity`
- 개인 링크, Supabase 프로젝트 id, 기기 정보는 `CLAUDE.local.md`(git 제외)에 둔다.
