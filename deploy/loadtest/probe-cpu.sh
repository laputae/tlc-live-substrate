#!/bin/bash
cd ~/tlc
docker rm -f tlc-loadtest 2>/dev/null
docker run -d --name tlc-loadtest --network tlc-live_tlc-net -v /home/xiaoming/tlc/test:/src -w /src eclipse-temurin:25-jre java -Xmx1g -cp /src JLoadTest 10000 1000
echo '=== CPU 采样（每 3 秒） ==='
for i in 1 2 3 4 5 6 7 8; do
  sleep 3
  echo "-- sample $i"
  docker stats --no-stream --format '{{.Name}} {{.CPUPerc}}' | grep -E 'tlc-(gateway|seckill|risk|redis|broker|namesrv|mysql|loadtest)'
done
echo '=== loadtest result ==='
sleep 15
docker logs --tail 6 tlc-loadtest