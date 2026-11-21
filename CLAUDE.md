# 야간 클린 워커 (NightClean)

2026 서울 야간경제 활성화 해커톤(AIFactory 9320) 출품작. 상권 골목 야간 청소 인력이 가게 광고를 달고 다니며 손님을 안내한다. 비용은 가게 광고비와 서울시 수거량 보상으로 나눈다. 시범 운영은 이태원 5개 가게.

## 백엔드
- Supabase(PostgreSQL + PostgREST)를 쓴다.
- `public.zones(grid_id text pk, name text, score real 0~1)`: RLS로 anon/authenticated 읽기만 허용. 지금 값은 예시 점수 36칸이고, 공공데이터 분석 결과로 갈아 끼운다.
- `public.ad_slots(id, shop_name, start_hour, end_hour, placement)`: 근무 중 광고 슬롯. 자정 이후 시각은 24~30. 읽기 전용 RLS, 전 구역 공통. 지금은 `[가게명 N]` 예시 5행(19~24시)이다.
- `public.profiles(user_id uuid pk → auth.users, role 'worker'|'shop', display_name)`: 본인 행만 읽기(`auth.uid()`). 계정은 관리자가 대시보드에서 만들고 역할을 이 테이블에 넣는다. 공개 가입은 꺼 두었다. 데모 계정은 워커 1, 상인 1(이메일은 `CLAUDE.local.md`).
- 쓰기가 필요한 테이블은 `authenticated` + `auth.uid()` 기준 RLS로 연다. anon 쓰기는 열지 않는다(시민 QR 웹 제외).
- 접속 정보는 `local.properties`의 `supabase.url`, `supabase.key`(publishable key)에서 BuildConfig로 들어간다. git에 올리지 않는다.

## 앱 구조와 규칙
- Kotlin, Compose, AGP 9.5.2, minSdk 24 / target·compileSdk 37.
- 라이브러리: Hilt + KSP, navigation-compose(type-safe `AppRoute`), Ktor Client 3.5.2(OkHttp), kotlinx-serialization, supabase-kt `auth-kt` 3.6.0(3.7 이상은 Kotlin 2.4 필요).
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
| 1. 구역 추천 지도 | ZoneMap | 36칸 격자(zones), 구역 선택, 근무 시작 | 시간대 칩(시간대별 점수 필요), 범례, 구역 카드(태그·시간대), 워커 이름, 이태원역 표시 |
| 2. 근무 중 | Shift | 근무 시간 타이머, 현재·다음 광고 슬롯(ad_slots) | GPS 기록, 체크인 수, 봉투 수, 긴급 연락 |
| 3. 가게 체크인 | Checkin | 없음(임시 화면) | QR 스캔, 가게 50m 반경 확인, 이번 슬롯 체크인 목록 |
| 4. 근무 종료·수거량 입력 | Collection | 없음(임시 화면) | 봉투 수, 종류별 무게, 저울·전후 사진, 예상 수입 |
| 5. 상인 리포트 | ShopReport | 없음(임시 화면) | 월간 지표, "+1 기록", 최근 청소 기록 |
| 6. QR 랜딩 웹(시민) | 없음 | 없음 | 앱이 아닌 웹 페이지. 이 저장소에서 만들지 미정 |

- 임시 화면은 `PlaceholderScreen`을 쓰고, 각 파일의 TODO에 붙일 ViewModel 이름이 적혀 있다.
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
- 추천도 = 야간 인구 0.5 + 휴지통 공백 0.3 + 야간 매출 0.2. 세 지표를 이태원 범위에서 0~1로 정규화하고, 가중치는 가정이다. 데이터는 OA-23016(생활인구 250m), OA-15069(가로휴지통), OA-15572(상권 추정매출). 열린데이터 인증키는 APK에 넣지 않고, 미리 받아 Supabase에 적재한다.
- 디자인: 짙은 남색 배경 + 형광 연두 포인트 1개. 본문 글꼴 Pretendard. 모서리 16dp 이상. 장갑 낀 한 손 조작을 위해 터치 영역 56dp 이상.

**메모와 다르게 간 것**
- Koin 대신 Hilt. Domain 생략 대신 `domain`(model, repository 인터페이스). DTO = 모델 대신 DTO 분리. 위 '앱 구조와 규칙'이 기준이다.
- 지도는 네이버 지도 SDK 대신 6×6 격자 그리드. 격자 경계 데이터가 들어오면 바꾼다.
- `ad_slots`는 메모 모델에 없는 임시 테이블이다. 구역·가게 id 없이 가게명만 둔다. `shops`와 구역별 슬롯이 생기면 대체한다.

## 정하지 않은 것
- 사전 개발 허용 여부: 규칙은 개발을 '본선 무박 2일' 활동으로 적고, 메모는 허용될 때만 코드를 쓰기로 했다. 운영팀 답을 아직 못 받았다(11/21 기준).
- 서비스 이름: 시안은 "클린나이트", PRD·메모·앱은 "야간 클린 워커".
- QR 랜딩 웹을 어디서 만들고 호스팅할지.

## 빌드·실행
- `./gradlew.bat assembleDebug testDebugUnitTest`
- 기기 설치: `./gradlew.bat installDebug`
- 실행: `adb shell am start -n com.example.nightclean/.MainActivity`
- 개인 링크, Supabase 프로젝트 id, 기기 정보는 `CLAUDE.local.md`(git 제외)에 둔다.
