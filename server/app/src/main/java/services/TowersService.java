package services;

import io.grpc.stub.StreamObserver;

// interactors
import interactors.CreateTowersResponseInteractor;
// proto
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    @Override
    public void getTowersCoordinates(PlayerCoordinatesRequest request, StreamObserver<TowersAggregationResponse> responseObserver) {
        System.out.println("Got request: Request[x=" + request.getX() + ", y=" + request.getY() + "]");

        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();

        TowersAggregationResponse response = interactor.execute(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
