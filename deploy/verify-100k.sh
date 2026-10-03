#!/bin/bash
sleep 15
W=$(docker exec tlc-redis redis-cli HLEN rp:winners:7335986747293699 2>/dev/null)
L=$(docker exec tlc-redis redis-cli LLEN rp:persist:queue)
ROWS=$(docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -N -e "SELECT COUNT(*) FROM t_redpacket_record WHERE red_packet_id=7335986747293699" 2>/dev/null)
echo "winners(redis)=$W  mysql_rows=$ROWS  persist_queue=$L"
echo "=== oversell check: distinct winners vs rows ==="
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -N -e "SELECT COUNT(DISTINCT user_id), SUM(amount_fen) FROM t_redpacket_record WHERE red_packet_id=7335986747293699" 2>/dev/null