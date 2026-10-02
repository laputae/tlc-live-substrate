#!/bin/bash
BASE=http://localhost
echo '=== 1. toggle 快照 ==='
curl -s $BASE:8087/api/toggles
echo; echo '=== 2. 风控-正常用户 ==='
curl -s -X POST $BASE:8082/api/risk/check -H 'Content-Type: application/json' -d '{"userId":10001,"roomId":"room-1","redPacketId":1}'
echo; echo '=== 3. 订阅下行广播 ==='
docker exec tlc-redis redis-cli SUBSCRIBE tlc:downstream:push > /tmp/sub.txt 2>&1 &
SUBPID=$!
sleep 1
echo '=== 4. 发红包雨 ==='
DROP=$(curl -s -X POST $BASE:8084/api/seckill/drop -H 'Content-Type: application/json' -d '{"roomId":"room-1","totalFen":1000,"count":3}')
echo "$DROP"
RP=$(echo "$DROP" | sed 's/[^0-9]//g')
echo '=== 5. 抢红包（同用户两次） ==='
curl -s -X POST $BASE:8084/api/seckill/rush -H 'Content-Type: application/json' -d "{\"userId\":10001,\"roomId\":\"room-1\",\"redPacketId\":$RP}"
echo
curl -s -X POST $BASE:8084/api/seckill/rush -H 'Content-Type: application/json' -d "{\"userId\":10001,\"roomId\":\"room-1\",\"redPacketId\":$RP}"
echo
sleep 5
kill $SUBPID 2>/dev/null
echo '=== 6. 下行广播捕获 ==='
grep -a REDPACKET /tmp/sub.txt || tail -5 /tmp/sub.txt
echo '=== 7. MySQL 落库 ==='
sleep 2
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e 'SELECT room_id,user_id,red_packet_id,amount_fen,status FROM t_redpacket_record' 2>/dev/null
echo '=== 8. 风控-黑名单拒绝 ==='
docker exec tlc-redis redis-cli SADD risk:blacklist 99999 > /dev/null
curl -s -X POST $BASE:8082/api/risk/check -H 'Content-Type: application/json' -d '{"userId":99999,"roomId":"room-1","redPacketId":1}'
echo