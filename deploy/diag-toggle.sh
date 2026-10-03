#!/bin/bash
echo '=== toggle status/log ==='
cd ~/tlc && docker compose ps toggle --format '{{.Status}}'
docker logs --since 2m tlc-toggle 2>&1 | grep -iE 'nacos|Started|ERROR' | tail -4
echo '=== nacos config now ==='
curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP' | head -c 200
echo
echo '=== gateway watcher log ==='
docker logs --since 2m tlc-gateway 2>&1 | grep -iE 'nacos' | tail -3