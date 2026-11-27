# 시연 가이드 (3분)

앱을 직접 보여 줄 때의 동선. "배정 확인 → 근무 → 상인 리포트" 한 바퀴를 워커 폰과 상인 폰 두 대로 진행한다. 해커톤 본선용으로 만든 대본을 일반 시연용으로 고쳤다.

## 미리 할 일

### 예시 데이터 날짜 갱신
상인 리포트는 **이번 달**만 센다. 예시 데이터가 지난달에 머물면 리포트가 비므로, 리포트를 보여 주기 전에 아래 SQL을 SQL Editor에서 실행한다. 가장 최근에 지난 토요일 밤을 기준으로 주 단위로 이동하므로 요일과 시각은 그대로이고, 이미 최근이면 아무것도 바뀌지 않는다.

```sql
begin;
create temp table demo_shift on commit drop as
select make_interval(weeks => greatest(0, floor((
  (((now() at time zone 'Asia/Seoul') - interval '6 hours')::date - 1)
  - max((started_at at time zone 'Asia/Seoul')::date)) / 7.0)::int)) as d
from public.shifts where id::text like '00000000-0000-4000-8000-%';
update public.shifts set started_at = started_at + (select d from demo_shift), ended_at = ended_at + (select d from demo_shift)
  where id::text like '00000000-0000-4000-8000-%';
update public.checkins set checked_at = checked_at + (select d from demo_shift)
  where shift_id::text like '00000000-0000-4000-8000-%';
update public.collections set created_at = created_at + (select d from demo_shift)
  where shift_id::text like '00000000-0000-4000-8000-%';
update public.guest_visits set created_at = created_at + (select d from demo_shift) where id between 6 and 12;
update public.qr_scans set scanned_at = scanned_at + (select d from demo_shift) where id::text like '00000000-0000-4000-8000-%';
commit;
```
월초에는 옮긴 네 밤 중 일부가 지난달에 남아 리포트 숫자가 작게 나올 수 있다. 실제 데이터도 월초에는 그렇다.

### 데모 가게를 시연 장소로 옮기기
체크인은 가게에서 50m 안이어야 인정된다. 이태원 밖에서 체크인하려면 데모 상인 가게(`NC-SHOP-001`)의 좌표를 시연 자리로 옮긴다. 그러면 체크인 → 상인 리포트 반영이 같은 가게로 이어진다.

```sql
update public.shops set lat = <시연 장소 위도>, lng = <시연 장소 경도> where qr_code = 'NC-SHOP-001';
-- 시연 뒤 되돌리기
update public.shops set lat = 37.5348, lng = 126.9941 where qr_code = 'NC-SHOP-001';
```
좌표는 시연 자리에서 지도 앱으로 확인한다(실내 GPS 오차가 크면 창가 쪽에서).

### 시작 전 (10분)
- [ ] 두 폰에 최신 APK(README 내려받기 링크). 워커 폰은 워커 계정, 상인 폰은 상인 계정으로 **미리 로그인**
- [ ] 워커 폰에서 근무를 한 번 시작해 위치 동의·카메라·알림 권한을 미리 허용하고, 정확한 위치를 켠다. 확인 뒤 근무 종료로 저장(예행 근무는 시연 뒤 정리)
- [ ] 인쇄한 `NC-SHOP-001` QR(`web/print.html`)을 시연 자리에 둠
- [ ] 휴대폰 핫스팟 준비(현장 와이파이가 막히면 전환), 밝기 최대, 방해 금지 켜기
- [ ] 막히면 보여 줄 화면 캡처: `docs/screenshots/`

## 대본

| 시간 | 폰 | 화면과 동작 | 말할 것 |
|---|---|---|---|
| 0:00 | 워커 | 구역 지도. 1위 칸(이태원 관광특구 4)을 누름 | 서울 생활인구·가로쓰레기통·상권 매출로 오늘 밤 갈 골목을 고릅니다. 여기는 금·토 밤 인구가 가장 많은 칸입니다 |
| 0:30 | 워커 | 근무 시작 → 근무 중 화면. 지금 광고 슬롯, 청소 전 사진 1장 | 근무 동안 GPS로 동선을 남기고, 조끼에는 지금 시간대 광고 가게가 걸립니다 |
| 1:00 | 워커 | QR 체크인(스캐너가 바로 열림) → '체크인 완료'(가게명, 이번 근무 몇 번째) | 가게 앞 QR을 찍으면 서버가 50m 안인지 확인합니다. 사진으로 찍은 QR로는 인정되지 않습니다 |
| 1:25 | 워커 | 근무 화면 복귀(체크인 1) → 봉투 +1 → 근무 종료 | 채운 봉투는 누른 시각의 시간대로 기록됩니다 |
| 1:45 | 워커 | 근무 종료: 근무 시간·예상 수당 확인, 봉투 사진 1장, 저장(무게는 비워 둠) | 워커는 시급제라 수거량과 상관없이 근무 시간만큼 받습니다 |
| 2:15 | 상인 | 상인 리포트(앱으로 돌아오면 새로 고침). 청소한 밤·치운 봉투·QR 스캔·청결도, 최근 청소 기록 사진 | 가게는 광고비가 무엇을 만들었는지 이 화면으로 봅니다 |
| 2:40 | 상인 | 손님 +1 | "클린 워커 보고 왔어요"라고 말한 손님(광고 보고 온 손님)을 셉니다 |
| 2:50 | 보는 사람 | 시연 자리의 QR을 각자 폰으로 스캔 → QR 랜딩 웹 | 시민은 앱 없이 가게 안내와 혜택을 보고 거리 청결도를 평가합니다 |

## 막힐 때
- 체크인 '거리 초과': 가게 좌표가 시연 장소로 옮겨졌는지, 정확한 위치가 켜졌는지 확인. 안 되면 화면 캡처로 넘어간다.
- 네트워크 오류: 핫스팟으로 바꾸고 '다시 시도'.
- 상인 리포트에 방금 체크인이 안 보임: 상인 앱을 홈으로 보냈다가 다시 연다.

## 시연 뒤 정리
- 데모 가게 좌표 되돌리기(위 SQL).
- 리허설·시연 중 생긴 근무(예시 id 접두가 아닌 행)는 예시 데이터와 섞이지 않게 지운다. Storage 사진은 대시보드에서 지운다.
