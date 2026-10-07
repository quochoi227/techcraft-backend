package com.techcraft.techcraftbackend.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.techcraft.techcraftbackend.entity.User;
import com.techcraft.techcraftbackend.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret:default-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256}")
    private String secret;

    @Value("${jwt.access-token-expiry-ms:3600000}")
    private long accessTokenExpiryMs;

    private byte[] getSigningKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Date issueTime = Date.from(now);
        Date expirationTime = Date.from(now.plusMillis(accessTokenExpiryMs));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        try {
            JWSSigner signer = new MACSigner(getSigningKey());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error signing JWT token: {}", e.getMessage(), e);
            throw new AppException("Không thể tạo mã xác thực JWT", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(getSigningKey());
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
        } catch (ParseException | JOSEException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public JWTClaimsSet getClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new AppException("Không thể đọc thông tin từ JWT token", HttpStatus.BAD_REQUEST);
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        try {
            return getClaims(token).getStringClaim("email");
        } catch (ParseException e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            return getClaims(token).getStringClaim("role");
        } catch (ParseException e) {
            return null;
        }
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }
}
