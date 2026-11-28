-- 시간대별(19~20 … 22~23시) 추천도와 금·토·평일 인구. 배열 4칸, 분석 스크립트(analysis/zones.py) 결과
alter table public.zones
  add column score_by_hour real[] check (score_by_hour is null or array_length(score_by_hour, 1) = 4),
  add column night_pop_by_hour int[] check (night_pop_by_hour is null or array_length(night_pop_by_hour, 1) = 4),
  add column weekday_pop_by_hour int[] check (weekday_pop_by_hour is null or array_length(weekday_pop_by_hour, 1) = 4);
