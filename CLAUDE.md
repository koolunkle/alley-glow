# 야간 클린 워커 (NightClean)

2026 서울 야간경제 활성화 해커톤(AIFactory 9320) 출품작. 상권 골목 야간 청소 인력이 가게 광고를 달고 다니며 손님을 안내한다. 비용은 가게 광고비와 서울시 수거량 보상으로 나눈다. 시범 운영은 이태원 5개 가게.

## 백엔드
- Supabase(PostgreSQL + PostgREST)를 쓴다.
- `public.zones(grid_id text pk, name text, score real 0~1)`: RLS로 anon/authenticated 읽기만 허용. 지금 값은 예시 점수 36칸이고, 공공데이터 분석 결과로 갈아 끼운다.
- `public.ad_slots(id, shop_name, start_hour, end_hour, placement)`: 근무 중 광고 슬롯. 자정 이후 시각은 24~30. 읽기 전용 RLS, 전 구역 공통. 지금은 `[가게명 N]` 예시 5행(19~24시)이다.
- 접속 정보는 `local.properties`의 `supabase.url`, `supabase.key`(publishable key)에서 BuildConfig로 들어간다. git에 올리지 않는다.

## 앱 구조와 규칙
- Kotlin, Compose, AGP 9.5.2, minSdk 24 / target·compileSdk 37.
- 라이브러리: Hilt + KSP, navigation-compose(type-safe `AppRoute`), Ktor Client 3.5.2(OkHttp), kotlinx-serialization.
- 계층: `data/remote`(DTO는 `@SerialName` snake_case, DataSource) → `data/repository`(Kotlin `Result`, `CancellationException`은 다시 던짐) → `domain`(model, repository 인터페이스). UseCase는 두지 않는다.
- 화면: `XxxRoute`(상태 보유, `hiltViewModel()`) + `XxxScreen`(상태 없음, Preview). 상태는 `StateFlow<XxxUiState>`(sealed interface).
- `HttpClient`는 `di/NetworkModule`의 싱글턴 하나를 쓴다(baseUrl `/rest/v1/`, apikey 헤더, `expectSuccess`).
- 테마는 다크 하나(`NightCleanTheme`, 다이내믹 컬러 없음). 상태바와 내비바는 `SystemBarStyle.dark`로 둔다.
- 사용자에게 보이는 오류 문구는 한글 고정 문구를 쓴다. 예외 원문은 보여주지 않는다.
- 과한 설계를 피한다. 임시 처리에는 `// shortcut: <한계>, <바꿀 시점>` 주석을 단다.

## 현재 상태 (MVP 화면 시안 기준)
시안은 보드 6개다(링크는 `CLAUDE.local.md`). 화면을 구현하거나 바꾸면 이 표와 README의 주요 화면 표를 함께 고친다.

| 시안 보드 | 앱 화면 | 구현됨 | 남음 |
|---|---|---|---|
| 1. 구역 추천 지도 | ZoneMap | 36칸 격자(zones), 구역 선택, 근무 시작 | 시간대 칩(시간대별 점수 필요), 범례, 구역 카드(태그·시간대), 워커 이름, 이태원역 표시 |
| 2. 근무 중 | Shift | 근무 시간 타이머, 현재·다음 광고 슬롯(ad_slots) | GPS 기록, 체크인 수, 봉투 수, 긴급 연락 |
| 3. 가게 체크인 | Checkin | 없음(임시 화면) | QR 스캔, 가게 50m 반경 확인, 이번 슬롯 체크인 목록 |
| 4. 근무 종료·수거량 입력 | Collection | 없음(임시 화면) | 봉투 수, 종류별 무게, 저울·전후 사진, 예상 수입 |
| 5. 상인 리포트 | ShopReport | 없음(임시 화면) | 월간 지표, "+1 기록", 최근 청소 기록 |
| 6. QR 랜딩 웹(시민) | 없음 | 없음 | 앱이 아닌 웹 페이지. 이 저장소에서 만들지 미정 |

- 임시 화면은 `PlaceholderScreen`을 쓰고, 각 파일의 TODO에 붙일 ViewModel 이름이 적혀 있다.
- 화면 이동: 지도 → 근무 → 체크인(뒤로) / 근무 종료 → 지도(근무 기록은 스택에서 지움). 실제 기기에서 확인했다.

## 정하지 않은 것
- 서비스 이름: 시안은 "클린나이트", 앱·문서는 "야간 클린 워커".
- DB 쓰기 방식: 지금 모든 테이블이 읽기 전용이다. 체크인·근무 종료·GPS·상인 기록은 인증 방식을 정한 뒤 붙인다.
- QR 랜딩 웹을 어디서 만들고 호스팅할지.

## 빌드·실행
- `./gradlew.bat assembleDebug testDebugUnitTest`
- 기기 설치: `./gradlew.bat installDebug`
- 실행: `adb shell am start -n com.example.nightclean/.MainActivity`
- 개인 링크, Supabase 프로젝트 id, 기기 정보는 `CLAUDE.local.md`(git 제외)에 둔다.
