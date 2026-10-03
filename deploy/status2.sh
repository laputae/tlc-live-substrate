#!/bin/bash
cd ~/tlc
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
echo '=== ES ==='
curl -s --max-time 5 http://localhost:9200 | grep -o '"tagline" : "[^"]*"' | head -1
echo '=== Nacos toggle snapshot ==='
curl -s --max-time 5 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP' | head -c 300
echo
echo '=== services ==='
for c in tlc-gateway tlc-toggle tlc-audit tlc-agent; do
  echo "-- $c"
  docker logs "$c" 2>&1 | grep -E 'Started .*Application|FAILED' | tail -1
done