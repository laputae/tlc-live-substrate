#!/bin/bash
echo '=== loadtest container log ==='
docker logs --tail 10 tlc-loadtest 2>&1
echo '=== rp:meta keys ==='
docker exec tlc-redis redis-cli --scan --pattern 'rp:meta:*' | head -5
echo '=== stock lens ==='
for k in $(docker exec tlc-redis redis-cli --scan --pattern 'rp:stock:*'); do
  echo "$k = $(docker exec tlc-redis redis-cli LLEN $k)"
done