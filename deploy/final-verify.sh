#!/bin/bash
sleep 20
W=$(docker exec tlc-redis redis-cli HLEN rp:winners:7335986747293699)
S=$(docker exec tlc-redis redis-cli LLEN rp:stock:7335986747293699)
echo "winners=$W stock=$S sum=$((W+S)) (expect 10000)"
docker exec tlc-redis redis-cli HGETALL rp:winners:7335986747293699 | head -4
echo '...'
echo '=== MySQL persisted ==='
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e 'SELECT COUNT(*) AS persisted, SUM(amount_fen) AS total_fen FROM t_redpacket_record WHERE red_packet_id=7335986747293699' 2>/dev/null
echo '=== persist queue left ==='
docker exec tlc-redis redis-cli LLEN rp:persist:queue
echo '=== ES audit count ==='
curl -s 'http://localhost:9200/agent-audit/_count' | head -c 100
echo