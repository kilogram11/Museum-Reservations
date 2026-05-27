package com.museum.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 管理端和小程序端都使用（但可以设置不同的过期时间）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:museum_secret_key_2025_must_be_very_long_for_hs256_security}")
    private String secret;

    @Value("${jwt.admin-expiration:86400000}") // 管理端24小时
    private Long adminExpiration;

    @Value("${jwt.app-expiration:2592000000}") // 小程序端30天
    private Long appExpiration;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成管理端Token
     */
    public String generateAdminToken(String adminId) {
        return generateToken(adminId, adminExpiration);
    }

    /**
     * 生成小程序端Token
     */
    public String generateAppToken(String userId) {
        return generateToken(userId, appExpiration);
    }

    /**
     * 生成Token（通用方法）
     */
    private String generateToken(String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("timestamp", now.getTime());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Token获取用户ID（adminId或userId）
     */
    public String getSubjectFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}