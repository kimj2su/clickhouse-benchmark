#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080/benchmark"
OUTPUT_CSV="results.csv"
TARGETS=("mysql" "clickhouse")
QUERY_TYPES=("count" "groupby")
VOLUME_STEPS=(100000 400000 500000)  # 누적: 10만 -> 50만(+40만) -> 100만(+50만)
WARMUP_RUNS=5
MEASURED_RUNS=5

echo "target,type,totalRows,avgApiElapsedMs,avgDbElapsedMs" > "$OUTPUT_CSV"

for target in "${TARGETS[@]}"; do
  curl -s -X DELETE "$BASE_URL/reset?target=$target" > /dev/null
done

for target in "${TARGETS[@]}"; do
  cumulative=0
  for step in "${VOLUME_STEPS[@]}"; do
    cumulative=$((cumulative + step))
    echo "Seeding $target with $step rows (cumulative: $cumulative)..."
    curl -s -X POST "$BASE_URL/seed?target=$target&count=$step" > /dev/null

    for type in "${QUERY_TYPES[@]}"; do
      for i in $(seq 1 $WARMUP_RUNS); do
        curl -s "$BASE_URL/query?target=$target&type=$type" > /dev/null
      done

      total_api=0
      total_db=0
      for i in $(seq 1 $MEASURED_RUNS); do
        response=$(curl -s "$BASE_URL/query?target=$target&type=$type")
        api_ms=$(echo "$response" | jq '.apiElapsedMs')
        db_ms=$(echo "$response" | jq '.dbElapsedMs')
        total_api=$((total_api + api_ms))
        total_db=$((total_db + db_ms))
      done

      avg_api=$((total_api / MEASURED_RUNS))
      avg_db=$((total_db / MEASURED_RUNS))
      echo "$target,$type,$cumulative,$avg_api,$avg_db" >> "$OUTPUT_CSV"
      echo "  [$target/$type] rows=$cumulative avgApiMs=$avg_api avgDbMs=$avg_db"
    done
  done
done

echo "Done. Results saved to $OUTPUT_CSV"
