package com.tlc.live.archiver.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 双数据源：MySQL（OLTP 业务库，只读搬运）与 ClickHouse（OLAP 列式仓库）。
 *
 * <p>initializationFailTimeout=-1 允许数据库暂不可用时服务照常启动，
 * 首次调度再建立连接，方便本地无依赖跑通骨架。
 */
@Configuration
public class ArchiveDataSourceConfig {

    @Bean
    public JdbcTemplate sourceJdbcTemplate(ArchiveProperties props) {
        return new JdbcTemplate(hikari(props.getSourceUrl(), props.getSourceUser(), props.getSourcePassword()));
    }

    @Bean
    public JdbcTemplate targetJdbcTemplate(ArchiveProperties props) {
        return new JdbcTemplate(hikari(props.getTargetUrl(), props.getTargetUser(), props.getTargetPassword()));
    }

    private HikariDataSource hikari(String url, String user, String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setMaximumPoolSize(2);
        ds.setInitializationFailTimeout(-1);
        return ds;
    }
}
