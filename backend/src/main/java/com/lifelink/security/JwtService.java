package com.lifelink.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expirationMs;
    private final long rememberMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.remember-ms}") long rememberMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
        this.expirationMs = expirationMs;
        this.rememberMs = rememberMs;
    }

    public String generateToken(Long userId, String email, String role, boolean remember) {
        long lifetime = remember ? rememberMs : expirationMs;
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + lifetime))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs(boolean remember) {
        return remember ? rememberMs : expirationMs;
    }
}
