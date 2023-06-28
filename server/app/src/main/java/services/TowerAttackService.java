package services;

import components.defend_spells.DefendSpellTimeout;
import components.defend_spells.DefendSpellsManager;
import enchantedtowers.common.utils.proto.common.DefendSpellDescription;
import enchantedtowers.common.utils.proto.requests.CastDefendSpellRequest;
import enchantedtowers.common.utils.proto.responses.ServerError.Builder;
import enchantedtowers.common.utils.proto.responses.ServerError.ErrorType;
import enchantedtowers.game_models.SpellBook;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

import components.mediator.TowersUpdatesMediator;
import components.session.AttackSession;
import components.session.AttackSession.Spectator;
import components.session.AttackSessionManager;
import components.time.Timeout;
import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.common.SpellDescription;
import enchantedtowers.common.utils.proto.common.SpellStat;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.common.utils.proto.requests.LeaveAttackRequest;
import enchantedtowers.common.utils.proto.requests.LeaveSpectatingRequest;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest.RequestType;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.*;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse.ResponseType;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.game_logic.algorithm.EnchantmentMatchingAlgorithm;
import enchantedtowers.game_logic.algorithm.SpellsMatchingAlgorithm;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.SpellTemplateDescription;
import enchantedtowers.game_models.Tower;
import components.registry.TowersRegistry;
import enchantedtowers.game_models.utils.Vector2;
import interactors.TowerAttackServiceInteractor;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import javax.swing.Action;
import javax.swing.text.html.Option;


public class TowerAttackService extends TowerAttackServiceGrpc.TowerAttackServiceImplBase {
    private final static double MINIMUM_REQUIRED_MATCH_COEFFICIENT = 0.80;
    private final AttackSessionManager sessionManager = new AttackSessionManager();
    private final DefendSpellsManager defendSpellsManager = new DefendSpellsManager();
    private final Logger logger = Logger.getLogger(TowerAttackService.class.getName());
    private final IntConsumer onSessionExpiredCallback = this::onSessionExpired;

    // TODO: check distance between player and tower

    // rpc calls
    /**
     * <p>If player is neither in an attack session nor in a spectating mode, then notifies client that creation of an attack session is allowed. Otherwise, sends error.</p>
     * <p>This method serves as a convenient check that attack session can be entered on the client side. Although, the {@link TowerAttackService#attackTowerById} method is still required to make the appropriate validations.</p>
     */
    @Override
    public synchronized void tryAttackTowerById(TowerIdRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        logger.info("tryAttackTowerById: got playerId=" + request.getPlayerData().getPlayerId() +
                    ", towerId=" + request.getTowerId());

        Optional<ServerError> serverError = validateAttackingTowerById(request);

        if (serverError.isEmpty()) {
            // establishment of attack session is allowed
            responseBuilder.setSuccess(true);
        }
        else {
            logger.info("Attack session cannot be created, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    /**
     * This method creates attack session associated with provided player id and stores the client's <code>responseObserver</code> in {@link AttackSession} for later notifications of session events (e.g. session expiration). The response contains id of created attack session.
     */
    @Override
    public synchronized void attackTowerById(TowerIdRequest request, StreamObserver<SessionStateInfoResponse> streamObserver) {
        SessionStateInfoResponse.Builder responseBuilder = SessionStateInfoResponse.newBuilder();

        logger.info("attackTowerById: got playerId=" + request.getPlayerData().getPlayerId() +
                ", towerId=" + request.getTowerId());

        Optional<ServerError> serverError = validateAttackingTowerById(request);

        if (serverError.isEmpty()) {
            int playerId = request.getPlayerData().getPlayerId();
            int towerId = request.getTowerId();

            TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(towerId);
            // mark tower as being under attack
            interactor.setTowerUnderAttackState();

            logger.info("Creating attack session for player with id " + playerId);

            // creating new attack session associated with player and registering onSessionExpiredCallback
            AttackSession session = sessionManager.createAttackSession(
                    playerId, towerId, interactor.getEnchantedProtectionWallId(), streamObserver, onSessionExpiredCallback);

            // create cancel handler to hook the event of client closing the connection
            // in this case spectators must be disconnected and attack session must be removed
            var callObserver = (ServerCallStreamObserver<SessionStateInfoResponse>) streamObserver;
            // `setOnCancelHandler` must be called before any `onNext` calls
            callObserver.setOnCancelHandler(() -> onAttackerStreamCancellation(session));

            // setting session id into response
            responseBuilder.setType(SessionStateInfoResponse.ResponseType.SESSION_CREATED);
            responseBuilder.getSessionBuilder()
                    .setSessionId(session.getId())
                    .setLeftTimeMs(session.getExpirationTimeoutMs())
                    .build();

            // setting active defend spells
            responseBuilder.addAllActiveDefendSpells(createActiveDefendSpellsList(towerId));

            streamObserver.onNext(responseBuilder.build());
            // notifying listeners of tower update
            TowersUpdatesMediator.getInstance().notifyObservers(List.of(towerId));
        }
        else {
            logger.info("Attack session cannot be created, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());

            // send error
            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }

    private List<DefendSpellDescription> createActiveDefendSpellsList(int towerId) {
        List<DefendSpellTimeout> activeDefendSpells = defendSpellsManager.getActiveDefendSpellsByTowerId(towerId);
        List<DefendSpellDescription> activeDefendSpellsToSend = new ArrayList<>();
        for (var defendSpell : activeDefendSpells) {
            DefendSpellDescription desc = DefendSpellDescription.newBuilder()
                .setDefendSpellTemplateId(defendSpell.getId())
                .setLeftTimeMs(defendSpell.getLeftExecutionTimeMs())
                .build();
            activeDefendSpellsToSend.add(desc);
        }

        return activeDefendSpellsToSend;
    }

    /**
     * If request is valid, closes connections to all spectators and removes attack session associated with player.
     * Otherwise, sends error to the player.
     */
    @Override
    public synchronized void leaveAttack(LeaveAttackRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        logger.info("leaveAttack: playerId=" + request.getPlayerData().getPlayerId() + ", sessionId=" + request.getSessionId());

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId = request.getPlayerData().getPlayerId();

        boolean sessionExists = sessionManager.getSessionById(sessionId).isPresent();
        boolean isPlayerAssociatedWithFoundSession = sessionExists &&
                playerId == sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();

        if (sessionExists && isPlayerAssociatedWithFoundSession) {
            AttackSession session = sessionManager.getSessionById(sessionId).get();

            // mark tower to not be under attack
            TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(session.getAttackedTowerId());
            interactor.unsetTowerUnderAttackState();

            // send onCompleted() to all spectators
            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onCompleted();
            }
            // remove attack session
            sessionManager.remove(session);

            responseBuilder.setSuccess(true);

            // notifying listeners of tower update
            TowersUpdatesMediator.getInstance().notifyObservers(List.of(session.getAttackedTowerId()));
        }
        else if (!sessionExists) {
            // session not found
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.SESSION_NOT_FOUND,
                    "Attack session with provided id " + sessionId + " not found");
        }
        else {
            // player id does not equal to the id of player associated with found session
            int associatedPlayerId = sessionManager.getSessionById(sessionId).get().getAttackingPlayerId();
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is no associated with found session, expected player id " + associatedPlayerId);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }


    /**
     * Sets new spell type in {@link AttackSession} instance and sends the updated type of current spell to all spectators.
     */
    @Override
    public synchronized void selectSpellType(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, RequestType.SELECT_SPELL_TYPE);

        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();

            logger.info("Session found: " + session.hashCode());
            // set current spell type
            final SpellType spellType = request.getSpellType().getSpellType();
            session.setCurrentSpellType(spellType);
            logger.info("Setting type of '" + session.getCurrentSpellType() + "'");

            // create response with type of `SELECT_SPELL_TYPE`
            SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
            spectatorResponseBuilder
                    .setResponseType(ResponseType.SELECT_SPELL_TYPE)
                    .setSpellType(session.getCurrentSpellType())
                    .build();

            // send current spell type to all spectators
            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }

            // request processed successfully
            responseBuilder.setSuccess(true);
        }
        else {
            // error occurred
            logger.info("Cannot select spell type, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        // sending response
        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Adds new canvas point of currently being drawn spell and sends this point to all spectators.
     */
    @Override
    public synchronized void drawSpell(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, RequestType.DRAW_SPELL);

        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();

            logger.info("Session found: " + session.hashCode());

            SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
            // adding data into response
            {
                // adding new point to the current spell
                double x = request.getDrawSpell().getPosition().getX();
                double y = request.getDrawSpell().getPosition().getY();
                logger.info("Adding new spell point: " + new Vector2(x, y));
                session.addPointToCurrentSpell(new Vector2(x, y));

                // create response with type of `DRAW_SPELL`
                spectatorResponseBuilder
                        .setResponseType(ResponseType.DRAW_SPELL)
                        .getSpellPointBuilder()
                        .setX(x)
                        .setY(y)
                        .build();
            }

            // send new point to all spectators
            logger.info("drawSpell: number of spectators: " + session.getSpectators().size());
            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }

            // sending response
            responseBuilder.setSuccess(true);
        }
        else {
            // error occurred
            logger.info("Cannot draw spell, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
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
    public synchronized void finishSpell(SpellRequest request, StreamObserver<SpellFinishResponse> streamObserver) {
        SpellFinishResponse.Builder responseBuilder = SpellFinishResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, RequestType.FINISH_SPELL);

        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();
            logger.info("Session found: " + session.hashCode());

            Vector2 offset = new Vector2(
                request.getFinishSpell().getPosition().getX(),
                request.getFinishSpell().getPosition().getY()
            );

            logger.info("finishSpell: run hausdorff and return id of matched template and offset");

            Optional<SpellTemplateDescription> matchedTemplateDescriptionOpt = SpellsMatchingAlgorithm.getMatchedTemplateWithHausdorffMetric(
                    session.getCurrentSpellPoints(),
                    offset,
                    session.getCurrentSpellType()
            );

            // no matching template found
            if (matchedTemplateDescriptionOpt.isEmpty()) {
                // send error to attacker
                ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                        "No template found to match provided spell");

                // create response with type of `FINISH_SPELL`
                SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();

                spectatorResponseBuilder.setResponseType(ResponseType.FINISH_SPELL);
                ProtoModelsUtils.buildServerError(spectatorResponseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                        "Attacking player drawing did not match any spells");

                // send error to all spectators
                for (var spectator : session.getSpectators()) {
                    spectator.streamObserver().onNext(spectatorResponseBuilder.build());
                }
            }
            else {
                SpellTemplateDescription spellTemplateDescription = matchedTemplateDescriptionOpt.get();

                // send data to attacker
                final int templateId = spellTemplateDescription.id();
                final double x = spellTemplateDescription.offset().x;
                final double y = spellTemplateDescription.offset().y;

                // Build template offset
                responseBuilder.getSpellDescriptionBuilder().getSpellTemplateOffsetBuilder()
                        .setX(x)
                        .setY(y)
                        .build();

                // Build template description
                responseBuilder.getSpellDescriptionBuilder()
                        .setSpellType(session.getCurrentSpellType())
                        .setSpellTemplateId(templateId)
                        .build();

                // save the template to the canvas state
                session.addTemplateToCanvasState(spellTemplateDescription);

                // send data to all spectators
                final SpellType spellType = session.getCurrentSpellType();

                // create spectators' response with type of `FINISH_SPELL`
                SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
                {
                    spectatorResponseBuilder.setResponseType(ResponseType.FINISH_SPELL);

                    // build spell description
                    var spellDescriptionBuilder = spectatorResponseBuilder.getSpellDescriptionBuilder();
                    spellDescriptionBuilder
                            .setSpellType(spellType)
                            .setSpellTemplateId(templateId)
                            .getSpellTemplateOffsetBuilder()
                            .setX(x)
                            .setY(y)
                            .build();
                    spellDescriptionBuilder.build();
                }

                for (var spectator : session.getSpectators()) {
                    spectator.streamObserver().onNext(spectatorResponseBuilder.build());
                }
            }

            // clear the session state for the new spell drawing
            session.clearCurrentDrawing();
        }
        else {
            // error occurred
            logger.info("Cannot finish spell, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Removes drawn spells descriptions from {@link AttackSession} instance associated with player, and notifies spectators of the clearing canvas event.
     */
    @Override
    public synchronized void clearCanvas(SpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, RequestType.CLEAR_CANVAS);

        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();

            // clearing drawn spells
            session.clearDrawnSpellsDescriptions();

            // notifying spectators to clear the canvas
            SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
            spectatorResponseBuilder.setResponseType(ResponseType.CLEAR_CANVAS);
            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }

            responseBuilder.setSuccess(true);
        }
        else {
            // error occurred
            logger.info("Cannot clear canvas, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Compares player's drawn enchantment (which is the list of {@link SpellTemplateDescription}) with the actual enchantment stored inside {@link ProtectionWall} of {@link Tower} that is being under attack. Comparison is being made by calling {@link EnchantmentMatchingAlgorithm#getEnchantmentMatchStatsWithHausdorffMetric} method.
     */
    @Override
    public synchronized void compareDrawnSpells(SpellRequest request, StreamObserver<MatchedSpellStatsResponse> streamObserver) {
        MatchedSpellStatsResponse.Builder responseBuilder = MatchedSpellStatsResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, RequestType.COMPARE_DRAWN_SPELLS);

        // check that protection wall is still enchanted (i.e. it was not destroyed during previous attempt)
        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();
            TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(session.getAttackedTowerId());

            if (!interactor.isProtectionWallEnchanted(session.getProtectionWallId())) {
                ServerError.Builder errorBuilder = ServerError.newBuilder();
                ProtoModelsUtils.buildServerError(errorBuilder,
                        ServerError.ErrorType.INVALID_REQUEST,
                        "Protection wall with id " + session.getProtectionWallId() + " already destroyed");

                serverError = Optional.of(errorBuilder.build());
            }
        }

        if (serverError.isEmpty()) {
            AttackSession session = sessionManager.getSessionById(request.getSessionId()).get();

            // retrieve actual and guess enchantments
            TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(session.getAttackedTowerId());
            Enchantment guess  = interactor.enchantmentOf(session.getDrawnSpellsDescriptions());
            Enchantment actual = interactor.getWallEnchantment(session.getProtectionWallId());

            logger.info("Comparing player's guess enchantment with actual enchantment of protection wall...");

            // [spell type] -> [match value in range of [0, 1]]
            Map<SpellType, Double> matches = EnchantmentMatchingAlgorithm.getEnchantmentMatchStatsWithHausdorffMetric(guess, actual);
            logger.info("Got matches: " + matches);

            // clearing drawn spells
            session.clearDrawnSpellsDescriptions();

            // checking whether player has succeeded to break the protection wall
            final boolean protectionWallDestroyed = isProtectionWallDestroyed(matches);

            logger.info("compareDrawnSpells: protection wall " + session.getProtectionWallId() +
                             " destroyed state: " + protectionWallDestroyed);

            if (protectionWallDestroyed) {
                // destroy protection wall
                interactor.destroyProtectionWallWithId(session.getProtectionWallId());
                // notifying listeners of tower update
                TowersUpdatesMediator.getInstance().notifyObservers(List.of(session.getAttackedTowerId()));
                responseBuilder.setProtectionWallDestroyed(true);
            }
            else {
                responseBuilder.setProtectionWallDestroyed(false);
            }

            // add spell matching stats into response
            List<SpellStat> spellStats = new ArrayList<>();

            for (var entry : matches.entrySet()) {
                SpellType type = entry.getKey();
                double match = entry.getValue();

                SpellStat stat = SpellStat.newBuilder()
                        .setSpellType(type)
                        .setMatch(match)
                        .build();

                spellStats.add(stat);
            }

            responseBuilder.addAllStats(spellStats);

            // notifying spectators with matched spells
            SpectateTowerAttackResponse.Builder spectatorResponseBuilder = SpectateTowerAttackResponse.newBuilder();
            spectatorResponseBuilder.setResponseType(ResponseType.COMPARE_ENCHANTMENTS);
            spectatorResponseBuilder.addAllSpellMatchStats(spellStats);
            spectatorResponseBuilder.setProtectionWallDestroyed(protectionWallDestroyed);

            for (var spectator : session.getSpectators()) {
                spectator.streamObserver().onNext(spectatorResponseBuilder.build());
            }
        }
        else {
            // error occurred
            logger.info("Cannot compare drawn spells, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    // spectating related methods
    /**
     * Retrieves any session associated with provided tower id and sends its id (i.e. session id) to the client.
     * If session not found, sends error.
     */
    @Override
    public synchronized void trySpectateTowerById(TowerIdRequest request, StreamObserver<SessionIdResponse> streamObserver) {
        SessionIdResponse.Builder responseBuilder = SessionIdResponse.newBuilder();
        Optional<AttackSession> session = sessionManager.getAnyAttackSessionByTowerId(request.getTowerId());

        Optional<ServerError> serverError = validateSpectatingAction(
                session, request.getPlayerData().getPlayerId(), SpectatingRequirement.SPECTATING_PROHIBITED);

        if (serverError.isEmpty()) {
            responseBuilder.setSessionId(session.get().getId());
        }
        else {
            // error occurred
            logger.info("trySpectateTowerById: Cannot enter tower spectating, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    /**
     * Registers player as spectator to the events of the requested attack session.
     * <p>
     * Creates <code>callObserver</code> instance of {@link ServerCallStreamObserver<SpectateTowerAttackResponse>} class
     * and registers {@link ServerCallStreamObserver#setOnCancelHandler(Runnable)} callback that invalidates spectator
     * (i.e. marks the underlining spectator's {@link StreamObserver} to be no longer valid and to be removed from spectators list).
     * <p>
     * Note that the handler only marks the spectator as invalid, the removal itself is being held inside {@link AttackSession}
     * and is opaque to the caller. In particular, the removal of an invalid spectator is done inside {@link AttackSession#getSpectators} method, thus making the removal lazy. The caller must not rely on this implementation.
     */
    @Override
    public synchronized void spectateTowerBySessionId(SessionIdRequest request, StreamObserver<SpectateTowerAttackResponse> streamObserver) {
        final int spectatingPlayerId = request.getPlayerData().getPlayerId();

        SpectateTowerAttackResponse.Builder responseBuilder = SpectateTowerAttackResponse.newBuilder();
        Optional<AttackSession> sessionOpt = sessionManager.getSessionById(request.getSessionId());

        Optional<ServerError> serverError = validateSpectatingAction(
                sessionOpt, spectatingPlayerId, SpectatingRequirement.SPECTATING_PROHIBITED);

        if (serverError.isEmpty()) {
            AttackSession session = sessionOpt.get();
            // create response with canvas state
            responseBuilder.setResponseType(ResponseType.CURRENT_CANVAS_STATE);
            addCurrentSpellIfExists(responseBuilder, session);
            addSpellDescriptionsOfDrawnSpells(responseBuilder, session);
            // build canvas
            responseBuilder.getCanvasStateBuilder().build();
            // setting remaining time
            responseBuilder.setLeftTimeMs(session.getLeftExecutionTimeMs());
            // setting active defend spells
            responseBuilder.addAllActiveDefendSpells(createActiveDefendSpellsList(session.getAttackedTowerId()));

            logger.info("Attack session with id " + session.getId() + " left execution time: " + session.getLeftExecutionTimeMs() + "ms");

            // adding spectator
            session.addSpectator(spectatingPlayerId, streamObserver);

            // create cancel handler to hook the event of client closing the connection (deleting spectator in this case)
            var callObserver = (ServerCallStreamObserver<SpectateTowerAttackResponse>) streamObserver;
            // `setOnCancelHandler` must be called before any `onNext` calls
            callObserver.setOnCancelHandler(() -> onSpectatorStreamCancellation(session, spectatingPlayerId));

            // send canvas state to spectator
            streamObserver.onNext(responseBuilder.build());
        }
        else {
            // error occurred
            logger.info("spectateTowerBySessionId: cannot enter tower spectating, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());

            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }


    /**
     * Changes the attacker that is being spectated by the player who called this method. Spectator specifies if he/she wants to
     * spectate next or previous attacker.
     *
     * Changes are made to the {@code StreamObserver<AttackSessionIdResponse>} object that is store inside a session,
     * here we only return <code>ActionResultResponse</code>: success or failure.
     */
    @Override
    public synchronized void toggleAttacker(ToggleAttackerRequest request, StreamObserver<SessionIdResponse> streamObserver) {
        final int spectatingPlayerId = request.getPlayerData().getPlayerId();

        SessionIdResponse.Builder responseBuilder = SessionIdResponse.newBuilder();
        Optional<AttackSession> sessionOpt = sessionManager.getSessionById(request.getSessionId());

        Optional<ServerError> serverError = validateSpectatingAction(
                sessionOpt, spectatingPlayerId, SpectatingRequirement.SPECTATING_REQUIRED);

        if (serverError.isEmpty()) {
            AttackSession session = sessionOpt.get();
            // changing the under spectating player
            Spectator spectator = session.pollSpectatorById(spectatingPlayerId).get();

            AttackSession newSession;

            logger.info("Getting getKthNeighbourOfSession of current sessionId=" + session.getId() + ", for towerId=" + session.getAttackedTowerId());

            if (request.getRequestType() == ToggleAttackerRequest.RequestType.SHOW_NEXT_ATTACKER) {
                newSession = sessionManager.getKthNeighbourOfSession(session.getAttackedTowerId(), session, 1);
            }
            else {
                newSession = sessionManager.getKthNeighbourOfSession(session.getAttackedTowerId(), session, -1);
            }

            // forming response
            responseBuilder.setSessionId(newSession.getId()).build();
            newSession.addSpectator(spectator.playerId(), spectator.streamObserver());

            // create response with canvas state
            SpectateTowerAttackResponse.Builder canvasStateResponseBuilder = SpectateTowerAttackResponse.newBuilder();
            canvasStateResponseBuilder.setResponseType(ResponseType.CURRENT_CANVAS_STATE);
            addCurrentSpellIfExists(canvasStateResponseBuilder, newSession);
            addSpellDescriptionsOfDrawnSpells(canvasStateResponseBuilder, newSession);
            // build canvas
            canvasStateResponseBuilder.getCanvasStateBuilder().build();
            // setting remaining time
            canvasStateResponseBuilder.setLeftTimeMs(newSession.getLeftExecutionTimeMs());

            // send canvas state to spectator
            spectator.streamObserver().onNext(canvasStateResponseBuilder.build());
        }
        else {
            // error occurred
            logger.info("Cannot toggle attack session, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public synchronized void castDefendSpell(CastDefendSpellRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        final int spectatingPlayerId = request.getPlayerData().getPlayerId();
        final int towerId = request.getTowerId();
        final int defendSpellId = request.getDefendSpellId();

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();
        Optional<ServerError> serverError = validateDefendSpellCast(spectatingPlayerId, towerId, defendSpellId);

        if (serverError.isEmpty()) {
            defendSpellsManager.addDefendSpell(towerId, defendSpellId, () -> {
                logger.info("Remove defend spell with id " + defendSpellId + " from tower with id " + towerId);
                notifyTowerAttackersWithDefendSpellUpdate(towerId, defendSpellId, SessionStateInfoResponse.ResponseType.REMOVE_DEFEND_SPELL);
                notifyTowerSpectatorsWithDefendSpellUpdate(towerId, defendSpellId, ResponseType.REMOVE_DEFEND_SPELL);

                // remove spell from manager
                defendSpellsManager.removeDefendSpell(towerId, defendSpellId);
            });

            logger.info("Add defend spell with id " + defendSpellId + " to tower with id " + towerId);
            notifyTowerAttackersWithDefendSpellUpdate(towerId, defendSpellId, SessionStateInfoResponse.ResponseType.ADD_DEFEND_SPELL);
            notifyTowerSpectatorsWithDefendSpellUpdate(towerId, defendSpellId, ResponseType.ADD_DEFEND_SPELL);
            responseBuilder.setSuccess(true);
        }
        else {
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    private void notifyTowerAttackersWithDefendSpellUpdate(int towerId, int defendSpellId, SessionStateInfoResponse.ResponseType type) {
        if (type != SessionStateInfoResponse.ResponseType.ADD_DEFEND_SPELL && type != SessionStateInfoResponse.ResponseType.REMOVE_DEFEND_SPELL) {
            logger.warning("Call notifyTowerAttackersWithDefendSpellUpdate with response type of `ADD_DEFEND_SPELL` or `REMOVE_DEFEND_SPELL`");
            return;
        }

        var attackers = sessionManager.getAttackersAssociatedWithTowerId(towerId);
        SessionStateInfoResponse notification = SessionStateInfoResponse.newBuilder()
            .setType(type)
            .setDefendSpellId(defendSpellId)
            .build();

        for (var attacker : attackers) {
            attacker.onNext(notification);
        }
    }

    private void notifyTowerSpectatorsWithDefendSpellUpdate(int towerId, int defendSpellId, SpectateTowerAttackResponse.ResponseType type) {
        if (type != SpectateTowerAttackResponse.ResponseType.ADD_DEFEND_SPELL && type != SpectateTowerAttackResponse.ResponseType.REMOVE_DEFEND_SPELL) {
            logger.warning("Call notifyTowerSpectatorsWithDefendSpellUpdate with response type of `ADD_DEFEND_SPELL` or `REMOVE_DEFEND_SPELL`");
            return;
        }

        var spectators = sessionManager.getSpectatorsAssociatedWithTowerId(towerId);
        SpectateTowerAttackResponse notification = SpectateTowerAttackResponse.newBuilder()
            .setResponseType(type)
            .setDefendSpellId(defendSpellId)
            .build();

        for (var spectator : spectators) {
            spectator.onNext(notification);
        }
    }

    private Optional<ServerError> validateDefendSpellCast(int spectatingPlayerId, int towerId, int defendSpellId) {
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);
        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (towerOpt.isEmpty()) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                ErrorType.TOWER_NOT_FOUND,
                "tower with id " + towerId + " not found");
        }
        else if (towerOpt.get().getOwnerId().isEmpty() || towerOpt.get().getOwnerId().get() != spectatingPlayerId) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                ErrorType.INVALID_REQUEST,
                "player with id " + spectatingPlayerId + " does not own tower with id " + towerId);
        }
        else if (SpellBook.getDefendSpellTemplateById(defendSpellId) == null) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                "defend spell template with id " + defendSpellId + " not found");
        }
        else if (defendSpellsManager.isDefendSpellAlreadyCasted(towerId, defendSpellId)) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                ErrorType.INVALID_REQUEST,
                "Defend spell with id '" + defendSpellId + "' already casted");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }

        return Optional.empty();
    }

    /**
     * Finds spectator with provided player id in the requested attack session.
     * Then closes the connection with the spectator and removes it from session spectators list.
     */
    @Override
    public synchronized void leaveSpectating(LeaveSpectatingRequest request, StreamObserver<ActionResultResponse> streamObserver) {
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
                ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.INVALID_REQUEST,
                        "Session with id " + sessionId + " does not have spectator with id " + spectatingPlayerId);
            }
        }
        else {
            // session not found
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.SESSION_NOT_FOUND,
                    "Attack session with provided id " + sessionId + " not found");
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }




    // helper methods

    private static boolean isProtectionWallDestroyed(Map<SpellType, Double> matches) {
        boolean destroyed = true;
        for (var match : matches.values()) {
            if (match < MINIMUM_REQUIRED_MATCH_COEFFICIENT) {
                destroyed = false;
                break;
            }
        }
        return destroyed;
    }

    /**
     * <p>Validates that player can start attack session of the tower.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Tower exists</li>
     *     <li>Player is not an owner of the tower</li>
     *     <li>Tower is protected by at least one protection wall</li>
     *     <li>Player is not attacking another tower</li>
     *     <li>Player is not in the spectating mode</li>
     *     <li>Tower is not under capture lock</li>
     *     <li>Tower is not under protection wall modification</li>
     * </ol>
     */
    private Optional<ServerError> validateAttackingTowerById(TowerIdRequest request) {
        int playerId = request.getPlayerData().getPlayerId();
        int towerId = request.getTowerId();

        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean isPlayerAttacking = sessionManager.hasSessionAssociatedWithPlayerId(playerId);

        boolean isPlayerSpectating = sessionManager.isPlayerInSpectatingMode(playerId);

        boolean towerExists = towerOpt.isPresent();

        boolean isPlayerOwner = towerExists && towerOpt.get().getOwnerId().isPresent() && towerOpt.get().getOwnerId().get() == playerId;

        boolean isTowerProtected = towerExists && towerOpt.get().isProtected();

        boolean isTowerUnderCaptureLock = towerExists && towerOpt.get().isUnderCaptureLock();

        boolean isTowerUnderProtectionWallsInstallation = towerExists && towerOpt.get().isUnderProtectionWallsInstallation();

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (isPlayerAttacking) {
            errorOccurred = true;
            // if player is already in attack session
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is already in attack session");
        }
        else if (isPlayerSpectating) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + playerId + " is already spectating someone's attack session");
        }
        if (!towerExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (isPlayerOwner) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + request.getPlayerData().getPlayerId() +
                            " is an owner of tower with id " + towerId);
        }
        else if (!isTowerProtected) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " has no enchanted protection walls");
        }
        else if (isTowerUnderCaptureLock) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is under capture lock");
        }
        else if (isTowerUnderProtectionWallsInstallation) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is under protection wall installation");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }
    }


    /**
     * <p>Validates requests that are related to modifying canvas or changing canvas related state.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Request type is valid</li>
     *     <li>Attack session with provided id exists</li>
     *     <li>Player's id matches with the attacker id stored in the attack session</li>
     * </ol>
     */
    private Optional<ServerError> validateCanvasAction(SpellRequest request, RequestType requiredRequestType) {
        int playerId = request.getPlayerData().getPlayerId();
        Optional<AttackSession> session = sessionManager.getSessionById(request.getSessionId());

        boolean additionalRequestCheck = switch(requiredRequestType) {
            case SELECT_SPELL_TYPE -> request.hasSpellType();
            case DRAW_SPELL -> request.hasDrawSpell();
            case FINISH_SPELL -> request.hasFinishSpell();
            case CLEAR_CANVAS, COMPARE_DRAWN_SPELLS -> true;
            case UNRECOGNIZED -> false;
        };

        boolean isRequestValid = (request.getRequestType() == requiredRequestType) && additionalRequestCheck;
        boolean sessionExists = session.isPresent();
        boolean attackerIdMatchesPlayerId = sessionExists && playerId == session.get().getAttackingPlayerId();

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (!isRequestValid) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
            "Invalid request: request type must be '" +
                    requiredRequestType + "'" +
                    (!additionalRequestCheck ? ", additional request check failed" : ""));
        }
        else if (!sessionExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.SESSION_NOT_FOUND,
            "Attack session with id " + request.getSessionId() + " not found");
        }
        else if (!attackerIdMatchesPlayerId) {
            errorOccurred = true;
            int attackerId = session.get().getAttackingPlayerId();
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
            "Attacker id " + attackerId + " does not match player id " + playerId);
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return  Optional.empty();
        }
    }


    private enum SpectatingRequirement {
        SPECTATING_REQUIRED,
        SPECTATING_PROHIBITED,
    }
    /**
     * <p>Validates requests that are dealing with spectating functionalities</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Player must not be in attacking mode</li>
     *     <li>Attack session must exist</li>
     *     <li>Player must either be or not be in spectating mode depending on <code>requirement</code> param</li>
     * </ol>
     */
    private Optional<ServerError> validateSpectatingAction(Optional<AttackSession> session, int playerId, SpectatingRequirement requirement) {
        boolean isAttacking = sessionManager.hasSessionAssociatedWithPlayerId(playerId);
        boolean sessionExists = session.isPresent();
        boolean isSpectating = sessionManager.isPlayerInSpectatingMode(playerId);

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (isAttacking) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
            "player with id " + playerId + " is in attacking mode");
        }
        else if (!sessionExists) {
            errorOccurred = true;
            // session not found
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.SESSION_NOT_FOUND,
            "Attack session not found");
        }
        else if (requirement == SpectatingRequirement.SPECTATING_REQUIRED && !isSpectating) {
            errorOccurred = true;
            // player is not spectating
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
            "player with id " + playerId + " is not spectating");
        }
        else if (requirement == SpectatingRequirement.SPECTATING_PROHIBITED && isSpectating) {
            errorOccurred = true;
            // player is spectating
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
            "player with id " + playerId + " is spectating");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }
    }


    /**
     * <p>Removes the under-attack state of the underlying {@link Tower} of the provided {@link AttackSession}, disconnects all spectators, and removes the {@link AttackSession} from {@link AttackSessionManager}.</p>
     * <p>Callback should be called in the case if client closes the connection, i.e. {@link ServerCallStreamObserver#setOnCancelHandler} method of attacker's {@link StreamObserver} should fire this callback.</p>
     */
    private void onAttackerStreamCancellation(AttackSession session) {
        logger.info("Attacker with id " + session.getAttackingPlayerId() +
                    " registered in attack session with id " + session.getId() +
                    " cancelled stream. Destroying the corresponding attack session...");

        // removing session from sessions list
        sessionManager.remove(session);

        // if no other attackers attack the same tower -> unset under attack state
        if (sessionManager.getAnyAttackSessionByTowerId(session.getAttackedTowerId()).isEmpty()) {
            // mark tower to not be under attack
            TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(session.getAttackedTowerId());
            interactor.unsetTowerUnderAttackState();
        }

        // disconnecting spectators
        disconnectSpectators(session);

        // notifying listeners of tower update
        TowersUpdatesMediator.getInstance().notifyObservers(List.of(session.getAttackedTowerId()));
    }

    /**
     * <p>Invalidates spectator in {@link AttackSession} for the future removal.</p>
     * <p>Callback should be called in the case if client closes the connection, i.e. {@link ServerCallStreamObserver#setOnCancelHandler} method of spectator's {@link StreamObserver} should fire this callback.</p>
     */
    private void onSpectatorStreamCancellation(AttackSession session, int spectatingPlayerId) {
        logger.info("Spectator with id " + spectatingPlayerId + " cancelled stream. Invalidating the spectator...");
        session.invalidateSpectator(spectatingPlayerId);
    }

    /**
     * <p>The callback is intended to be fired once the session expires.</p>
     * <p>Callback is <code>synchronized</code> because {@link AttackSession} fires it in another thread using {@link Timeout} utility.</p>
     */
    private synchronized void onSessionExpired(int sessionId) {
        Optional<AttackSession> sessionOpt = sessionManager.getSessionById(sessionId);

        if (sessionOpt.isPresent()) {
            AttackSession session = sessionOpt.get();
            logger.info("onSessionExpired: session with id " + sessionId + " expired");
            closeSessionWithCorrectDisconnections(session);
        }
        else {
            // no session found
            logger.info("onSessionExpired: session with id " + sessionId + " not found");
        }
    }

    /**
     * <p><b>The method does all the following actions:</b></p>
     * <ol>
     *     <li>Marks the tower as not being under attack any more</li>
     *     <li>Disconnects all spectators</li>
     *     <li>Sends response with session expired state to the attacker and disconnects him</li>
     * </ol>
     */
    private void closeSessionWithCorrectDisconnections(AttackSession session) {
        // mark tower to not be under attack
        TowerAttackServiceInteractor interactor = new TowerAttackServiceInteractor(session.getAttackedTowerId());
        interactor.unsetTowerUnderAttackState();

        // closing connection with spectators
        disconnectSpectators(session);

        // sending response with session expiration to attacker and closing connection
        SessionStateInfoResponse.Builder responseBuilder = SessionStateInfoResponse.newBuilder();
        responseBuilder.setType(SessionStateInfoResponse.ResponseType.SESSION_EXPIRED);
        responseBuilder.getExpirationBuilder().build();

        // closing connection with attacker
        var attackerResponseObserver = session.getAttackerResponseObserver();
        attackerResponseObserver.onNext(responseBuilder.build());
        attackerResponseObserver.onCompleted();

        // notifying listeners of tower update
        TowersUpdatesMediator.getInstance().notifyObservers(List.of(session.getAttackedTowerId()));

        sessionManager.remove(session);
    }

    /**
     * Calls {@link StreamObserver#onCompleted} on all spectators stored inside provided {@link AttackSession}.
     */
    private void disconnectSpectators(AttackSession session) {
        logger.info("Disconnecting spectators of session with id " + session.getId());
        for (var spectator : session.getSpectators()) {
            // TODO: send 'AttackSessionExpired' to spectators
            spectator.streamObserver().onCompleted();
        }
    }

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
                .setSpellType(session.getCurrentSpellType())
                .addAllPoints(points)
                .build();
        }
    }

    /**
     * Adds spell descriptions (i.e. {@link SpellDescription} instances) of already drawn spells into {@link SpectateTowerAttackResponse} response.
     */
    private void addSpellDescriptionsOfDrawnSpells(SpectateTowerAttackResponse.Builder responseBuilder, AttackSession session) {
        List<SpellDescription> spellDescriptionResponses = new ArrayList<>();

        // collecting building descriptions of all already drawn spells on canvas
        for (var spellDescription : session.getDrawnSpellsDescriptions()) {
            SpellDescription.Builder spellDescriptionResponseBuilder = SpellDescription.newBuilder();

            // add spell template offset
            Vector2 offset = spellDescription.offset();
            spellDescriptionResponseBuilder.getSpellTemplateOffsetBuilder()
                .setX(offset.x)
                .setY(offset.y)
                .build();

            // add the other fields
            int spellTemplateId = spellDescription.id();
            SpellType spellType = spellDescription.spellType();
            spellDescriptionResponseBuilder
                .setSpellType(spellType)
                .setSpellTemplateId(spellTemplateId);

            spellDescriptionResponses.add(spellDescriptionResponseBuilder.build());
        }

        // adding spell descriptions into canvas state response
        var canvasBuilder = responseBuilder.getCanvasStateBuilder();
        canvasBuilder.addAllSpellDescriptions(spellDescriptionResponses);
    }
}
