#!/bin/bash
docker logs --since 5m tlc-seckill > /tmp/sk.log 2>&1
grep -n 'ERROR\|Caused by\|Exception' /tmp/sk.log | tail -8
echo '=== persist_pop / NOSCRIPT ==='
grep -n 'persist_pop\|NOSCRIPT\|script' /tmp/sk.log | tail -5