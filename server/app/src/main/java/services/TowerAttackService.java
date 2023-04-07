package services;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// components
import components.AttackSession;
// requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
// responses
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.GameError;
// services
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;

import javax.swing.text.html.Option;


public class TowerAttackService extends TowerAttackServiceGrpc.TowerAttackServiceImplBase {
    private final List<AttackSession> sessions = new ArrayList<>();

    @Override
    public void attackTowerById(TowerAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: if player attacks several towers simultaneously?
        // TODO: check whether player already has an attack session
        sessions.add(AttackSession.fromRequest(request));
    }

    @Override
    public void leaveAttack(TowerAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        final int playerId = request.getPlayerData().getPlayerId();
        Optional<AttackSession> session = getAttackSessionByPlayerId(playerId);

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        if (session.isEmpty()) {
            // session not found -> error
            responseBuilder.setSuccess(false);
            responseBuilder.getErrorBuilder()
                    .setHasError(true)
                    .setType(GameError.ErrorType.ATTACK_SESSION_NOT_FOUND)
                    .setMessage("Attack session associated with player id of '" + playerId + "' not found")
                    .build();
        }
        else {
            // removing session
            sessions.remove(session.get());
            responseBuilder.setSuccess(true);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private Optional<AttackSession> getAttackSessionByPlayerId(int playerId) {
        for (var session : sessions) {
            if (session.getAttackingPlayerId() == playerId) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    /*
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
    */
}
