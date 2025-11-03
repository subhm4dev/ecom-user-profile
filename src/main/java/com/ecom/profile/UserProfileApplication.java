package com.ecom.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ecom.profile.config.JwtConfig;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // For JWKS cache refresh
@EnableConfigurationProperties(JwtConfig.class)
public class UserProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserProfileApplication.class, args);
    }
}

