package com.revplay.musicplatform.security.service;

import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.entity.User;
import io.jsonwebtoken.Claims;

import java.time.Instant;

public interface JwtService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    Claims parseToken(String token);

    boolean isAccessToken(String token);

    boolean isRefreshToken(String token);

    AuthenticatedUserPrincipal toPrincipal(String token);

    Instant getExpiry(String token);
}


