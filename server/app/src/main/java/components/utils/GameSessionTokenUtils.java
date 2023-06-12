package components.utils;

import java.util.UUID;

public class GameSessionTokenUtils {
    public static String generateGameSessionToken() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
