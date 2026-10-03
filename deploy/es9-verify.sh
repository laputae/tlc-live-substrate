#!/bin/bash
cd ~/tlc
sed -i 's/elasticsearch:8.14.3/elasticsearch:9.1.0/' compose.yml
docker compose up -d es 2>&1 | tail -1
sleep 12
echo '=== ES version ==='
curl -s --max-time 5 http://localhost:9200 | grep -o '"number" : "[^"]*"'
echo '=== 重新上报审计事件 ==='
curl -s -X POST http://localhost:8086/api/audit/ingest -H 'Content-Type: application/json' -d '{"eventId":"evt-test-002","traceId":"t-002","roomId":"room-1","agentName":"SentimentAgent","prompt":"danmaku x6","output":"COLD/12","promptTokens":50,"completionTokens":8,"costMs":120,"decision":"分析完成","createdAt":"2026-10-03T05:40:00Z"}'
sleep 4
echo
echo '=== ES 文档 ==='
curl -s 'http://localhost:9200/agent-audit/_doc/evt-test-002' | head -c 400
echo
echo '=== gateway 快照刷新日志 ==='
docker logs --since 15m tlc-gateway 2>&1 | grep -E '快照' | tail -2