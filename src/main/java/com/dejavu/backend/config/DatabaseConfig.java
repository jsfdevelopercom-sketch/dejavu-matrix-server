package com.dejavu.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${spring.datasource.url:jdbc:h2:file:./data/dejavudb;DB_CLOSE_DELAY=-1}")
    private String defaultUrl;

    @Value("${DB_USERNAME:sa}")
    private String defaultUsername;

    @Value("${DB_PASSWORD:}")
    private String defaultPassword;

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        if (databaseUrl != null && !databaseUrl.isEmpty() && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            URI dbUri = new URI(databaseUrl);
            
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

            return DataSourceBuilder.create()
                    .url(dbUrl)
                    .username(username)
                    .password(password)
                    .build();
        }

        return DataSourceBuilder.create()
                .url(defaultUrl)
                .username(defaultUsername)
                .password(defaultPassword)
                .build();
    }
}
