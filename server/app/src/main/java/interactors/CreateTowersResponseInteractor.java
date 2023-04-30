package interactors;

import java.util.List;
import java.util.ArrayList;

// game-models
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.game_models.Tower;

// proto
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.game_models.utils.Vector2;


// TODO: create ResponseInteractor interface and implement it
public class CreateTowersResponseInteractor {
    private final List<Tower> storedTowers;

    final static double MAX_DISTANCE = 2000;
    public CreateTowersResponseInteractor() {
        storedTowers = new ArrayList<>();
        storedTowers.add(new Tower(0, new Vector2(1, 1)));
        storedTowers.add(new Tower(1, new Vector2(2, 3)));
        storedTowers.add(new Tower(2, new Vector2(5, 12)));
    }

    private static boolean isInsideRequiredArea(double x, double x0, double y, double y0) {
        double dx = x - x0;
        double dy = y - y0;
        return dx * dx + dy * dy <= MAX_DISTANCE * MAX_DISTANCE;
    }

    public TowersAggregationResponse execute(PlayerCoordinatesRequest request) {
        double playerX = request.getX();
        double playerY = request.getY();

        List<TowerResponse> towers = new ArrayList<>();

        for (Tower tower : storedTowers) {
            Vector2 position = tower.getPosition();

            if (isInsideRequiredArea(playerX, position.x, playerY, position.y)) {
                TowerResponse towerResponse = TowerResponse.newBuilder()
                        .setX(position.x)
                        .setY(position.y)
                        .build();

                towers.add(towerResponse);
            }
        }

        TowersAggregationResponse response = TowersAggregationResponse
                .newBuilder()
                .addAllTowers(towers)
                .build();

        return response;
    }
}
