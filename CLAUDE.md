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

## 현재 상태
- 구역 지도(ZoneMap)와 근무(Shift) 화면이 실제 데이터를 쓴다. 근무 화면은 근무 시간 타이머와 현재·다음 광고 슬롯을 보여 준다. GPS 기록, 체크인·봉투 수, 긴급 연락은 아직 없다.
- 체크인(Checkin), 근무 종료(Collection), 상인 리포트(ShopReport)는 `PlaceholderScreen` 임시 화면이다. 각 파일의 TODO에 붙일 ViewModel 이름이 적혀 있다.
- 화면 이동: 지도 → 근무 → 체크인(뒤로) / 근무 종료 → 지도(근무 기록은 스택에서 지움). 실제 기기에서 확인했다.

## 빌드·실행
- `./gradlew.bat assembleDebug testDebugUnitTest`
- 기기 설치: `./gradlew.bat installDebug`
- 실행: `adb shell am start -n com.example.nightclean/.MainActivity`
- 개인 링크, Supabase 프로젝트 id, 기기 정보는 `CLAUDE.local.md`(git 제외)에 둔다.
