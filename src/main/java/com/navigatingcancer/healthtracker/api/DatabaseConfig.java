package com.navigatingcancer.healthtracker.api;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.navigatingcancer.healthtracker.api.data.model.CheckIn;
import com.navigatingcancer.healthtracker.api.data.model.HealthTrackerEvent;
import com.navigatingcancer.healthtracker.api.data.model.ProReview;
import com.navigatingcancer.healthtracker.api.data.model.survey.SurveyInstance;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
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

  @Bean
  MongoTemplate mongoTemplate(MongoDatabaseFactory databaseFactory) {
    var mongoTemplate = new MongoTemplate(databaseFactory);
    initIndices(mongoTemplate);
    return mongoTemplate;
  }

  private static final List<Class<?>> AUTO_INDEXABLE_TYPES =
      List.of(HealthTrackerEvent.class, SurveyInstance.class, ProReview.class, CheckIn.class);

  private void initIndices(MongoTemplate mongoTemplate) {
    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext =
        mongoTemplate.getConverter().getMappingContext();

    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    for (var type : AUTO_INDEXABLE_TYPES) {
      log.info("Auto-initializing indices for type {}", type);
      IndexOperations indexOps = mongoTemplate.indexOps(type);
      for (var indexDefinition : resolver.resolveIndexFor(type)) {
        log.info("Ensuring index {}", indexDefinition);
        indexOps.ensureIndex(indexDefinition);
      }
    }
  }
}
