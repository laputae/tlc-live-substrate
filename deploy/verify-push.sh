#!/bin/bash
echo '=== 1. set ai-sentiment=true ==='
curl -s -X PUT http://localhost:8087/api/toggles/ai-sentiment -H 'Content-Type: application/json' -d '{"enabled":true}'
echo; sleep 6
echo '=== 2. nacos config ==='
curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP'
echo; echo '=== 3. gateway refresh log ==='
docker logs --since 30s tlc-gateway 2>&1 | grep -E '快照' | tail -2
echo '=== 4. disable danmaku-ingest ==='
curl -s -X PUT http://localhost:8087/api/toggles/danmaku-ingest -H 'Content-Type: application/json' -d '{"enabled":false}'
echo; sleep 6
docker logs --since 15s tlc-gateway 2>&1 | grep -E '快照' | tail -1
echo '=== 5. restore danmaku-ingest ==='
curl -s -X PUT http://localhost:8087/api/toggles/danmaku-ingest -H 'Content-Type: application/json' -d '{"enabled":true}'
echo