package com.revplay.musicplatform.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationSeconds;
    private long refreshTokenExpirationSeconds;
}
