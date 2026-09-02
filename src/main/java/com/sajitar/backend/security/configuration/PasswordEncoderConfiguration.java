package com.sajitar.backend.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Getter;

@Configuration
public class PasswordEncoderConfiguration {

    @Getter(onMethod_ = @Bean)
    private final PasswordEncoder passwordEncoder;

    public PasswordEncoderConfiguration() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

}
