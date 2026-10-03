#!/bin/bash
for img in elasticsearch:9.1.0 docker.1ms.run/library/elasticsearch:9.0.0 docker.m.daocloud.io/library/elasticsearch:9.0.0; do
  echo "-- try $img"
  timeout 150 docker pull "$img" 2>&1 | tail -1
done
docker images --format '{{.Repository}}:{{.Tag}}' | grep -i elastic
echo '=== gateway nacos watcher log ==='
docker logs tlc-gateway 2>&1 | grep -E 'Nacos|快照' | tail -3