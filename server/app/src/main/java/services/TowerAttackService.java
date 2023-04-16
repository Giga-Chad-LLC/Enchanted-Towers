package services;

import enchantedtowers.common.utils.proto.requests.*;
import enchantedtowers.common.utils.proto.responses.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

// components
import components.session.AttackSession;
import components.session.AttackSessionManager;
// requests
import enchantedtowers.common.utils.proto.requests.SpellRequest.RequestType;
// responses
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
     * If player is neither in an attack session nor in a spectating mode, then creates an attack session associated with current player.
     * Otherwise, sends error.
     */
    @Override
    public void attackTowerById(TowerIdRequest request, StreamObserver<AttackSessionIdResponse> responseObserver) {
        int playerId = request.getPlayerData().getPlayerId();
        int towerId = request.getTowerId();

        AttackSessionIdResponse.Builder responseBuilder = AttackSessionIdResponse.newBuilder();

        boolean isAttacking = sessionManager.hasSessionAssociatedWithPlayerId(playerId);
        boolean isSpectating = sessionManager.isPlayerInSpectatingMode(playerId);

        logger.info("attackTowerById: got playerId=" + playerId + ", towerId=" + towerId);

        if (!isAttacking && !isSpectating) {
            // creating new attack session associated with player
            logger.info("Creating attack session for player with id " + playerId);
            int sessionId = sessionManager.add(playerId, towerId);
            responseBuilder.setSessionId(sessionId);
        }
        else if (isAttacking) {
            // if player is already in attack session
            logger.info("Player with id " + playerId + " is already in attack session");
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is already in attack session");
        }
        else {
            // if player is already spectating
            logger.info("Player with id " + playerId + " is already spectating someone's attack session");
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is already spectating someone's attack session");
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * If request is valid, closes connections to all spectators and removes attack session associated with player.
     * Otherwise, sends error to the player.
     */
    @Override
    public void leaveAttack(LeaveAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        logger.info("leaveAttack: playerId=" + request.getPlayerData().getPlayerId() + ", sessionId=" + request.getSessionId());

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId = request.getPlayerData().getPlayerId();

        boolean sessionExists = sessionManager.getSessionById(sessionId).isPresent();
        boolean isPlayerAssociatedWithFoundSession = sessionExists &&
                playerId == sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();

        if (sessionExists && isPlayerAssociatedWithFoundSession) {
            AttackSession session = sessionManager.getSessionById(sessionId).get();

            // send onComplete() to all spectators
            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onCompleted();
            }
            // remove attack session
            sessionManager.remove(session);

            responseBuilder.setSuccess(true);
        }
        else if (!sessionExists) {
            // session not found
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with provided id " + sessionId + " not found");
        }
        else {
            // player id does not equal to the id of player associated with found session
            int associatedPlayerId = sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is no associated with found session, expected player id " + associatedPlayerId);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * Sets new spell color in {@link AttackSession} instance and sends the updated color of current spell to all spectators.
     */
    @Override
    public void selectSpellColor(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        boolean isRequestValid = (request.getRequestType() == RequestType.SELECT_SPELL_COLOR && request.hasSpellColor());
        boolean sessionExists = isRequestValid && sessionManager.getSessionById(sessionId).isPresent();
        boolean AttackerIdMatchesPlayerId = sessionExists && playerId == sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();

        if (isRequestValid && sessionExists && AttackerIdMatchesPlayerId) {
            final int colorId = request.getSpellColor().getColorId();

            // session must exist
            assert(sessionManager.getSessionById(sessionId).isPresent());
            AttackSession session = sessionManager.getSessionById(sessionId).get();

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
            buildServerError(responseBuilder.getErrorBuilder(),
                ServerError.ErrorType.INVALID_REQUEST,
                "Invalid request: request type must be 'SELECT_SPELL_COLOR' and spell color must be provided");
        }
        else if (!sessionExists) {
            // session does not exist
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with id " + sessionId + " not found");
        }
        else {
            // attacker id does not match player id
            int attackerId = sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Attacker id " + attackerId + " does not match player id " + playerId);
        }

        // sending response
        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Adds new canvas point of currently being drawn spell and sends this point to all spectators.
     */
    @Override
    public void drawSpell(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        boolean isRequestValid = (request.getRequestType() == RequestType.DRAW_SPELL) && (request.hasDrawSpell());
        boolean sessionExists = isRequestValid && sessionManager.getSessionById(sessionId).isPresent();
        boolean AttackerIdMatchesPlayerId = sessionExists && playerId == sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();

        if (isRequestValid && sessionExists && AttackerIdMatchesPlayerId) {
            AttackSession session = sessionManager.getSessionById(sessionId).get();

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
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Invalid request: request type must be 'DRAW_SPELL' and DrawSpell must be provided");
        }
        else if (!sessionExists) {
            // session does not exist
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with id " + sessionId + " not found");
        }
        else {
            // attacker id does not match player id
            int attackerId = sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Attacker id " + attackerId + " does not match player id " + playerId);
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Finishes current spell drawing and tries to match a spell template to the drawn spell.
     * If template found, sends the template id as a substitution of a spell to the attack and to all spectators.
     * Otherwise, sends error (template not found) to the attacker and all spectators about.
     */
    @Override
    public void finishSpell(SpellRequest request, StreamObserver<SpellFinishResponse> streamObserver) {
        SpellFinishResponse.Builder responseBuilder = SpellFinishResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        boolean isRequestValid = (request.getRequestType() == RequestType.FINISH_SPELL && request.hasFinishSpell());
        boolean sessionExists = isRequestValid && sessionManager.getSessionById(sessionId).isPresent();
        boolean AttackerIdMatchesPlayerId = sessionExists && playerId == sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();

        if (isRequestValid && sessionExists && AttackerIdMatchesPlayerId) {
            Optional<AttackSession> session = sessionManager.getSessionById(sessionId);
            assert(session.isPresent());
            logger.info("Session found: " + session.get().hashCode());

            Vector2 offset;
            {
                double x = request.getFinishSpell().getPosition().getX();
                double y = request.getFinishSpell().getPosition().getY();
                offset = new Vector2(x, y);
            }

            logger.info("finishSpell: run hausdorff and return id of matched template and offset");

            Optional<SpellsPatternMatchingAlgorithm.MatchedTemplateDescription> matchedTemplateDescription = session.get().getMatchedTemplate(offset);

            // no matching template found
            if (matchedTemplateDescription.isEmpty()) {
                // send error to attacker
                buildServerError(responseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                        "No template found to match provided spell");

                // send error to all spectators
                for (var spectator : session.get().getSpectators()) {
                    // create response with type of `FINISH_SPELL`
                    SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                    spectatorResponseBuilder.setResponseType(ResponseType.FINISH_SPELL);
                    buildServerError(spectatorResponseBuilder.getErrorBuilder(),
                            ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                            "Attacking player drawing did not match any spells");

                    spectator.streamObserver().onNext(spectatorResponseBuilder.build());
                }
            }
            else {
                // send data to attacker
                final int templateId = matchedTemplateDescription.get().id();
                final double x = matchedTemplateDescription.get().offset().x;
                final double y = matchedTemplateDescription.get().offset().y;

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

                // send data to all spectators
                final int colorId = session.get().getCurrentSpellColorId();
                for (var spectator : session.get().getSpectators()) {
                    // create response with type of `FINISH_SPELL`
                    SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                    spectatorResponseBuilder.setResponseType(ResponseType.FINISH_SPELL);

                    // build spell description
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
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Invalid request: request type must be 'FINISH_SPELL' and FinishSpell must be provided");
        }
        else if (!sessionExists) {
            // session does not exist
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with id " + sessionId + " not found");
        }
        else {
            // attacker id does not match player id
            int attackerId = sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Attacker id " + attackerId + " does not match player id " + playerId);
        }

        if (sessionExists) {
            // clear the session state for the new spell drawing
            var session = sessionManager.getSessionById(sessionId);
            assert(session.isPresent());
            session.get().clearCurrentDrawing();
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    // spectating related methods

    /**
     * Retrieves any session associated with provided tower id and sends its id (i.e. session id) the client.
     * If session not found, sends error.
     */
    @Override
    public void trySpectateTowerById(TowerIdRequest request, StreamObserver<AttackSessionIdResponse> streamObserver) {
        int towerId = request.getTowerId();
        AttackSessionIdResponse.Builder responseBuilder = AttackSessionIdResponse.newBuilder();
        Optional<AttackSession> session = sessionManager.getAnyAttackSessionByTowerId(towerId);

        if (session.isPresent()) {
            responseBuilder.setSessionId(session.get().getId());
        }
        else {
            logger.info("trySpectateTowerById: session associated with tower id " + towerId + " not found");
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session associated with tower id of " + towerId + " not found");
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    /**
     * Registers player as spectator to the events of the requested attack session.
     */
    @Override
    public void spectateTowerBySessionId(AttackSessionIdRequest request, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        final int sessionId = request.getSessionId();
        final int spectatingPlayerId = request.getPlayerData().getPlayerId();

        SpectateTowerAttackResponse.Builder responseBuilder = SpectateTowerAttackResponse.newBuilder();
        Optional<AttackSession> sessionOpt = sessionManager.getSessionById(sessionId);

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
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with id " + sessionId + " not found");

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }

    /**
     * Finds spectator with provided player id in the requested attack session.
     * Then closes the connection with the spectator and removes it from session spectators list.
     */
    @Override
    public void leaveSpectating(LeaveSpectatingRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        logger.info("leaveSpectating: playerId=" + request.getPlayerData().getPlayerId() + ", sessionId=" + request.getSessionId());

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int spectatingPlayerId = request.getPlayerData().getPlayerId();

        boolean sessionExists = sessionManager.getSessionById(sessionId).isPresent();
        if (sessionExists) {
            AttackSession session = sessionManager.getSessionById(sessionId).get();
            Optional<AttackSession.Spectator> spectator = session.pollSpectatorById(spectatingPlayerId);

            if (spectator.isPresent()) {
                // closing connection
                logger.info("Closing connection with spectator with id " + spectatingPlayerId);
                spectator.get().streamObserver().onCompleted();
                responseBuilder.setSuccess(true);
            }
            else {
                // player is not spectating
                buildServerError(responseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.INVALID_REQUEST,
                        "Session with id " + sessionId + " does not have spectator with id " + spectatingPlayerId);
            }
        }
        else {
            // session not found
            buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.ATTACK_SESSION_NOT_FOUND,
                    "Attack session with provided id " + sessionId + " not found");
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
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

    private void buildServerError(ServerError.Builder builder, ServerError.ErrorType type, String message) {
        builder.setType(type).setMessage(message).build();
    }
}
