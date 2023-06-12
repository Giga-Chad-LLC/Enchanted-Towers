package components.utils;

import components.config.Config;
import io.jsonwebtoken.*;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtTokenUtils {
    private static final long EXPIRATION_TIMEOUT_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    public static String generateJWSToken(@NonNull String payload) {
        SecretKey key = Config.getSecretKey();
        return Jwts.builder()
                .setSubject(payload)
                .setExpiration(getExpirationDate())
                .signWith(key).compact();
    }

    public static String validate(String jws) throws JwtException {
        if (jws == null) {
            throw new JwtException("No token provided");
        }

        JwtParserBuilder parserBuilder = Jwts.parserBuilder().setSigningKey(Config.getSecretKey());
        Jws<Claims> jwsClaims = parserBuilder.build().parseClaimsJws(jws);
        Claims claims = jwsClaims.getBody();

        if (isExpired(claims.getExpiration())) {
            throw new ExpiredJwtException(jwsClaims.getHeader(), claims, "JwtToken expired: " + jws);
        }

        return claims.getSubject();
    }

    private static Date getExpirationDate() {
        return new Date(System.currentTimeMillis() + EXPIRATION_TIMEOUT_MS);
    }

    private static boolean isExpired(Date expirationTime) {
        return expirationTime.before(new Date());
    }
}
