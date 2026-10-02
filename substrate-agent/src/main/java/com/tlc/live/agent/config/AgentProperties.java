package com.tlc.live.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 场控配置：LLM 输出的金额与数量会被预算围栏硬性裁剪（clamp），
 * 这是 AI 自主花钱的安全底线。
 */
@ConfigurationProperties(prefix = "tlc.agent")
public class AgentProperties {

    private long scanIntervalMs = 15_000;
    private int minSamples = 5;
    private int budgetMaxTotalFen = 10_000;
    private int budgetMaxCount = 200;
    private int coldThreshold = 35;
    private String auditBaseUrl = "http://localhost:8086";
    private int windowSize = 100;

    public long getScanIntervalMs() {
        return scanIntervalMs;
    }

    public void setScanIntervalMs(long scanIntervalMs) {
        this.scanIntervalMs = scanIntervalMs;
    }

    public int getMinSamples() {
        return minSamples;
    }

    public void setMinSamples(int minSamples) {
        this.minSamples = minSamples;
    }

    public int getBudgetMaxTotalFen() {
        return budgetMaxTotalFen;
    }

    public void setBudgetMaxTotalFen(int budgetMaxTotalFen) {
        this.budgetMaxTotalFen = budgetMaxTotalFen;
    }

    public int getBudgetMaxCount() {
        return budgetMaxCount;
    }

    public void setBudgetMaxCount(int budgetMaxCount) {
        this.budgetMaxCount = budgetMaxCount;
    }

    public int getColdThreshold() {
        return coldThreshold;
    }

    public void setColdThreshold(int coldThreshold) {
        this.coldThreshold = coldThreshold;
    }

    public String getAuditBaseUrl() {
        return auditBaseUrl;
    }

    public void setAuditBaseUrl(String auditBaseUrl) {
        this.auditBaseUrl = auditBaseUrl;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }
}
