#!/bin/bash
cd ~/tlc
echo '=== waiting for ES ==='
for i in 1 2 3 4 5 6 8 10 12; do
  if curl -s --max-time 3 http://localhost:9200 > /dev/null; then
    echo "ES ready (waited ~${i}0s)"
    break
  fi
  sleep 10
done
curl -s --max-time 5 http://localhost:9200 | grep -o '"number" : "[^"]*"'
echo '=== ingest audit event ==='
curl -s -X POST http://localhost:8086/api/audit/ingest -H 'Content-Type: application/json' -d '{"eventId":"evt-test-003","traceId":"t-003","roomId":"room-1","agentName":"SentimentAgent","prompt":"danmaku x6","output":"COLD/12","promptTokens":50,"completionTokens":8,"costMs":120,"decision":"analysis-done","createdAt":"2026-10-03T05:40:00Z"}'
sleep 4
echo
echo '=== ES doc ==='
curl -s 'http://localhost:9200/agent-audit/_doc/evt-test-003' | head -c 500
echo
echo '=== gateway snapshot log ==='
docker logs --since 30m tlc-gateway 2>&1 | grep -E '快照' | tail -2