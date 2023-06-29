package components.utils;

import components.db.dao.GameSessionTokensDao;

import java.util.UUID;

public class GameSessionTokenUtils {
    public static String generateGameSessionToken() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static boolean isValid(String gameSessionToken) {
        GameSessionTokensDao dao = new GameSessionTokensDao();
        return dao.existsByToken(gameSessionToken);
    }
}
