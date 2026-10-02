#!/bin/bash
cd ~/tlc
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
echo '=== 各服务启动日志 ==='
for c in tlc-gateway tlc-risk tlc-seckill tlc-agent tlc-push tlc-audit tlc-toggle tlc-archiver; do
  echo "-- ${c}"
  docker logs "$c" 2>&1 | grep -E 'Started .*Application|APPLICATION FAILED' | tail -1
done