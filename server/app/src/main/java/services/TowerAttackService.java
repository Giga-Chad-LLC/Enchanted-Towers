package services;

import enchantedtowers.common.utils.proto.requests.SpellRequest.RequestType;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

// components
import components.AttackSession;
// requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
// responses
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.GameError;
// services
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;



public class TowerAttackService extends TowerAttackServiceGrpc.TowerAttackServiceImplBase {
    private final List<AttackSession> sessions = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TowerAttackService.class.getName());

    @Override
    public void attackTowerById(TowerAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: if player attacks several towers simultaneously?
        // TODO: check whether player already has an attack session
        sessions.add(AttackSession.fromRequest(request));

        int playerId = request.getPlayerData().getPlayerId();
        int towerId = request.getTowerId();
        logger.info("attackTowerById: playerId=" + playerId + ", towerId=" + towerId);
        // System.out.println("attackTowerById: playerId=" + playerId + ", towerId=" + towerId);

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();
        responseBuilder.setSuccess(true);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void leaveAttack(TowerAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // System.out.println("leaveAttack: " + request.getPlayerData().getPlayerId());
        logger.info("leaveAttack: " + request.getPlayerData().getPlayerId());

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

    @Override
    public void selectSpellColor(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        // if request is invalid: either incorrect request type or spell color is not provided
        if (request.getRequestType() != SpellRequest.RequestType.SELECT_SPELL_COLOR || !request.hasSpellColor()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Invalid request: request type must be 'SELECT_SPELL_COLOR' and spell color must be provided");

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        var spellColorRequest = request.getSpellColor();

        int playerId = spellColorRequest.getPlayerData().getPlayerId();
        Optional<AttackSession> session = getAttackSessionByPlayerId(playerId);

        // if session not found
        if (session.isEmpty()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Attack session associated with player with id of " + playerId + " not found");

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        // set current color id
        session.get().setCurrentSpellColorId(spellColorRequest.getColorId());
        logger.info("Setting color id of '" + spellColorRequest.getColorId() + "'");

        /*
        TODO: implement sending logic
        for spectator in spectators:
            spectator.streamObserver.sendColor()
        */

        responseBuilder.setSuccess(true);

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void drawSpell(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        // if request is invalid: either incorrect request type or DrawSpell is not provided
        if (request.getRequestType() != RequestType.DRAW_SPELL || !request.hasDrawSpell()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Invalid request: request type must be 'DRAW_SPELL' and DrawSpell must be provided"
            );

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        var drawSpellRequest = request.getDrawSpell();

        int playerId = drawSpellRequest.getPlayerData().getPlayerId();
        Optional<AttackSession> session = getAttackSessionByPlayerId(playerId);

        // if session not found
        if (session.isEmpty()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Attack session associated with player with id of " + playerId + " not found");

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        // adding new point to the current spell
        double x = drawSpellRequest.getPosition().getX();
        double y = drawSpellRequest.getPosition().getY();
        session.get().addPointToCurrentSpell(new Vector2(x, y));
        logger.info("Adding new spell point: " + new Vector2(x, y));

        // sending response
        responseBuilder.setSuccess(true);

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void finishSpell(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        // if request is invalid: either incorrect request type or FinishSpell is not provided
        if (request.getRequestType() != RequestType.FINISH_SPELL || !request.hasFinishSpell()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Invalid request: request type must be 'FINISH_SPELL' and FinishSpell must be provided"
            );

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        var finishSpellRequest = request.getFinishSpell();

        int playerId = finishSpellRequest.getPlayerData().getPlayerId();
        Optional<AttackSession> session = getAttackSessionByPlayerId(playerId);

        // if session not found
        if (session.isEmpty()) {
            setErrorInActionResultResponse(
                responseBuilder,
                GameError.ErrorType.INVALID_REQUEST,
                "Attack session associated with player with id of " + playerId + " not found");

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
            return;
        }

        double x = finishSpellRequest.getPosition().getX();
        double y = finishSpellRequest.getPosition().getY();
        Vector2 offset = new Vector2(x, y);

        // TODO: run hausdorff and return id of matched template and offset
        logger.info("finishSpell: run hausdorff and return id of matched template and offset");

        responseBuilder.setSuccess(true);

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    private void setErrorInActionResultResponse(
        ActionResultResponse.Builder responseBuilder, GameError.ErrorType errorType, String message) {
        responseBuilder.setSuccess(false);
        responseBuilder.getErrorBuilder()
            .setHasError(true)
            .setType(errorType)
            .setMessage(message);
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
