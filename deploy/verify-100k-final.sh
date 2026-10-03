#!/bin/bash
ID=7335989493731329
sleep 20
W=$(docker exec tlc-redis redis-cli HLEN rp:winners:$ID)
S=$(docker exec tlc-redis redis-cli LLEN rp:stock:$ID)
Q=$(docker exec tlc-redis redis-cli LLEN rp:persist:queue)
echo "winners=$W stock=$S sum=$((W+S)) (expect 100000) persist_queue=$Q"
sleep 20
ROWS=$(docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -N -e "SELECT COUNT(*), COUNT(DISTINCT user_id), SUM(amount_fen) FROM t_redpacket_record WHERE red_packet_id=$ID" 2>/dev/null)
echo "mysql: rows/distinct_users/total_fen = $ROWS"
echo '=== oversell check: 任一用户不得超过 1 次 ==='
DUP=$(docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -N -e "SELECT COUNT(*) FROM (SELECT user_id FROM t_redpacket_record WHERE red_packet_id=$ID GROUP BY user_id HAVING COUNT(*)>1) t" 2>/dev/null)
echo "duplicate winners = $DUP (expect 0)"