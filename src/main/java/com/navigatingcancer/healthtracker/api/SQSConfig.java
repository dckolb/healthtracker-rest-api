package com.navigatingcancer.healthtracker.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SQSConfig {
  @Bean
  AWSCredentialsProvider awsCredentialsProvider() {
    return new DefaultAWSCredentialsProviderChain();
  }
}
