#!/bin/bash
cd ~/tlc
docker rm -f tlc-loadtest 2>/dev/null
docker run -d --name tlc-loadtest --network tlc-live_tlc-net -v /home/xiaoming/tlc/test:/src -w /src eclipse-temurin:25-jre java -Xmx1g -cp /src JLoadTest 50000 2000
for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
  sleep 4
  echo "-- sample $i"
  docker stats --no-stream --format '{{.Name}} {{.CPUPerc}}' | grep -E 'tlc-(gateway|seckill|risk|redis|broker|namesrv|mysql|loadtest)'
done
echo '=== loadtest result ==='
sleep 10
docker logs --tail 8 tlc-loadtest