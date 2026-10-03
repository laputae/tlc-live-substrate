#!/bin/bash
docker logs --since 5m tlc-risk > /tmp/risk.log 2>&1
echo '=== reject reasons ==='
grep '风控拒绝' /tmp/risk.log | grep -oE 'reason=[^"]*' | sed 's/[0-9]*//g' | sort | uniq -c | sort -rn | head -5
echo "timeout: $(grep -c '窗口超时' /tmp/risk.log)"
echo "dedup: $(grep -c '重复参与' /tmp/risk.log)"
echo "blacklist: $(grep -c '黑名单' /tmp/risk.log)"
echo "err count: $(grep -cE '长轮询异常|ERROR' /tmp/risk.log)"