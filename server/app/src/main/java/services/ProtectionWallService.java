package services;

import io.grpc.stub.StreamObserver;

// interactors

// proto/common
import enchantedtowers.common.utils.proto.common.ProtectionWallModel;
// requests
import enchantedtowers.common.utils.proto.requests.ProtectionWallsRequest;
import enchantedtowers.common.utils.proto.requests.ProtectionWallSetupRequest;
// responses
import enchantedtowers.common.utils.proto.responses.ProtectionWallsAggregationResponse;
// services
import enchantedtowers.common.utils.proto.services.ProtectionWallServiceGrpc;



public class ProtectionWallService extends ProtectionWallServiceGrpc.ProtectionWallServiceImplBase {
    @Override
    public void getTowerProtectionWalls(ProtectionWallsRequest request,
                                        StreamObserver<ProtectionWallsAggregationResponse> responseObserver) {

    }

    @Override
    public void setUpProtectionWallOnTower(ProtectionWallSetupRequest request,
                                           StreamObserver<ProtectionWallModel> responseObserver) {

    }
}
