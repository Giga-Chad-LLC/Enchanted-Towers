package services;

import io.grpc.stub.StreamObserver;

// interactors
import interactors.CreateTowersResponseInteractor;
// proto
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;

import java.util.List;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    @Override
    public void getTowersCoordinates(PlayerCoordinatesRequest request, StreamObserver<TowerResponse> responseObserver) {
        System.out.println("Got request: Request[x=" + request.getX() + ", y=" + request.getY() + "]");

        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();

        List<TowerResponse> responses =  interactor.execute(request);

        for (TowerResponse response : responses) {
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
