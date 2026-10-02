package com.tlc.live.gateway.client;

import com.tlc.live.common.util.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 风控服务客户端。
 *
 * <p>ScopedValue 里绑定的用户令牌随请求头透传给风控服务，
 * 全链路不落地 ThreadLocal，也不污染线程池。
 */
@Component
public class RiskClient {

    private final RestClient restClient;

    public RiskClient(@Value("${tlc.gateway.risk-base-url:http://localhost:8082}") String riskBaseUrl) {
        this.restClient = RestClient.builder().baseUrl(riskBaseUrl).build();
    }

    public RiskCheckResult check(long userId, String roomId, long redPacketId) {
        RiskCheckRequest request = new RiskCheckRequest(userId, roomId, redPacketId);
        return restClient.post()
                .uri("/api/risk/check")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Token", UserContext.token() == null ? "" : UserContext.token())
                .body(request)
                .retrieve()
                .body(RiskCheckResult.class);
    }

    /** 与 risk 服务契约一致的本地 DTO（微服务间不共享类型，只共享 JSON 契约）。 */
    public record RiskCheckRequest(long userId, String roomId, long redPacketId) {
    }

    public record RiskCheckResult(long userId, boolean passed, String reason, long latencyMs) {
    }
}
