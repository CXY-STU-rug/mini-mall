package com.minimall.minimall.common.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Date;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
@Component
public class JwtUtil {
    // ② 从 yml 读密钥（jwt.secret）
    @Value("${jwt.secret}")
    private String secret;

    // ③ 从 yml 读过期时间（jwt.expiration）
    @Value("${jwt.expiration}")
    private Long expiration;


    /**
     * 生成 token
     */
    public String generateToken(Long userId, String username) {

        // ④ 把业务数据放进 Map
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);       // 填 key 名
        claims.put("username", username);     // 填 key 名

        // ⑤ 用 Jwts.builder() 构建 token（这段照抄，是库的固定写法）
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())                                       // 签发时间 = 现在
                .expiration(new Date(System.currentTimeMillis() + expiration))// 过期时间 = 现在 + 过期毫秒
                .signWith(getSigningKey())
                .compact();
    }


    /**
     * 解析 token，返回 Claims（里面装着业务数据）
     */
    public Claims parseToken(String token) {
        // 库的固定写法，照抄
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * 从 token 提取 userId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);          // 调上面写好的方法
        return claims.get("userId", Long.class); // 填 key 名（和上面对应）
    }


    /**
     * 从 token 提取 username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }


    /**
     * 生成 SecretKey（密钥对象）—— 库的固定写法
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
