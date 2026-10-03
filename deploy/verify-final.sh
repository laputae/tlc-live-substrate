#!/bin/bash
sleep 3
echo '=== MySQL 本批次落库 ==='
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e "SELECT user_id,red_packet_id,amount_fen,status FROM t_redpacket_record WHERE red_packet_id=7335953784221698" 2>/dev/null
echo '=== Redis 中奖名单 ==='
docker exec tlc-redis redis-cli HGETALL rp:winners:7335953784221698
echo '=== 剩余库存 ==='
docker exec tlc-redis redis-cli LLEN rp:stock:7335953784221698
echo '=== agent 决策日志 ==='
docker logs tlc-agent 2>&1 | grep -E '情绪分析|红包雨' | tail -4