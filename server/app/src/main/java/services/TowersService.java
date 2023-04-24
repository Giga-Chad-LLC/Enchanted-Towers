package services;

import io.grpc.stub.StreamObserver;

// interactors
import interactors.CreateTowersResponseInteractor;
// requests
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
// responses
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
// services
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    @Override
    public void getTowersCoordinates(PlayerCoordinatesRequest request, StreamObserver<TowersAggregationResponse> responseObserver) {
        System.out.println("getTowersCoordinates: player cords: [x=" + request.getX() + ", y=" + request.getY() + "]");

        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();
        TowersAggregationResponse response = interactor.execute(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
