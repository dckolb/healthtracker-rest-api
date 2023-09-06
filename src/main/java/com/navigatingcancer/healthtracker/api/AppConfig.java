package com.navigatingcancer.healthtracker.api;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@ComponentScan(
    basePackages = {"com.navigatingcancer.healthtracker", "com.navigatingcancer.notification"})
@EnableWebSecurity
public class AppConfig extends WebSecurityConfigurerAdapter {
  private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

  @Value(value = "${auth.enabled}")
  private boolean authEnabled = true;

  @Value(value = "${auth0.apiAudience}")
  private String apiAudience;

  @Value(value = "${auth0.issuer}")
  private String issuer;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.info("audience {} issuer {}", apiAudience, issuer);
    http.cors();
    if (authEnabled == false) {
      JwtWebSecurityConfigurer.forRS256(apiAudience, issuer)
          .configure(http)
          .authorizeRequests()
          .antMatchers("/**")
          .permitAll();
    } else {
      JwtWebSecurityConfigurer.forRS256(apiAudience, issuer)
          .configure(http)
          .authorizeRequests()
          .antMatchers("/actuator/**")
          .permitAll()
          .antMatchers("/callattempts")
          .authenticated()
          .antMatchers("/callattempts/**")
          .authenticated()
          .antMatchers("/checkins")
          .authenticated()
          .antMatchers("/checkins/**")
          .authenticated()
          .antMatchers("/enrollments")
          .authenticated()
          .antMatchers("/enrollments/**")
          .authenticated()
          .antMatchers("/status")
          .authenticated()
          .antMatchers("/status/**")
          .authenticated()
          .antMatchers("/survey")
          .authenticated()
          .antMatchers("/survey/**")
          .authenticated();
    }
  }
}
