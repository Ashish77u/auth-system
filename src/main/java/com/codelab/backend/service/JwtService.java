package com.codelab.backend.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${application.jwt.secret}")
    private String secretKey;

    @Value("${application.jwt.expiration}")
    private long jwtExpiration;

    // ── Public API ─────────────────────────────────────────────

    // Generate a token with just the user's email as subject
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // Generate a token with extra claims (e.g. role, userId)
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())   // username = email in our User entity
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Is this token valid for this user and not expired?
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // Extract the email (subject) from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ── Private helpers ────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic claim extractor — pass any function to pull out what you need
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Decode the hex secret from application.yml into a usable signing key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}


/*


***  JWTService is responsible for generating and validating JWT tokens. It uses the secret key defined in application.yml to sign the tokens, and it can extract claims (like the username/email) from the token. This service is used during login to create a token for the user, and during authentication to validate incoming tokens and extract user information.


*** step by step JWTService kaise kaam karta hai:

1. Generate karega token: Jab user login karega, toh hum JWTService ke generateToken method ko call karenge, jisme userDetails pass karenge. Ye method ek JWT token create karega jisme user's email (username) as subject hoga, aur optional extra claims bhi add kar sakte hain (jaise role, userId).
2. Validate karega token: Jab user koi protected endpoint access karega, toh hum JWTService ke isTokenValid method ko call karenge, jisme token aur userDetails pass karenge. Ye method check karega ki token valid hai (signature sahi hai) aur expired nahi hai.
3. Extract karega username: Agar humein token se email extract karna hai, toh hum JWTService ke extractUsername method ko call karenge, jisme token pass karenge. Ye method token se subject claim (email) ko extract karega.

*** claims kya hote hain JWT mein?
JWT mein claims are key-value pairs that provide additional information about the token. Claims can include things like the user's role, userId, expiration date, etc. In our case, we at least have the "sub" claim which contains the user's email. We can also add custom claims when generating the token.




*/