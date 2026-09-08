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
