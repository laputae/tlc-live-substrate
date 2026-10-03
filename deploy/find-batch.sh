#!/bin/bash
echo '=== all batches: stock left ==='
for k in $(docker exec tlc-redis redis-cli --scan --pattern 'rp:stock:*' | sort); do
  L=$(docker exec tlc-redis redis-cli LLEN "$k")
  [ "$L" -gt 0 ] && echo "$k stock=$L"
done
echo '=== find the 100000 batch (meta count=100000) ==='
for k in $(docker exec tlc-redis redis-cli --scan --pattern 'rp:meta:*'); do
  C=$(docker exec tlc-redis redis-cli HGET "$k" count)
  echo "$k count=$C"
done