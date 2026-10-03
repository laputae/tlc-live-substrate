#!/bin/bash
CONTENT=$(curl -s 'http://localhost:8848/nacos/v1/cs/configs?dataId=tlc-toggle-snapshot.json&group=TLC_GROUP')
MD5=$(printf '%s' "$CONTENT" | md5sum | cut -d' ' -f1)
echo "content length=${#CONTENT} md5=$MD5"
ENC=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote('tlc-toggle-snapshot.json^2TLC_GROUP^2' + sys.argv[1] + '^2', safe=''))" "$MD5")
START=$(date +%s)
CODE=$(curl -s --max-time 35 -o /tmp/lp.body -w '%{http_code}' -X POST 'http://localhost:8848/nacos/v1/cs/configs/listener' -H 'Long-Pulling-Timeout: 3000' -H 'Content-Type: application/x-www-form-urlencoded' --data "Listening-Configs=$ENC")
END=$(date +%s)
echo "http=$CODE elapsed=$((END-START))s body=[$(head -c 100 /tmp/lp.body)]"