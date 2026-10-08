package com.shopnest.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Robust DataSource Configuration supporting cloud deployments (Render, Supabase, Neon, Railway)
 * as well as local development environments.
 *
 * Automatically converts cloud-standard DATABASE_URL (postgres:// or postgresql://)
 * into a valid JDBC connection URL and extracts embedded credentials.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${spring.datasource.url:#{null}}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String springDatasourceUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String springDatasourcePassword;

    @Value("${spring.datasource.driver-class-name:#{null}}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String rawUrl = databaseUrl;
        if (!StringUtils.hasText(rawUrl)) {
            rawUrl = springDatasourceUrl;
        }

        if (StringUtils.hasText(rawUrl)) {
            rawUrl = rawUrl.trim();

            // Cloud format: postgres://user:password@host:port/database or postgresql://...
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    URI dbUri = new URI(rawUrl);
                    String userInfo = dbUri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        config.setUsername(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                        config.setPassword(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    } else if (userInfo != null) {
                        config.setUsername(URLDecoder.decode(userInfo, StandardCharsets.UTF_8));
                    }

                    String host = dbUri.getHost();
                    int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                    String path = dbUri.getPath();

                    StringBuilder jdbcUrl = new StringBuilder();
                    jdbcUrl.append("jdbc:postgresql://").append(host).append(":").append(port).append(path);

                    if (StringUtils.hasText(dbUri.getQuery())) {
                        jdbcUrl.append("?").append(dbUri.getQuery());
                    }

                    config.setJdbcUrl(jdbcUrl.toString());
                    config.setDriverClassName("org.postgresql.Driver");
                    log.info("Constructed PostgreSQL JDBC connection to host: {}, port: {}, database: {}", host, port, path);
                } catch (Exception e) {
                    log.error("Failed to parse DATABASE_URL URI: {}", e.getMessage(), e);
                    throw new IllegalStateException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
                }
            } else if (rawUrl.startsWith("jdbc:")) {
                config.setJdbcUrl(rawUrl);
                if (StringUtils.hasText(springDatasourceUsername)) {
                    config.setUsername(springDatasourceUsername);
                }
                if (StringUtils.hasText(springDatasourcePassword)) {
                    config.setPassword(springDatasourcePassword);
                }

                if (StringUtils.hasText(driverClassName)) {
                    config.setDriverClassName(driverClassName);
                } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                    config.setDriverClassName("org.postgresql.Driver");
                } else if (rawUrl.startsWith("jdbc:mysql:")) {
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                }
            } else {
                config.setJdbcUrl("jdbc:" + rawUrl);
                if (StringUtils.hasText(springDatasourceUsername)) {
                    config.setUsername(springDatasourceUsername);
                }
                if (StringUtils.hasText(springDatasourcePassword)) {
                    config.setPassword(springDatasourcePassword);
                }
            }
        } else {
            // Default fallback
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/shopnest");
            config.setUsername(StringUtils.hasText(springDatasourceUsername) ? springDatasourceUsername : "postgres");
            config.setPassword(StringUtils.hasText(springDatasourcePassword) ? springDatasourcePassword : "root");
            config.setDriverClassName("org.postgresql.Driver");
        }

        // Hikari Connection Pool Settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
