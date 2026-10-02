#!/bin/bash
for c in tlc-gateway tlc-seckill tlc-agent tlc-archiver; do
  echo "===== $c ====="
  docker logs "$c" 2>&1 | grep -B2 -A18 'APPLICATION FAILED TO START' | head -30
done