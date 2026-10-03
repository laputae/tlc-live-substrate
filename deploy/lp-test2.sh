#!/bin/bash
echo '=== gateway nacos log (last 3m) ==='
docker logs --since 3m tlc-gateway 2>&1 | grep -iE 'nacos|快照|ERR' | tail -4
echo '=== long-poll test with ^1 ==='
CONTENT=$(curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP')
MD5=$(printf '%s' "$CONTENT" | md5sum | cut -d' ' -f1)
ENC=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote('tlc-toggle-snapshot.json^2TLC_GROUP^2' + sys.argv[1] + '^2^1', safe=''))" "$MD5")
START=$(date +%s)
CODE=$(curl -s --max-time 35 -o /tmp/lp.body -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/cs/configs/listener' -H 'Long-Pulling-Timeout: 5000' -H 'Content-Type: application/x-www-form-urlencoded' --data "Listening-Configs=$ENC")
END=$(date +%s)
echo "http=$CODE elapsed=$((END-START))s body=[$(head -c 80 /tmp/lp.body)]"
echo '=== change toggle then re-poll ==='
curl -s -X PUT http://localhost:8087/api/toggles/fancy-danmaku -H 'Content-Type: application/json' -d '{"enabled":false}' > /dev/null
CODE=$(curl -s --max-time 35 -o /tmp/lp2.body -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/cs/configs/listener' -H 'Long-Pulling-Timeout: 5000' -H 'Content-Type: application/x-www-form-urlencoded' --data "Listening-Configs=$ENC")
echo "after change: http=$CODE body=[$(head -c 80 /tmp/lp2.body)]"
curl -s -X PUT http://localhost:8087/api/toggles/fancy-danmaku -H 'Content-Type: application/json' -d '{"enabled":true}' > /dev/null