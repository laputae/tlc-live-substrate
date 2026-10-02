#!/bin/bash
echo '=== docker 镜像加速配置 ==='
docker info 2>/dev/null | grep -A4 -i 'registry mirrors' || echo 'no mirrors configured'
echo '=== 常用镜像源连通性 ==='
for r in docker.m.daocloud.io dockerproxy.net docker.1ms.run hub.rat.dev; do
  code=$(curl -sI --max-time 6 "https://$r/v2/" -o /dev/null -w '%{http_code}')
  echo "$r: $code"
done
echo '=== 实测拉取 redis:7-alpine ==='
timeout 90 docker pull redis:7-alpine >/dev/null 2>&1 && echo 'pull redis OK' || echo 'pull redis FAILED'
