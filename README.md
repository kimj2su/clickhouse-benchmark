# ClickHouse vs MySQL 조회 성능 벤치마크

MySQL과 ClickHouse에 동일한 로그성 데이터를 적재하고, 데이터량(10만/50만/100만 건)에
따른 집계 쿼리(COUNT, GROUP BY) 응답 속도를 비교하는 벤치마크 앱.

설계 문서: `../docs/superpowers/specs/2026-09-08-clickhouse-benchmark-design.md`

## 실행 방법

```bash
# 1. 인프라 기동
docker compose up -d

# 2. 앱 실행
./gradlew bootRun

# 3. 전체 벤치마크 자동 실행 (jq 필요: brew install jq)
./run-benchmark.sh
# -> results.csv 생성됨

# 4. 개별 API 수동 호출 예시
curl -X POST "http://localhost:8080/benchmark/seed?target=mysql&count=100000"
curl "http://localhost:8080/benchmark/query?target=mysql&type=groupby"
curl "http://localhost:8080/benchmark/results"

# 5. 초기화
curl -X DELETE "http://localhost:8080/benchmark/reset?target=mysql"
curl -X DELETE "http://localhost:8080/benchmark/reset?target=clickhouse"
```

## 테스트 실행

```bash
docker compose up -d
./gradlew test
```

## 결과 해석

`results.csv`의 `avgApiElapsedMs`는 API 왕복 전체 시간(HTTP 디스패치 포함),
`avgDbElapsedMs`는 순수 JDBC 쿼리 실행 시간이다. 데이터량이 늘어날 때 두 값이
MySQL과 ClickHouse에서 어떻게 벌어지는지 비교한다.

## 실측 결과

`run-benchmark.sh`로 10만 → 50만 → 100만 건까지 누적 증가시키며 실제 측정한 결과
(`avgDbElapsedMs` 기준, 단위: ms).

### GROUP BY 쿼리

| 데이터량 | MySQL | ClickHouse | 배율 |
|---|---|---|---|
| 10만 건 | 32 | 8 | 4.0배 |
| 50만 건 | 158 | 9 | 17.6배 |
| 100만 건 | 308 | 11 | 28.0배 |

### COUNT 쿼리

| 데이터량 | MySQL | ClickHouse | 배율 |
|---|---|---|---|
| 10만 건 | 11 | 5 | 2.2배 |
| 50만 건 | 41 | 3 | 13.7배 |
| 100만 건 | 101 | 4 | 25.3배 |

데이터량이 늘어날수록 격차가 뚜렷해진다. MySQL은 volume 증가에 비례해 지연이 계속
늘어나는 반면, ClickHouse는 100만 건까지도 거의 평탄한 응답 속도를 유지한다.

전체 원본 데이터는 `results.csv` 참고.
