#!/bin/bash
echo '=== 服务健康 ==='
docker logs --tail 2 tlc-risk 2>&1 | grep -E 'Started|FAILED' | tail -1
docker logs --tail 2 tlc-seckill 2>&1 | grep -E 'Started|FAILED' | tail -1
echo '=== 功能回归：drop + 双抢 + 幂等 ==='
DROP=$(curl -s --max-time 10 -X POST http://localhost:8084/api/seckill/drop -H 'Content-Type: application/json' -d '{"roomId":"opt-1","totalFen":300,"count":3}')
echo "drop: $DROP"
RP=$(echo "$DROP" | sed 's/[^0-9]//g')
curl -s -X POST http://localhost:8084/api/seckill/rush -H 'Content-Type: application/json' -d "{\"userId\":30001,\"roomId\":\"opt-1\",\"redPacketId\":$RP}"
echo
curl -s -X POST http://localhost:8084/api/seckill/rush -H 'Content-Type: application/json' -d "{\"userId\":30001,\"roomId\":\"opt-1\",\"redPacketId\":$RP}"
echo; echo '=== 已参与标记（风控预过滤用，应在 Lua 内写入） ==='
docker exec tlc-redis redis-cli SISMEMBER rp:rushed:$RP 30001
echo '=== 持久化入队（应在 Lua 内写入） ==='
sleep 3
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e "SELECT user_id,amount_fen,status FROM t_redpacket_record WHERE red_packet_id=$RP" 2>/dev/null