#!/bin/bash
echo '=== containers ==='
docker ps -a --format '{{.Names}} {{.Image}} {{.Status}}' | grep -iE 'nacos|elastic|milvus|minio|seata|sentinel|xxl' || echo 'none'
echo '=== es images ==='
docker images --format '{{.Repository}}:{{.Tag}}' | grep -i elastic || echo 'none'
echo '=== pull es 8.14.3 ==='
timeout 300 docker pull elasticsearch:8.14.3 2>&1 | tail -2