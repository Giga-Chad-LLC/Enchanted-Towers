package services;

import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.registry.TowersRegistry;
import io.grpc.stub.StreamObserver;

import java.util.Optional;



public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {


    @Override
    public void captureTower(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void tryEnterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        /**
             if (now - tower.timestamp >= 24h || tower.timestamp == -1) {
                good;
             }
         */
    }

    @Override
    public void enterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<SessionInfoResponse> streamObserver) {}

    @Override
    public void addSpell(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void clearCanvas(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {}
}
