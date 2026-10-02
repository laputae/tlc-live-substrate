package com.tlc.live.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关配置。
 */
@ConfigurationProperties(prefix = "tlc.gateway")
public class GatewayProperties {

    /** 风控服务基址。 */
    private String riskBaseUrl = "http://localhost:8082";

    /** 抢红包事件投递 topic（风控放行后）。 */
    private String rushTopic = "tlc-seckill-rush";

    /** 弹幕事件投递 topic（AI 中枢消费做情绪聚合）。 */
    private String danmakuTopic = "tlc-agent-danmaku";

    /** 上行 WebSocket 路径。 */
    private String wsPath = "/ws/upstream";

    public String getRiskBaseUrl() {
        return riskBaseUrl;
    }

    public void setRiskBaseUrl(String riskBaseUrl) {
        this.riskBaseUrl = riskBaseUrl;
    }

    public String getRushTopic() {
        return rushTopic;
    }

    public void setRushTopic(String rushTopic) {
        this.rushTopic = rushTopic;
    }

    public String getDanmakuTopic() {
        return danmakuTopic;
    }

    public void setDanmakuTopic(String danmakuTopic) {
        this.danmakuTopic = danmakuTopic;
    }

    public String getWsPath() {
        return wsPath;
    }

    public void setWsPath(String wsPath) {
        this.wsPath = wsPath;
    }
}
