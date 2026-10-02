#!/bin/bash
for c in tlc-gateway tlc-seckill; do
  echo "===== $c ====="
  docker logs --tail 40 "$c" 2>&1 | grep -B3 -A16 'FAILED\|ERROR' | tail -30
done