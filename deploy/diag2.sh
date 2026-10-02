#!/bin/bash
for c in tlc-gateway tlc-seckill tlc-agent; do
  echo "===== $c ====="
  docker logs --since 3m "$c" 2>&1 | grep -A14 'APPLICATION FAILED TO START' | head -20
done