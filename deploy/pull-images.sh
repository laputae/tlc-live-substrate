#!/bin/bash
set -x
docker pull eclipse-temurin:25-jre
docker pull apache/rocketmq:5.3.2
docker pull mysql:8.4
docker images | grep -E 'temurin|rocketmq|mysql|redis'
