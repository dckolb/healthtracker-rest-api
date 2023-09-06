package com.navigatingcancer.healthtracker.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.navigatingcancer.healthtracker.api.processor.model.HealthTrackerStatusCommand;
import com.navigatingcancer.sqs.SqsHelper;
import com.navigatingcancer.sqs.SqsProducer;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
  /**
   * Overides the default SqsHelper with a mock to prevent tests from attempting real SQS API calls.
   *
   * @return
   */
  @Bean
  @Primary
  public SqsHelper sqsHelper() {
    SqsHelper sqsHelper = Mockito.mock(SqsHelper.class);
    Mockito.doNothing().when(sqsHelper).startConsumer(Mockito.any(), Mockito.any());
    Mockito.doReturn("url").when(sqsHelper).getQueueUrl(Mockito.any());
    SqsProducer<HealthTrackerStatusCommand> mockProducer = Mockito.mock(SqsProducer.class);
    Mockito.doReturn(mockProducer).when(sqsHelper).createProducer(Mockito.any(), Mockito.any());
    return sqsHelper;
  }

  @Bean
  @Primary
  public AWSCredentialsProvider credentialsProvider() {
    return Mockito.mock(AWSCredentialsProvider.class);
  }

  @Bean
  public MongodConfig embeddedMongoConfig() {
    return MongodConfig.builder().version(Version.V3_6_5).build();
  }
}
