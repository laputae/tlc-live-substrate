#!/bin/bash
CONTENT=$(curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP')
MD5=$(printf '%s' "$CONTENT" | md5sum | cut -d' ' -f1)
ENC=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote('tlc-toggle-snapshot.json\x02TLC_GROUP\x02' + sys.argv[1] + '\x01', safe=''))" "$MD5")
echo "encoded: $ENC"
CODE=$(curl -s --max-time 20 -o /tmp/lp.body -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/cs/configs/listener' -H 'Long-Pulling-Timeout: 3000' -H 'Content-Type: application/x-www-form-urlencoded' --data "Listening-Configs=$ENC")
echo "no-change: http=$CODE body=[$(head -c 80 /tmp/lp.body)]"
curl -s -X PUT http://localhost:8087/api/toggles/fancy-danmaku -H 'Content-Type: application/json' -d '{"enabled":false}' > /dev/null
CODE=$(curl -s --max-time 20 -o /tmp/lp2.body -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/cs/configs/listener' -H 'Long-Pulling-Timeout: 3000' -H 'Content-Type: application/x-www-form-urlencoded' --data "Listening-Configs=$ENC")
echo "changed: http=$CODE body=[$(head -c 120 /tmp/lp2.body)]"
curl -s -X PUT http://localhost:8087/api/toggles/fancy-danmaku -H 'Content-Type: application/json' -d '{"enabled":true}' > /dev/null