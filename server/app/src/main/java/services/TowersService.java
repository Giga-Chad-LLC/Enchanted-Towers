package services;

import components.mediator.TowersUpdatesMediator;
import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import interactors.CreateTowersResponseInteractor;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.logging.Logger;


public class TowersService extends TowersServiceGrpc.TowersServiceImplBase {
    private final static Logger logger = Logger.getLogger(TowersService.class.getName());

    @Override
    public void getTowers(Empty emptyRequest, StreamObserver<TowersAggregationResponse> responseObserver) {
        logger.info("getTowers requested");
        CreateTowersResponseInteractor interactor = new CreateTowersResponseInteractor();
        TowersAggregationResponse response = interactor.createResponseWithAllTowers();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listenTowersUpdates(Empty emptyRequest, StreamObserver<TowersAggregationResponse> streamObserver) {
        logger.info("listenTowersUpdates requested: registering stream observer");
        // create cancel handler to hook the event of client closing the connection
        var callObserver = (ServerCallStreamObserver<TowersAggregationResponse>) streamObserver;
        // `setOnCancelHandler` must be called before any `onNext` calls
        callObserver.setOnCancelHandler(() -> onClientStreamCancellation(streamObserver));

        // store client's response observer in mediator
        TowersUpdatesMediator.getInstance().registerObserver(streamObserver);
    }

    private void onClientStreamCancellation(StreamObserver<TowersAggregationResponse> streamObserver) {
        logger.info("onClientStreamCancellation: client disconnected, removing observer...");
        TowersUpdatesMediator.getInstance().removeObserver(streamObserver);
    }
}
