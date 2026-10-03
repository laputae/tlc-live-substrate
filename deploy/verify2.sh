#!/bin/bash
echo '=== 1. 上报审计事件到 audit 服务 ==='
curl -s -X POST http://localhost:8086/api/audit/ingest -H 'Content-Type: application/json' -d '{"eventId":"evt-test-001","traceId":"t-001","roomId":"room-1","agentName":"SentimentAgent","prompt":"danmaku x6","output":"COLD/12","promptTokens":50,"completionTokens":8,"costMs":120,"decision":"分析完成","createdAt":"2026-10-03T05:40:00Z"}'
echo; sleep 3
echo '=== 2. ES 中查询该文档 ==='
curl -s 'http://localhost:9200/agent-audit/_search?q=evt-test-001' | grep -o '"value":[0-9]*' | head -1
curl -s 'http://localhost:9200/agent-audit/_doc/evt-test-001' | head -c 300
echo; echo '=== 3. 修改开关（应自动推送 Nacos 并刷新网关） ==='
curl -s -X PUT http://localhost:8087/api/toggles/ai-sentiment -H 'Content-Type: application/json' -d '{"enabled":false}'
echo; sleep 6
echo '=== 4. Nacos 中的配置 ==='
curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP' | head -c 300
echo; echo '=== 5. 网关日志（快照刷新） ==='
docker logs --since 30s tlc-gateway 2>&1 | grep -E '快照|开关' | tail -2