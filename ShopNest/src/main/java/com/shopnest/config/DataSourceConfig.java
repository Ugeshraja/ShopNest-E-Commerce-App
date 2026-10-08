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
 * Production-ready DataSource Configuration supporting Render, Aiven, Supabase, Neon, Railway
 * and local environments.
 *
 * Automatically converts postgres:// and postgresql:// URIs into valid JDBC URLs,
 * extracts and decodes credentials, enforces sslmode=require for cloud hosts,
 * and outputs clear diagnostic logs to help troubleshoot connectivity.
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${INTERNAL_DATABASE_URL:#{null}}")
    private String internalDatabaseUrl;

    @Value("${EXTERNAL_DATABASE_URL:#{null}}")
    private String externalDatabaseUrl;

    @Value("${SPRING_DATASOURCE_URL:#{null}}")
    private String springDatasourceUrl;

    @Value("${POSTGRES_URL:#{null}}")
    private String postgresUrl;

    @Value("${DB_URL:#{null}}")
    private String dbUrl;

    @Value("${DB_HOST:#{null}}")
    private String dbHost;

    @Value("${DB_PORT:#{null}}")
    private String dbPort;

    @Value("${DB_NAME:#{null}}")
    private String dbName;

    @Value("${spring.datasource.username:#{null}}")
    private String springDatasourceUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String springDatasourcePassword;

    @Value("${DB_USER:#{null}}")
    private String envDbUser;

    @Value("${DB_PASSWORD:#{null}}")
    private String envDbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        String rawUrl = null;
        String source = null;

        // 1. Check all possible cloud database URL environment variables in priority order
        if (StringUtils.hasText(databaseUrl)) {
            rawUrl = databaseUrl.trim();
            source = "DATABASE_URL";
        } else if (StringUtils.hasText(internalDatabaseUrl)) {
            rawUrl = internalDatabaseUrl.trim();
            source = "INTERNAL_DATABASE_URL";
        } else if (StringUtils.hasText(externalDatabaseUrl)) {
            rawUrl = externalDatabaseUrl.trim();
            source = "EXTERNAL_DATABASE_URL";
        } else if (StringUtils.hasText(springDatasourceUrl)) {
            rawUrl = springDatasourceUrl.trim();
            source = "SPRING_DATASOURCE_URL / spring.datasource.url";
        } else if (StringUtils.hasText(postgresUrl)) {
            rawUrl = postgresUrl.trim();
            source = "POSTGRES_URL";
        } else if (StringUtils.hasText(dbUrl)) {
            rawUrl = dbUrl.trim();
            source = "DB_URL";
        } else if (StringUtils.hasText(dbHost) && StringUtils.hasText(dbName)) {
            // Built from individual parts (DB_HOST, DB_NAME, etc.)
            String port = StringUtils.hasText(dbPort) ? dbPort.trim() : "5432";
            rawUrl = "jdbc:postgresql://" + dbHost.trim() + ":" + port + "/" + dbName.trim() + "?sslmode=require";
            source = "DB_HOST + DB_NAME";
        }

        String username = StringUtils.hasText(springDatasourceUsername) ? springDatasourceUsername : envDbUser;
        String password = StringUtils.hasText(springDatasourcePassword) ? springDatasourcePassword : envDbPassword;
        String finalJdbcUrl;

        if (StringUtils.hasText(rawUrl)) {
            // Case A: Cloud URI format (postgres:// or postgresql://)
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    URI dbUri = new URI(rawUrl);
                    String userInfo = dbUri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                        password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    } else if (userInfo != null) {
                        username = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                    }

                    String host = dbUri.getHost();
                    int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                    String path = dbUri.getPath();

                    StringBuilder jdbc = new StringBuilder();
                    jdbc.append("jdbc:postgresql://").append(host).append(":").append(port).append(path);

                    boolean isLocal = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
                    String query = dbUri.getQuery();

                    if (!isLocal) {
                        // Render and cloud PostgreSQL require sslmode=require
                        if (!StringUtils.hasText(query)) {
                            jdbc.append("?sslmode=require");
                        } else if (!query.contains("sslmode=")) {
                            jdbc.append("?").append(query).append("&sslmode=require");
                        } else {
                            jdbc.append("?").append(query);
                        }
                    } else if (StringUtils.hasText(query)) {
                        jdbc.append("?").append(query);
                    }

                    finalJdbcUrl = jdbc.toString();
                    config.setDriverClassName("org.postgresql.Driver");
                } catch (Exception e) {
                    log.error("Failed to parse database URI from {}: {}", source, e.getMessage(), e);
                    throw new IllegalStateException("Failed to parse database URL from " + source + ": " + e.getMessage(), e);
                }
            } else if (rawUrl.startsWith("jdbc:postgresql:")) {
                // Case B: JDBC PostgreSQL URL
                boolean isLocal = rawUrl.contains("localhost") || rawUrl.contains("127.0.0.1");
                if (!isLocal && !rawUrl.contains("sslmode=")) {
                    rawUrl = rawUrl.contains("?") ? rawUrl + "&sslmode=require" : rawUrl + "?sslmode=require";
                }
                finalJdbcUrl = rawUrl;
                config.setDriverClassName("org.postgresql.Driver");
            } else if (rawUrl.startsWith("jdbc:mysql:")) {
                // Case C: JDBC MySQL URL
                finalJdbcUrl = rawUrl;
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else {
                finalJdbcUrl = "jdbc:" + rawUrl;
            }
        } else {
            // Case D: Fallback to localhost
            source = "LOCAL FALLBACK (No env var found)";
            finalJdbcUrl = "jdbc:postgresql://localhost:5432/shopnest";
            if (!StringUtils.hasText(username)) username = "postgres";
            if (!StringUtils.hasText(password)) password = "root";
            config.setDriverClassName("org.postgresql.Driver");
        }

        config.setJdbcUrl(finalJdbcUrl);
        if (StringUtils.hasText(username)) config.setUsername(username);
        if (StringUtils.hasText(password)) config.setPassword(password);

        // Print prominent diagnostic logs to Render console
        log.info("==================================================================");
        log.info("DATABASE CONFIGURATION ACTIVE:");
        log.info("  Source      : {}", source);
        log.info("  JDBC URL    : {}", maskUrl(finalJdbcUrl));
        log.info("  DB User     : {}", username != null ? username : "(none)");
        if (finalJdbcUrl.contains("localhost") || finalJdbcUrl.contains("127.0.0.1")) {
            log.warn("  ATTENTION   : Connecting to localhost. If running on Render, set DATABASE_URL in the Environment tab!");
        }
        log.info("==================================================================");

        // HikariCP connection pool settings tuned for cloud databases
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(60000); // 60s to allow idle cloud DBs to wake up
        config.setValidationTimeout(5000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("==================================================================");
            log.error("DATABASE CONNECTION ERROR: Failed to connect to database!");
            log.error("  Target URL : {}", maskUrl(finalJdbcUrl));
            log.error("  Details    : {}", e.getMessage());
            log.error("==================================================================");
            throw e;
        }
    }

    private String maskUrl(String url) {
        if (url == null) return null;
        // Hide password if embedded in URL
        return url.replaceAll(":[^:@/]+@", ":****@");
    }
}
