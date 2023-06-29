package components.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;


public class Config {
    private final static SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    public static SecretKey getSecretKey() {
        return secretKey;
    }
}
