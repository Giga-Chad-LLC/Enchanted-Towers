package services;

import io.grpc.stub.StreamObserver;

// interactors
import interactors.CreateTowersResponseInteractor;
import interactors.AttackTowerResponseInteractor;
// requests
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
// responses
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.responses.AttackTowerResponse;
// services
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    @Override
    public void getTowersCoordinates(PlayerCoordinatesRequest request, StreamObserver<TowersAggregationResponse> responseObserver) {
        System.out.println("getTowersCoordinates:\tPlayerCoordinatesRequest[x=" + request.getX() + ", y=" + request.getY() + "]");

        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();

        TowersAggregationResponse response = interactor.execute(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void attackTower(TowerAttackRequest request, StreamObserver<AttackTowerResponse> responseObserver) {
        System.out.println("attackTower:\tTowerAttackRequest[towerId=" + request.getTowerId()
                + ", playerX=" + request.getPlayerCoordinates().getX()
                + ", playerY=" + request.getPlayerCoordinates().getY() + "]");

        AttackTowerResponseInteractor interactor = new AttackTowerResponseInteractor();
        AttackTowerResponse response = interactor.execute(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
