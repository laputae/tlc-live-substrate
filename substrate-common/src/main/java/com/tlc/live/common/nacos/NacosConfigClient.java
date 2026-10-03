package com.tlc.live.common.nacos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Nacos Open API 轻量客户端（v1 兼容接口，无需 spring-cloud-alibaba）。
 *
 * <p>支持：配置发布、配置读取、长轮询监听（push 模式的基础）。
 * 不引入 SDK 依赖，保持各微服务轻量。
 */
@Component
public class NacosConfigClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String nacosAddr;
    private final String dataId;
    private final String group;

    public NacosConfigClient(
            @Value("${tlc.nacos-addr:http://nacos:8848}") String nacosAddr,
            @Value("${tlc.nacos.data-id:tlc-toggle-snapshot.json}") String dataId,
            @Value("${tlc.nacos.group:TLC_GROUP}") String group) {
        this.nacosAddr = nacosAddr;
        this.dataId = dataId;
        this.group = group;
    }

    /** 发布配置（覆盖写）。 */
    public void publish(String content) throws Exception {
        String body = "dataId=" + enc(dataId) + "&group=" + enc(group)
                + "&content=" + enc(content) + "&type=json";
        HttpRequest req = HttpRequest.newBuilder(URI.create(nacosAddr + "/nacos/v1/cs/configs"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Nacos publish failed: " + resp.statusCode() + " " + resp.body());
        }
    }

    /** 读取配置，不存在返回 null。 */
    public String fetch() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(nacosAddr + "/nacos/v1/cs/configs?dataId="
                        + enc(dataId) + "&group=" + enc(group)))
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    /**
     * 长轮询：配置无变化时服务端挂起请求直到超时；返回 true 表示有变化（需重新 fetch）。
     */
    public boolean longPoll(String contentMd5, int timeoutSec) throws Exception {
        // Listening-Configs 格式: dataId^2group^2md5^2tenant^1（条目以 ^1 结尾，缺失会报 invalid probeModify）
        String listening = enc(dataId + "\u0002" + group + "\u0002" + contentMd5 + "\u0001");
        HttpRequest req = HttpRequest.newBuilder(URI.create(nacosAddr + "/nacos/v1/cs/configs/listener"))
                .header("Long-Pulling-Timeout", String.valueOf(timeoutSec * 1000))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("Listening-Configs=" + listening, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Nacos long-poll failed: " + resp.statusCode() + " " + resp.body());
        }
        return !resp.body().isBlank();
    }

    /** 配置内容的 MD5（长轮询比对用）。 */
    public static String md5Hex(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}