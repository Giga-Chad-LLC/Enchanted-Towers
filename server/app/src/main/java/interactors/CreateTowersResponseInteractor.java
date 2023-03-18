package interactors;

import java.util.List;
import java.util.ArrayList;

// game_models
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.game_models.Tower;
// game_models.utils
import enchantedtowers.game_models.utils.Point;
// proto
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;



// TODO: create ResponseInteractor interface and implement it
public class CreateTowersResponseInteractor {
    private final List<Tower> storedTowers;

    final static double MAX_DISTANCE = 2000;
    public CreateTowersResponseInteractor() {
        storedTowers = new ArrayList<>();
        storedTowers.add(new Tower(1, Tower.TowerType.CASTLE, new Point(1, 1)));
        storedTowers.add(new Tower(2, Tower.TowerType.FORTRESS, new Point(2, 3)));
        storedTowers.add(new Tower(3, Tower.TowerType.HUT, new Point(5, 12)));
    }

    private static boolean isInsideRequiredArea(double x, double x0, double y, double y0) {
        double dx = x - x0;
        double dy = y - y0;
        return dx * dx + dy * dy <= MAX_DISTANCE * MAX_DISTANCE;
    }

    public TowersAggregationResponse execute(PlayerCoordinatesRequest request) {
        double playerX = request.getLocation().getX();
        double playerY = request.getLocation().getY();

        List<TowerResponse> towers = new ArrayList<>();

        for (Tower tower : storedTowers) {
            double towerX = tower.getPosition().getX();
            double towerY = tower.getPosition().getY();

            if (isInsideRequiredArea(playerX, towerX, playerY, towerY)) {
                TowerResponse.Builder towerBuilder = TowerResponse.newBuilder();
                // setting coordinates
                towerBuilder.getLocationBuilder().setX(towerX).setY(towerY);
                towers.add(towerBuilder.build());
            }
        }

        TowersAggregationResponse response = TowersAggregationResponse
                .newBuilder()
                .addAllTowers(towers)
                .build();

        return response;
    }
}
