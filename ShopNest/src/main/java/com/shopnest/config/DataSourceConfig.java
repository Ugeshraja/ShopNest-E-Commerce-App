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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready DataSource Configuration supporting Render, Aiven, Supabase, Neon, Railway
 * and local environments.
 *
 * Automatically converts postgres://, postgresql://, and jdbc:postgresql:// URIs into valid JDBC URLs,
 * strips embedded credentials from host authorities to avoid UnknownHostException,
 * enforces sslmode=require for cloud hosts, and outputs crystal-clear diagnostic logs.
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

    @Value("${spring.datasource.url:#{null}}")
    private String propDatasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:#{null}}")
    private String springDatasourceUsername;

    @Value("${spring.datasource.username:#{null}}")
    private String propDatasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:#{null}}")
    private String springDatasourcePassword;

    @Value("${spring.datasource.password:#{null}}")
    private String propDatasourcePassword;

    @Value("${SPRING_DATASOURCE_DRIVER_CLASS_NAME:#{null}}")
    private String springDatasourceDriver;

    @Value("${spring.datasource.driver-class-name:#{null}}")
    private String propDatasourceDriver;

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

        // Pick URL in priority order
        if (StringUtils.hasText(springDatasourceUrl)) {
            rawUrl = clean(springDatasourceUrl);
            source = "SPRING_DATASOURCE_URL";
        } else if (StringUtils.hasText(databaseUrl)) {
            rawUrl = clean(databaseUrl);
            source = "DATABASE_URL";
        } else if (StringUtils.hasText(internalDatabaseUrl)) {
            rawUrl = clean(internalDatabaseUrl);
            source = "INTERNAL_DATABASE_URL";
        } else if (StringUtils.hasText(externalDatabaseUrl)) {
            rawUrl = clean(externalDatabaseUrl);
            source = "EXTERNAL_DATABASE_URL";
        } else if (StringUtils.hasText(propDatasourceUrl)) {
            rawUrl = clean(propDatasourceUrl);
            source = "spring.datasource.url";
        }

        String username = firstNonEmpty(clean(springDatasourceUsername), clean(propDatasourceUsername), clean(envDbUser));
        String password = firstNonEmpty(clean(springDatasourcePassword), clean(propDatasourcePassword), clean(envDbPassword));
        String driver = firstNonEmpty(clean(springDatasourceDriver), clean(propDatasourceDriver));

        String finalJdbcUrl;
        String targetHost = "unknown";
        String targetPort = "unknown";
        String targetDb = "unknown";

        if (StringUtils.hasText(rawUrl)) {
            // Regex to parse any variant: [jdbc:][postgres|postgresql]://[user:pass@]host[:port][/db][?query]
            Pattern credPattern = Pattern.compile("^(?:jdbc:)?(?:postgres(?:ql)?)://(?:([^:@/]+)(?::([^@/]*))?@)?([^:/]+)(?::(\\d+))?(/[^?#]*)?(?:\\?(.*))?$");
            Matcher m = credPattern.matcher(rawUrl);

            if (m.matches()) {
                String uriUser = m.group(1);
                String uriPass = m.group(2);
                targetHost = m.group(3);
                targetPort = m.group(4) != null ? m.group(4) : "5432";
                targetDb = m.group(5) != null ? m.group(5) : "/shopnest";
                String query = m.group(6);

                if (uriUser != null && !StringUtils.hasText(username)) {
                    username = urlDecode(uriUser);
                }
                if (uriPass != null && !StringUtils.hasText(password)) {
                    password = urlDecode(uriPass);
                }

                StringBuilder jdbc = new StringBuilder();
                jdbc.append("jdbc:postgresql://").append(targetHost).append(":").append(targetPort).append(targetDb);

                boolean isLocal = "localhost".equalsIgnoreCase(targetHost) || "127.0.0.1".equals(targetHost);
                if (!isLocal) {
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
                if (!StringUtils.hasText(driver)) driver = "org.postgresql.Driver";
            } else if (rawUrl.startsWith("jdbc:h2:")) {
                finalJdbcUrl = rawUrl;
                if (!StringUtils.hasText(driver)) driver = "org.h2.Driver";
                if (!StringUtils.hasText(username)) username = "sa";
                if (!StringUtils.hasText(password)) password = "";
            } else if (rawUrl.startsWith("jdbc:mysql:")) {
                finalJdbcUrl = rawUrl;
                if (!StringUtils.hasText(driver)) driver = "com.mysql.cj.jdbc.Driver";
            } else if (rawUrl.startsWith("jdbc:")) {
                finalJdbcUrl = rawUrl;
                if (rawUrl.startsWith("jdbc:postgresql:") && !rawUrl.contains("localhost") && !rawUrl.contains("127.0.0.1") && !rawUrl.contains("sslmode=")) {
                    finalJdbcUrl = rawUrl.contains("?") ? rawUrl + "&sslmode=require" : rawUrl + "?sslmode=require";
                }
                if (!StringUtils.hasText(driver)) driver = "org.postgresql.Driver";
            } else {
                finalJdbcUrl = "jdbc:" + rawUrl;
                if (!StringUtils.hasText(driver)) driver = "org.postgresql.Driver";
            }
        } else {
            source = "LOCAL IN-MEMORY H2 DATABASE";
            targetHost = "in-memory (H2)";
            targetPort = "N/A";
            targetDb = "shopnest";
            finalJdbcUrl = "jdbc:h2:mem:shopnest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL";
            if (!StringUtils.hasText(username)) username = "sa";
            if (!StringUtils.hasText(password)) password = "";
            driver = "org.h2.Driver";
        }

        config.setJdbcUrl(finalJdbcUrl);
        if (StringUtils.hasText(username)) config.setUsername(username);
        if (StringUtils.hasText(password)) config.setPassword(password);
        if (StringUtils.hasText(driver)) config.setDriverClassName(driver);

        log.info("==================================================================");
        log.info("DATABASE CONFIGURATION ACTIVE:");
        log.info("  Config Source: {}", source);
        log.info("  JDBC URL     : {}", maskUrl(finalJdbcUrl));
        log.info("  Host         : {}", targetHost);
        log.info("  DB Username  : {}", username != null ? username : "(none)");
        log.info("  Driver Class : {}", driver);
        if ("localhost".equalsIgnoreCase(targetHost) || "127.0.0.1".equals(targetHost)) {
            log.warn("  WARNING: Connecting to localhost. Ensure DATABASE_URL or SPRING_DATASOURCE_URL is set in Render Environment!");
        }
        log.info("==================================================================");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(60000);
        config.setValidationTimeout(5000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            StringBuilder errorReport = new StringBuilder();
            errorReport.append("\n==================================================================\n");
            errorReport.append("CRITICAL: DATABASE CONNECTION FAILED!\n");
            errorReport.append("  Target JDBC URL: ").append(maskUrl(finalJdbcUrl)).append("\n");
            errorReport.append("  Target Host    : ").append(targetHost).append("\n");
            errorReport.append("  Target Port    : ").append(targetPort).append("\n");
            errorReport.append("  Database Name  : ").append(targetDb).append("\n");
            errorReport.append("  Username       : ").append(username != null ? username : "(none)").append("\n");
            errorReport.append("  Error Chain    :\n");
            Throwable curr = e;
            while (curr != null) {
                errorReport.append("    -> [").append(curr.getClass().getSimpleName()).append("] ").append(curr.getMessage()).append("\n");
                curr = curr.getCause();
            }

            if (targetHost != null && targetHost.matches("dpg-[a-z0-9]+(-a)?")) {
                errorReport.append("\n  DIAGNOSIS:\n");
                errorReport.append("  Host '").append(targetHost).append("' is a Render Internal Database hostname.\n");
                errorReport.append("  Internal URLs only resolve if your Web Service and Database are in the SAME Render Region!\n");
                errorReport.append("  -> FIX: In Render, go to your Postgres database, copy the 'External Database URL',\n");
                errorReport.append("     and update SPRING_DATASOURCE_URL (or DATABASE_URL) in your Web Service Environment tab.\n");
            } else if ("localhost".equalsIgnoreCase(targetHost) || "127.0.0.1".equals(targetHost)) {
                errorReport.append("\n  DIAGNOSIS:\n");
                errorReport.append("  The application attempted to connect to localhost:5432, which does not exist in Render.\n");
                errorReport.append("  -> FIX: Provide your remote Postgres connection string in your Render Environment tab.\n");
            }

            errorReport.append("==================================================================\n");
            log.error(errorReport.toString());
            throw new RuntimeException(errorReport.toString(), e);
        }
    }

    private String clean(String val) {
        if (!StringUtils.hasText(val)) return null;
        String trimmed = val.trim();
        // Remove surrounding quotes if accidentally entered in Render UI
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String urlDecode(String val) {
        try {
            return URLDecoder.decode(val, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return val;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private String maskUrl(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^:@/]+@", ":****@");
    }
}
