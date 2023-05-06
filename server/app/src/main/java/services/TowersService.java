package services;

import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import interactors.CreateTowersResponseInteractor;
import io.grpc.stub.StreamObserver;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    @Override
    public void getTowers(Empty emptyRequest, StreamObserver<TowersAggregationResponse> responseObserver) {
        System.out.println("TowersService::getTowers");

        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();
        TowersAggregationResponse response = interactor.execute();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
