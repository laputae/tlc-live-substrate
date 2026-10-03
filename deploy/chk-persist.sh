#!/bin/bash
sleep 5
docker exec tlc-redis redis-cli LLEN rp:persist:queue
docker exec tlc-mysql mysql -uroot -ptlc123456 tlc_live -e "SELECT user_id,amount_fen,status FROM t_redpacket_record WHERE red_packet_id=7335994175557633" 2>/dev/null
docker logs --since 2m tlc-seckill 2>&1 | grep -iE '落库|ERROR' | tail -3