package com.tlc.live.archiver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 冷热数据沉淀配置。
 */
@ConfigurationProperties(prefix = "tlc.archive")
public class ArchiveProperties {

    /** 总开关：默认关闭，避免无 ClickHouse 环境时启动即报错。 */
    private boolean enabled = false;

    /** 源端 MySQL 业务库。 */
    private String sourceUrl = "jdbc:mysql://localhost:3306/tlc_live?useSSL=false&serverTimezone=UTC";
    private String sourceUser = "root";
    private String sourcePassword = "root";

    /** 目标端 ClickHouse 列式仓库。 */
    private String targetUrl = "jdbc:clickhouse://localhost:8123/tlc_dw";
    private String targetUser = "default";
    private String targetPassword = "";

    /** 单批迁移行数。 */
    private int batchSize = 5000;

    /** 调度时间：默认凌晨 3 点业务低谷期。 */
    private String cron = "0 0 3 * * ?";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public void setSourcePassword(String sourcePassword) {
        this.sourcePassword = sourcePassword;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
