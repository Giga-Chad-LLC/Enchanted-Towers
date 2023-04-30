package services;

import io.grpc.stub.StreamObserver;

// requests
import enchantedtowers.common.utils.proto.requests.DrawnSpellRequest;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
// responses
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
// services
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;




public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {
    @Override
    public void captureTower(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void tryEnterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void enterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<SessionInfoResponse> streamObserver) {}

    @Override
    public void addSpell(DrawnSpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void clearCanvas(SessionIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {}
}
