#!/bin/bash
echo '=== 功能回归（drop+双抢） ==='
DROP=$(curl -s --max-time 10 -X POST http://localhost:8084/api/seckill/drop -H 'Content-Type: application/json' -d '{"roomId":"opt-2","totalFen":300,"count":3}')
RP=$(echo "$DROP" | sed 's/[^0-9]//g')
echo "drop: $DROP"
curl -s -X POST http://localhost:8084/api/seckill/rush -H 'Content-Type: application/json' -d "{\"userId\":30001,\"roomId\":\"opt-2\",\"redPacketId\":$RP}"
echo
echo '=== 等待落库（1s 调度 + Lua 批量弹出） ==='
sleep 6
docker exec tlc-redis redis-cli LLEN rp:persist:queue
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e "SELECT user_id,amount_fen,status FROM t_redpacket_record WHERE red_packet_id=$RP" 2>/dev/null
echo '=== seckill 错误检查 ==='
docker logs --since 1m tlc-seckill 2>&1 | grep -cE 'ERROR'