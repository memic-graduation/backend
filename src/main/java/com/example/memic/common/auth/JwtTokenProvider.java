package com.example.memic.common.auth;

import com.example.memic.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String SCHEME = "Bearer ";
    private final SecretKey secretKey;
    private final Long expired;

    public JwtTokenProvider(@Value("${jwt.key}") String secretKey, @Value("${jwt.expiration}") Long expired) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.expired = expired;
    }

    public String createAccessToken(Long id) {
        Instant issuedAt = Instant.now();
        return SCHEME + Jwts.builder()
                            .setExpiration(Date.from(issuedAt.plus(expired, ChronoUnit.MINUTES)))
                            .setIssuedAt(Date.from(issuedAt))
                            .setSubject(String.valueOf(id))
                            .signWith(secretKey, SignatureAlgorithm.HS256)
                            .compact();
    }

    public Long parseToken(String token) {
        if (!token.startsWith(SCHEME)) {
            throw new InvalidTokenException("유효하지 않은 토큰 타입입니다. 입력된 token : " + token);
        }
        String credentials = token.substring(SCHEME.length());
        JwtParser parser = Jwts.parserBuilder()
                               .setSigningKey(secretKey)
                               .build();
        Claims claims = parser.parseClaimsJws(credentials)
                              .getBody();

        return Long.parseLong(claims.getSubject());
    }
}
