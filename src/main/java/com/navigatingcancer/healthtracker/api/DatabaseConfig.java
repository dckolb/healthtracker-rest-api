package com.navigatingcancer.healthtracker.api;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories
@RequiredArgsConstructor
@Slf4j
public class DatabaseConfig {
  @Bean
  public LoggingEventListener mongoEventListener() {
    return new LoggingEventListener();
  }

  /**
   * Custom mongo client construction for deployed environments to compose a connection string while
   * handling URL-escaping of the password. Only applied if the property `spring.data.mongodb.uri`
   * is not defined.
   *
   * @param mongoProps
   * @param environment
   * @return
   */
  @Bean
  @ConditionalOnExpression("'${spring.data.mongodb.uri:true}'.equals('true')")
  public MongoClientSettings mongoClientSettings(
      MongoProperties mongoProps, Environment environment) {
    log.info("Mongo connection uri: {}", mongoProps.getUri());

    String urlEncodedPassword =
        URLEncoder.encode(new String(mongoProps.getPassword()), StandardCharsets.UTF_8);
    String connectionString =
        String.format(
            "mongodb://%s:%s@%s:%s/%s?ssl=true&authSource=%s&replicaSet=rs0",
            mongoProps.getUsername(),
            urlEncodedPassword,
            mongoProps.getHost(),
            mongoProps.getPort(),
            mongoProps.getDatabase(),
            mongoProps.getAuthenticationDatabase());

    log.info(
        "Mongo connection string: {}",
        connectionString.replaceAll(urlEncodedPassword, "ENCODED_PASSWORD"));

    return MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(connectionString))
        .applyToConnectionPoolSettings(
            c -> {
              c.maxConnectionIdleTime(10, TimeUnit.MINUTES);
            })
        .build();
  }
}
