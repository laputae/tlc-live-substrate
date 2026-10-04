# tlc-live-substrate
直播场控与百万级并发博弈基座
**AI-Native 直播场控与百万级并发博弈基座** —— 解决大模型（LLM）长耗时推理与直播间极高频互动（秒杀/红包雨）之间的架构矛盾。

> Java 25 (LTS) · Spring Boot 4.1 · Spring AI 2.0（DeepSeek） · Redis · RocketMQ 5 · MySQL 8.4 · Elasticsearch 9 · Nacos 2.5
> 全部服务运行于虚拟线程（`spring.threads.virtual.enabled=true`）

## 系统架构

```
 用户 WS / HTTP
      |
+------------------+   DANMAKU(MQ)   +------------------+  LLM(DeepSeek)  +--------------+
|  substrate-      | --------------> |  substrate-agent | <--------------- |  DeepSeek    |
|  gateway         |                 |  (Multi-Agent)   | --决策(MQ)-----> +--------------+
+---------+--------+                 +--------+---------+
          | RUSH(MQ)                          |
+---------v--------+   短路查杀       +--------v---------+
|  substrate-risk  | <--------------- |  substrate-      |
|  结构化并发       |   放行(HTTP)     |  seckill         |
+------------------+                 |  Lua 原子扣减    |
                                     +--------+---------+
          广播 / 审计 / 开关                    |
+------------------------------------------------------+
| redis · mysql · rocketmq · es · nacos   (compose)     |
+------------------------------------------------------+
          |
+---------v--------+   +--------------------+   +----------------------+
| substrate-push   |   | substrate-audit    |   | substrate-toggle     |
| 下行广播          |   | ES 审计落库         |   | Nacos 推拉开关        |
+------------------+   +--------------------+   +----------------------+
```

## 模块一览

| 模块 | 端口 | 核心职责 | 关键技术 |
| --- | --- | --- | --- |
| substrate-common | - | 事件契约(DOP)/异常/Nacos 客户端/工具 | sealed interface、record |
| substrate-gateway | 8081 | 长连接接入、DOP 路由、风控转发、MQ 削峰投递 | 虚拟线程、WebSocket、RocketMQ |
| substrate-risk | 8082 | 指纹/信誉/去重三路并发校验 | StructuredTaskScope 短路查杀、ScopedValue、50ms 熔断 |
| substrate-agent | 8083 | 弹幕情绪聚合、AI 自主红包决策（预算围栏）、Token 审计 | Spring AI 2.0 Multi-Agent（DeepSeek） |
| substrate-seckill | 8084 | 凭证预热、Lua 原子扣减、批量落库 | RocketMQ 消费、Redis Lua 单脚本原子 |
| substrate-push | 8085 | 下行广播 | Redis Pub/Sub 订阅 -> WS 推送，无状态扩容 |
| substrate-audit | 8086 | LLM Prompt/输出/Token 审计 | 异步缓冲管道、Elasticsearch（可降级） |
| substrate-toggle | 8087 | 特征开关、一键保命（核心链路永不切断） | 内存热开关 + Nacos 推拉同步 |
| substrate-archiver | 8088 | 凌晨低谷 MySQL -> ClickHouse 冷数据沉淀 | 调度批处理（默认关闭） |
## 核心链路：AI 下发红包雨并遭遇突发秒杀

1. **AI 决策**：弹幕经 gateway 投递 MQ -> agent 聚合窗口 -> DeepSeek 情绪判定（如 COLD/12）-> ActionAgent 在**预算围栏**内决策 `{action:"DROP_REDPACKET", totalFen, count}` -> MQ
2. **预热**：seckill 消费决策，金额按二倍均值法预拆分为凭证 LPUSH 入库，广播 `REDPACKET_DROP`
3. **抢夺**：用户 RUSH -> gateway -> **risk 结构化并发短路查杀**（任一路命中立即取消其余 I/O；50ms 窗口熔断）-> 放行投 MQ
4. **扣减**：seckill 消费，**一个 Lua 脚本内原子完成**幂等校验 + 凭证弹出 + 中奖记录——发 10000 个绝不被第 10001 人抢到
5. **收尾**：`REDPACKET_WIN` 广播 -> MySQL 批量落库（最终一致）-> LLM Token 消耗落 ES 审计

## 性能实测（服务器 msi：AMD R7 5700G 8C/16T，全容器化）

| 场景 | 结果 |
| --- | --- |
| 1 万用户 WS 抢红包 | 发送 **50,761 msg/s**（0.15s 发完），ACK 6s 全量回收 |
| 2 千用户对照 | PASS=1597 / REJECT=403（50ms 熔断兜底），账目 1597+403=2000 精确守恒 |
| 5 万用户 | CPU 峰值 ~14 逻辑核（gateway 384% / risk 299% / broker 364%） |
| **10 万用户** | **ACK 全量 PASS=100000**，扣减稳定 **~3850 deduct/s**，`winners 100000 + stock 0` 精确守恒，**零超卖、零重复中奖** |

风控行为：黑名单命中 **1ms 短路拒绝**（其余校验 I/O 被结构化并发取消）；过载时 50ms 窗口熔断主动拒单（`RISK_TIMEOUT_MS` 可调）。

## 快速开始

### 构建与静态检查

```powershell
$env:JAVA_HOME = '<JDK 25 路径>'   # 结构化并发为预览 API，需 Java 25
mvn -B -DskipTests package          # 编译含 -Xlint:all
```

### 部署（全容器化，目标 Linux + Docker）

```bash
# 见 deploy/compose.yml：Redis/MySQL/RocketMQ/ES/Nacos + 8 个微服务
cd ~/tlc
docker compose up -d
```

- `risk` 运行期必须带 `--enable-preview`（compose 已配置）
- DeepSeek 密钥：`DEEPSEEK_API_KEY` 环境变量，或写入 `~/tlc/.env`（compose 自动读取，勿入库）
- 替换 bind-mount 的 jar 后必须 `docker compose up -d --force-recreate <service>`，仅 restart 会读到错位文件

### 冒烟测试

```bash
# 发红包雨
curl -X POST http://localhost:8084/api/seckill/drop -H 'Content-Type: application/json' \
  -d '{"roomId":"room-1","totalFen":1000,"count":3}'
# 抢红包（重复抢返回 ALREADY，抢完返回 SOLD_OUT）
curl -X POST http://localhost:8084/api/seckill/rush -H 'Content-Type: application/json' \
  -d '{"userId":10001,"roomId":"room-1","redPacketId":<上一步 ID>}'
# 风控直测 / 开关快照 / 审计健康
curl -X POST http://localhost:8082/api/risk/check -H 'Content-Type: application/json' -d '{"userId":10001,"roomId":"room-1","redPacketId":1}'
curl http://localhost:8087/api/toggles
curl http://localhost:8086/api/audit/health
```

### 万人压测

```bash
# deploy/loadtest/JLoadTest.java，在 compose 网络内运行
docker run --rm --network tlc-live_tlc-net -v ~/tlc/test:/src -w /src \
  eclipse-temurin:25-jre java -cp /src JLoadTest 10000 1000
# 参数：<用户数> <连接数>；结束后核对 winners+stock==总发放（零超卖）
```

## 配置要点（环境变量）

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `DEEPSEEK_API_KEY` | - | LLM 密钥（agent），未配置时 LLM 安全降级不发红包 |
| `RISK_TIMEOUT_MS` | 50 | 风控判定窗口，过载熔断阈值 |
| `NACOS_ADDR` | http://nacos:8848 | 开关配置中心地址 |
| `ARCHIVE_ENABLED` | false | 冷数据归档任务开关（需 ClickHouse） |
| `AUDIT_ES_ENABLED` | true | ES 审计落库开关，false 时降级本地日志 |

## 工程化

- **静态检查**：编译开启 `-Xlint:all`；SpotBugs 4.9（threshold=Medium，排除清单 `spotbugs-exclude.xml`）
- **测试**：冒烟脚本（`deploy/`）+ WebSocket 全链路集成测试 + 万人压测客户端（`deploy/loadtest/`）
