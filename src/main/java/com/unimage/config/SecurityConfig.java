package com.unimage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/screenshot/upload").authenticated()
            .requestMatchers(HttpMethod.GET, "/screenshot/all").authenticated()
            .requestMatchers(HttpMethod.PUT, "/screenshot/modify").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/screenshot/delete").authenticated()
            .requestMatchers(HttpMethod.GET, "/screenshot/{filename}").authenticated()
            .anyRequest().denyAll()
        )
        .oauth2Login(oauth -> oauth.successHandler((request, response, authentication) -> {
        }));

    return http.build();
  }
}
