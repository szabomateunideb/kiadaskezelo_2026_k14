package hu.unideb.inf.kiadaskezelo.service.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ser.jackson.JacksonSerializableSerializer;


import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class TokenService {

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode("hkgjkfgjsdjkfsklfjesklfjslkefjlsejfsfnsfnfjxvxfjkvnxdjkvnyxdkjvnxjkdvnxjkdvnxdjkvnxdjk");
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token,
                               Function<Claims, T> claimsResolver) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final Claims claims = Jwts.parser()
                .json(new JacksonDeserializer<>(objectMapper))

                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private Boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    public String generateToken(UserDetails user) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Map<String, Object> claims = new HashMap<>();
        user.getAuthorities().forEach(a ->
                claims.put(a.getAuthority(),a));

        return Jwts.builder()
                .json(new JacksonSerializer<>(objectMapper))
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+150000))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Boolean validateToken(String token, UserDetails user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername())
                && !isTokenExpired(token));
    }
}
