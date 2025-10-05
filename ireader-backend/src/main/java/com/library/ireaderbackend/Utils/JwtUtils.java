package com.library.ireaderbackend.Utils;
import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtils {
    // 正确生成符合HS256要求的256位（32字节）密钥
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    // 令牌有效期（24小时）
    private static final long EXPIRATION = 24 * 60 * 60 * 1000;

    // 生成令牌（传入用户ID）
    public String generateToken(Long userId) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION);
        return Jwts.builder()
                .setSubject(userId.toString()) // 存储用户ID
                .setExpiration(expirationDate)
                .signWith(SECRET_KEY) // 使用SecretKey对象签名
                .compact();
    }

    // 从令牌中解析用户ID
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY) // 使用SecretKey对象验证
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    // 验证令牌是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY) // 使用SecretKey对象验证
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}