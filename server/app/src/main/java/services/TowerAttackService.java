package services;

import enchantedtowers.common.utils.proto.requests.LeaveAttackRequest;
import enchantedtowers.common.utils.proto.requests.LeaveSpectatingRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

// components
import components.session.AttackSession;
import components.session.AttackSessionManager;
// proto
// requests
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest.RequestType;
// responses
import enchantedtowers.common.utils.proto.responses.GameError.ErrorType;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse.ResponseType;
// services
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
// game-logic
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
// game-models
import enchantedtowers.game_models.utils.Vector2;



public class TowerAttackService extends TowerAttackServiceGrpc.TowerAttackServiceImplBase {
    private final AttackSessionManager sessionManager = new AttackSessionManager();
    private static final Logger logger = Logger.getLogger(TowerAttackService.class.getName());

    // rpc calls
    /**
     * Entry point before the attack.
     * TODO: fully implement method and add description
     */
    @Override
    public void attackTowerById(TowerIdRequest request, StreamObserver<AttackSessionIdResponse> responseObserver) {
        // TODO: if player attacks several towers simultaneously?
        // TODO: check whether player already has an attack session
        // sessions.add(AttackSession.fromRequest(request));
        int playerId = request.getPlayerData().getPlayerId();
        int towerId = request.getTowerId();

        int sessionId = sessionManager.add(playerId, towerId);

        logger.info("attackTowerById: playerId=" + playerId + ", towerId=" + towerId);
        // System.out.println("attackTowerById: playerId=" + playerId + ", towerId=" + towerId);

        AttackSessionIdResponse.Builder responseBuilder = AttackSessionIdResponse.newBuilder();
        responseBuilder.setSessionId(sessionId);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // TODO: implement leaveAttack & add description
    @Override
    public void leaveAttack(LeaveAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        /*
        // System.out.println("leaveAttack: " + request.getPlayerData().getPlayerId());

        logger.info("leaveAttack: " + request.getPlayerData().getPlayerId());

        final int playerId = request.getPlayerData().getPlayerId();
        Optional<AttackSession> session = sessionManager.getAttackSessionByPlayerId(playerId);

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
            // sessions.remove(session.get());
            sessionManager.remove(session.get());
            responseBuilder.setSuccess(true);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
        */
    }

    // TODO: add description
    @Override
    public void selectSpellColor(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        boolean isRequestValid = (request.getRequestType() == RequestType.SELECT_SPELL_COLOR && request.hasSpellColor());
        boolean sessionExists = isRequestValid && sessionManager.getAttackSessionByPlayerId(
            request.getPlayerData().getPlayerId()).isPresent();

        if (isRequestValid && sessionExists) {
            final int colorId = request.getSpellColor().getColorId();
            final int playerId = request.getPlayerData().getPlayerId();

            // session must exist
            assert(sessionManager.getAttackSessionByPlayerId(playerId).isPresent());
            AttackSession session = sessionManager.getAttackSessionByPlayerId(playerId).get();

            logger.info("Session found: " + session.hashCode());
            // set current color id
            session.setCurrentSpellColorId(colorId);
            logger.info("Setting color id of '" + session.getCurrentSpellColorId() + "'");

            // send current color id to all spectators
            for (var spectator : session.getSpectators()) {
                // create response with type of `SELECT_SPELL_COLOR`
                SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                spectatorResponseBuilder
                        .setResponseType(ResponseType.SELECT_SPELL_COLOR)
                        .getSpellColorBuilder()
                        .setColorId(session.getCurrentSpellColorId())
                        .build();

                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }
            // request processed successfully
            responseBuilder.setSuccess(true);
        }
        else if (!isRequestValid) {
            setErrorInActionResultResponse(
                    responseBuilder,
                    GameError.ErrorType.INVALID_REQUEST,
                    "Invalid request: request type must be 'SELECT_SPELL_COLOR' and spell color must be provided");
        }
        else {
            // session does not exist
            int playerId = request.getPlayerData().getPlayerId();
            setErrorInActionResultResponse(
                    responseBuilder,
                    GameError.ErrorType.INVALID_REQUEST,
                    "Attack session associated with player with id of " + playerId + " not found");
        }

        // sending response
        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    // TODO: add description
    @Override
    public void drawSpell(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        boolean isRequestValid = (request.getRequestType() == RequestType.DRAW_SPELL) && (request.hasDrawSpell());
        boolean sessionExists = isRequestValid && sessionManager.getAttackSessionByPlayerId(
                request.getPlayerData().getPlayerId()).isPresent();

        if (isRequestValid && sessionExists) {
            int playerId = request.getPlayerData().getPlayerId();

            assert(sessionManager.getAttackSessionByPlayerId(playerId).isPresent());
            AttackSession session = sessionManager.getAttackSessionByPlayerId(playerId).get();

            logger.info("Session found: " + session.hashCode());

            // adding new point to the current spell
            double x = request.getDrawSpell().getPosition().getX();
            double y = request.getDrawSpell().getPosition().getY();
            logger.info("Adding new spell point: " + new Vector2(x, y));
            session.addPointToCurrentSpell(new Vector2(x, y));

            // send new point to all spectators
            for (var spectator : session.getSpectators()) {
                // create response with type of `DRAW_SPELL`
                SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();

                spectatorResponseBuilder
                        .setResponseType(ResponseType.DRAW_SPELL)
                        .getSpellPointBuilder()
                        .setX(x)
                        .setY(y)
                        .build();

                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }

            // sending response
            responseBuilder.setSuccess(true);
        }
        else if (!isRequestValid) {
            setErrorInActionResultResponse(
                    responseBuilder,
                    GameError.ErrorType.INVALID_REQUEST,
                    "Invalid request: request type must be 'DRAW_SPELL' and DrawSpell must be provided"
            );
        }
        else {
            // session does not exist
            int playerId = request.getPlayerData().getPlayerId();
            setErrorInActionResultResponse(
                    responseBuilder,
                    GameError.ErrorType.INVALID_REQUEST,
                    "Attack session associated with player with id of " + playerId + " not found");
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    @Override
    public void finishSpell(SpellRequest request, StreamObserver<SpellFinishResponse> streamObserver) {
        SpellFinishResponse.Builder responseBuilder = SpellFinishResponse.newBuilder();

        boolean isRequestValid = (request.getRequestType() == RequestType.FINISH_SPELL && request.hasFinishSpell());
        boolean sessionExists = isRequestValid && sessionManager.getAttackSessionByPlayerId(
            request.getPlayerData().getPlayerId()).isPresent();

        if (isRequestValid && sessionExists) {
            int playerId = request.getPlayerData().getPlayerId();
            Optional<AttackSession> session = sessionManager.getAttackSessionByPlayerId(playerId);

            assert(session.isPresent());
            logger.info("Session found: " + session.get().hashCode());

            Vector2 offset;
            {
                double x = request.getFinishSpell().getPosition().getX();
                double y = request.getFinishSpell().getPosition().getY();
                offset = new Vector2(x, y);
            }

            logger.info("finishSpell: run hausdorff and return id of matched template and offset");

            Optional <SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> matchedTemplateDescription = session.get().getMatchedTemplate(offset);

            if (matchedTemplateDescription.isEmpty()) {
                // send error to attacker
                responseBuilder.getErrorBuilder()
                    .setHasError(true)
                    .setType(GameError.ErrorType.SPELL_TEMPLATE_NOT_FOUND)
                    .setMessage("No template found to match provided spell");

                // TODO: refactor (two cycles that send either error or data)
                // send error to all spectators
                for (var spectator : session.get().getSpectators()) {
                    // create response with type of `FINISH_SPELL`
                    SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                    spectatorResponseBuilder
                        .setResponseType(ResponseType.FINISH_SPELL)
                        .getErrorBuilder()
                            .setHasError(true)
                            .setType(ErrorType.SPELL_TEMPLATE_NOT_FOUND)
                            .setMessage("Attacking player drawing did not match any spells")
                            .build();

                    spectator.streamObserver().onNext(spectatorResponseBuilder.build());
                }
            }
            else {
                // send data to attacker
                int templateId = matchedTemplateDescription.get().id();
                double x = matchedTemplateDescription.get().offset().x;
                double y = matchedTemplateDescription.get().offset().y;

                // Build template offset
                responseBuilder.getSpellDescriptionBuilder().getSpellTemplateOffsetBuilder()
                    .setX(x)
                    .setY(y)
                    .build();

                // Build template description
                responseBuilder.getSpellDescriptionBuilder()
                    .setColorId(session.get().getCurrentSpellColorId())
                    .setSpellTemplateId(templateId)
                    .build();

                // save the template to the canvas history
                session.get().saveMatchedTemplate();

                // TODO: refactor this place (two cycles that do the same thing)
                // send data to all spectators
                for (var spectator : session.get().getSpectators()) {
                    // create response with type of `FINISH_SPELL`
                    SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                    spectatorResponseBuilder.setResponseType(ResponseType.FINISH_SPELL);

                    // build spell description
                    int colorId = session.get().getCurrentSpellColorId();
                    var spellDescriptionBuilder = spectatorResponseBuilder.getSpellDescriptionBuilder();
                    spellDescriptionBuilder
                        .setColorId(colorId)
                        .setSpellTemplateId(templateId)
                        .getSpellTemplateOffsetBuilder()
                            .setX(x)
                            .setY(y)
                            .build();
                    spellDescriptionBuilder.build();

                    spectator.streamObserver().onNext(spectatorResponseBuilder.build());
                }
            }
        }
        else if (!isRequestValid) {
            responseBuilder.getErrorBuilder()
                .setHasError(true)
                .setType(GameError.ErrorType.INVALID_REQUEST)
                .setMessage("Invalid request: request type must be 'FINISH_SPELL' and FinishSpell must be provided");
        }
        else {
            // session does not exist
            int playerId = request.getPlayerData().getPlayerId();

            responseBuilder.getErrorBuilder()
                .setHasError(true)
                .setType(GameError.ErrorType.INVALID_REQUEST)
                .setMessage(
                    "Attack session associated with player with id of " + playerId + " not found");
        }

        if (sessionExists) {
            // clear the session state for the new spell drawing
            int playerId = request.getPlayerData().getPlayerId();
            var session = sessionManager.getAttackSessionByPlayerId(playerId);
            assert(session.isPresent());
            session.get().clearCurrentDrawing();
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    // spectating related methods
    @Override
    public void trySpectateTowerById(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        int towerId = request.getTowerId();
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<AttackSession> session = sessionManager.getAttackSessionByTowerId(towerId);

        if (session.isPresent()) {
            responseBuilder.setSuccess(true);
        }
        else {
            setErrorInActionResultResponse(
                responseBuilder,
                ErrorType.ATTACK_SESSION_NOT_FOUND,
                "Attack session associated with tower id of " + towerId + " not found"
            );
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void spectateTowerById(TowerIdRequest request, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        int towerId = request.getTowerId();
        int spectatingPlayerId = request.getPlayerData().getPlayerId();

        SpectateTowerAttackResponse.Builder responseBuilder = SpectateTowerAttackResponse.newBuilder();

        Optional<AttackSession> sessionOpt = sessionManager.getAttackSessionByTowerId(towerId);
        if (sessionOpt.isPresent()) {
            AttackSession session = sessionOpt.get();
            // create response with canvas state
            responseBuilder.setResponseType(ResponseType.CURRENT_CANVAS_STATE);
            addCurrentSpellIfExists(responseBuilder, session);
            addSpellDescriptionsOfDrawnSpells(responseBuilder, session);
            // build canvas
            responseBuilder.getCanvasStateBuilder().build();
            // send canvas state to spectator
            streamObserver.onNext(responseBuilder.build());
            // adding spectator
            session.addSpectator(spectatingPlayerId, streamObserver);
        }
        else {
            // session does not exist
            responseBuilder.getErrorBuilder()
                .setHasError(true)
                .setType(GameError.ErrorType.ATTACK_SESSION_NOT_FOUND)
                .setMessage("Attack session associated with tower id of " + towerId + " not found")
                .build();

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }

    // TODO: implement
    @Override
    public void leaveSpectating(LeaveSpectatingRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        /*
        final int playerId = request.getData().getPlayerId();
        boolean successfullyRemoved = false;

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        // TODO: reimplement using session manager
        for (var session : sessions) {
            AttackSession.Spectator spectator = session.pollSpectatorById(playerId);
            if (spectator != null) {
                // if spectator found, close connection
                spectator.streamObserver().onCompleted();
                successfullyRemoved = true;
                break;
            }
        }

        if (successfullyRemoved) {
            responseBuilder.setSuccess(true);
        }
        else {
            // could not remove spectator since player did not spectate any attack session
            setErrorInActionResultResponse(
                responseBuilder,
                ErrorType.INVALID_REQUEST,
                "Player with id of '" + playerId + "' is not spectating any tower attack"
            );
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
        */
    }


    // helper methods
    /**
    * Adds currently being drawn spell to {@link SpectateTowerAttackResponse} if the one exists.
    */
    private void addCurrentSpellIfExists(SpectateTowerAttackResponse.Builder responseBuilder, AttackSession session) {
        // set current spell if it exists (aka spell that is being used for drawing right now)
        if (session.hasCurrentSpell()) {
            List<enchantedtowers.common.utils.proto.common.Vector2> points = new ArrayList<>();
            // collect drawn spell points
            for (var point : session.getCurrentSpellPoints()) {
                var vector2 = enchantedtowers.common.utils.proto.common.Vector2.newBuilder()
                    .setX(point.x)
                    .setY(point.y)
                    .build();
                points.add(vector2);
            }

            // build current spell (aka spell that is being drawn right now)
            var canvasBuilder = responseBuilder.getCanvasStateBuilder();
            canvasBuilder.getCurrentSpellStateBuilder()
                .setColorId(session.getCurrentSpellColorId())
                .addAllPoints(points)
                .build();
        }
    }

    /**
     * Adds spell descriptions (i.e. {@link SpellDescriptionResponse} instances) of already drawn spells into {@link SpectateTowerAttackResponse} response.
     */
    private void addSpellDescriptionsOfDrawnSpells(SpectateTowerAttackResponse.Builder responseBuilder, AttackSession session) {
        List<SpellDescriptionResponse> spellDescriptionResponses = new ArrayList<>();

        // collecting building descriptions of all already drawn spells on canvas
        for (var spellDescription : session.getDrawnSpellsDescriptions()) {
            SpellDescriptionResponse.Builder spellDescriptionResponseBuilder = SpellDescriptionResponse.newBuilder();

            // add spell template offset
            Vector2 offset = spellDescription.offset();
            spellDescriptionResponseBuilder.getSpellTemplateOffsetBuilder()
                .setX(offset.x)
                .setY(offset.y)
                .build();

            // add the other fields
            int spellTemplateId = spellDescription.id();
            int colorId = spellDescription.colorId();
            spellDescriptionResponseBuilder
                .setColorId(colorId)
                .setSpellTemplateId(spellTemplateId);

            spellDescriptionResponses.add(spellDescriptionResponseBuilder.build());
        }

        // adding spell descriptions into canvas state response
        var canvasBuilder = responseBuilder.getCanvasStateBuilder();
        canvasBuilder.addAllSpellDescriptions(spellDescriptionResponses);
    }

    // TODO: generify to accept not only `ActionResultResponse` type of response model
    private void setErrorInActionResultResponse(
        ActionResultResponse.Builder responseBuilder, GameError.ErrorType errorType, String message) {
        responseBuilder.setSuccess(false);
        responseBuilder.getErrorBuilder()
            .setHasError(true)
            .setType(errorType)
            .setMessage(message);
    }
}
