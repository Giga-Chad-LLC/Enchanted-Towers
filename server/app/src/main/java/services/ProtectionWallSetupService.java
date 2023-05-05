package services;

import components.session.AttackSession;
import components.session.AttackSessionManager;
import components.session.ProtectionWallSession;
import components.session.ProtectionWallSessionManager;
import components.time.Timeout;
import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.requests.EnterProtectionWallCreationRequest;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.responses.SpellFinishResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.TemplateDescription;
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.registry.TowersRegistry;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;


// TODO: add rpc methods descriptions

public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {
    // TODO: test that logic that involves these constants works correctly
    private static final long CAPTURE_LOCK_TIMEOUT_MS = 5 * 1000; // 5s   /* 30 * 60 * 1000; // 30min */
    private static final long SESSION_CREATION_COOLDOWN_MS = 10 * 1000; // 10s   /* 24 * 60 * 60 * 1000; // 24h */

    private final Logger logger = Logger.getLogger(ProtectionWallSetupService.class.getName());
    private final IntConsumer onSessionExpiredCallback = this::onSessionExpired;
    private final ProtectionWallSessionManager sessionManager = new ProtectionWallSessionManager();
    private final Map<Integer, Timeout> timeouts = new HashMap<>();

    // TODO: check distance between player and tower

    // TODO: add synchronized on all rpc methods
    /**
     * <p>Starts timeout before the trigger of which creation of new protection walls is allowed, and tower is set to be under capture lock during which no players are allowed to attack the tower.</p>
     * <p>Once the timeout triggers capture lock is removed and tower can be attacked, and protection walls can be installed only once in {@link ProtectionWallSetupService#SESSION_CREATION_COOLDOWN_MS} time period.</p>
     */
    @Override
    public void captureTower(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateTowerCapturingRequest(request);

        if (serverError.isEmpty()) {
            int towerId = request.getTowerId();
            int playerId = request.getPlayerData().getPlayerId();

            // tower may be captured
            Tower tower = TowersRegistry.getInstance().getTowerById(towerId).get();

            // make player an owner of tower and allow to set up protection walls
            tower.setOwnerId(playerId);
            tower.setUnderCaptureLock(true);

            // TODO: what if player is already in creation session?
            // setting timeout of protection walls installation
            timeouts.put(towerId, new Timeout(CAPTURE_LOCK_TIMEOUT_MS, () -> {
                logger.info("Remove capture lock for tower with id " + tower.getId() + " of owner with id " + playerId);
                tower.setLastProtectionWallModificationTimestamp(Instant.now());
                tower.setUnderCaptureLock(false);
                // removing timeout from map
                timeouts.remove(towerId);
            }));

            logger.info("Successful capture of tower with id " + towerId + " by player with id " + playerId);

            responseBuilder.setSuccess(true);
        }
        else {
            // error occurred
            logger.info("Tower cannot be captured, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    @Override
    public void tryEnterProtectionWallCreationSession(EnterProtectionWallCreationRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateEnteringProtectionWallCreationSession(request);

        if (serverError.isEmpty()) {
            logger.info("Player with id " + request.getPlayerData().getPlayerId() +
                    " can enter creation session of protection wall with id " +
                    request.getProtectionWallId() + " of tower with id " + request.getTowerId());

            responseBuilder.setSuccess(true);
        }
        else {
            logger.info("Creation session cannot be entered, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void enterProtectionWallCreationSession(EnterProtectionWallCreationRequest request, StreamObserver<SessionInfoResponse> streamObserver) {
        SessionInfoResponse.Builder responseBuilder = SessionInfoResponse.newBuilder();

        Optional<ServerError> serverError = validateEnteringProtectionWallCreationSession(request);

        if (serverError.isEmpty()) {
            // create protection wall creation session
            int playerId = request.getPlayerData().getPlayerId();
            int towerId = request.getTowerId();
            int protectionWallId = request.getProtectionWallId();

            logger.info("Player with id " + playerId + " successfully entered creation session of protection wall with id " +
                    protectionWallId + " of tower with id " + towerId);

            ProtectionWallSession session = sessionManager.createSession(
                    playerId, towerId, protectionWallId, streamObserver, onSessionExpiredCallback);

            Tower tower = TowersRegistry.getInstance().getTowerById(towerId).get();
            // block other players from attacking the tower
            tower.setUnderProtectionWallsInstallation(true);

            int sessionId = session.getId();

            // create cancel handler to hook the event of client closing the connection
            var callObserver = (ServerCallStreamObserver<SessionInfoResponse>) streamObserver;
            // `setOnCancelHandler` must be called before any `onNext` calls
            callObserver.setOnCancelHandler(() -> onProtectionWallCreatorStreamCancellation(session));

            // send session id to client
            responseBuilder.setType(SessionInfoResponse.ResponseType.SESSION_ID);
            responseBuilder.getSessionBuilder().setSessionId(sessionId);

            streamObserver.onNext(responseBuilder.build());
        }
        else {
            // error
            logger.info("Creation session cannot be entered, reason: '" + serverError.get().getMessage() + "'");

            responseBuilder.setError(serverError.get());
            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }

    @Override
    public void addSpell(ProtectionWallRequest request, StreamObserver<SpellFinishResponse> streamObserver) {
        SpellFinishResponse.Builder responseBuilder = SpellFinishResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);

        boolean isRequestValid = (request.getRequestType() == ProtectionWallRequest.RequestType.ADD_SPELL) &&
                                 (request.hasSpell());
        boolean sessionExists = isRequestValid && sessionOpt.isPresent();
        boolean isPlayerIdValid = sessionExists && playerId == sessionOpt.get().getPlayerId();

        if (isRequestValid && sessionExists && isPlayerIdValid) {
            // find matching spell template
            List<Vector2> spellPoints = request.getSpell().getPointsList().stream()
                    .map(point -> new Vector2(point.getX(), point.getY()))
                    .toList();

            Vector2 offset = new Vector2(
                    request.getSpell().getOffset().getX(),
                    request.getSpell().getOffset().getY()
            );

            logger.info("addSpell: run hausdorff and return id of matched template and offset");

            Optional<TemplateDescription> matchedTemplateDescriptionOpt = SpellsPatternMatchingAlgorithm.getMatchedTemplateWithHausdorffMetric(
                    spellPoints,
                    offset,
                    request.getSpell().getSpellType()
            );

            // add match spell into canvas state
            if (matchedTemplateDescriptionOpt.isPresent()) {
                TemplateDescription template = matchedTemplateDescriptionOpt.get();
                ProtectionWallSession session = sessionOpt.get();

                session.addTemplateToCanvasState(template);

                // setting template in response
                {
                    responseBuilder.getSpellDescriptionBuilder()
                            .setSpellTemplateId(template.id())
                            .setSpellType(template.spellType());

                    responseBuilder.getSpellDescriptionBuilder()
                            .getSpellTemplateOffsetBuilder()
                            .setX(template.offset().x)
                            .setY(template.offset().y)
                            .build();

                    responseBuilder.getSpellDescriptionBuilder().build();
                }
            }
            else {
                // no template found
                ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                        ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND,
                        "No template found to match provided spell");
            }
        }
        else if (!isRequestValid) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
            "Request type must be '" + ProtectionWallRequest.RequestType.ADD_SPELL + "'" +
                    ", got '" + request.getRequestType() + "'" +
                    ", as well as Spell must be provided");
        }
        else if (!sessionExists) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.SESSION_NOT_FOUND,
                    "ProtectionWallSession with id " + sessionId + " not found");
        }
        else {
            // player id is not the same as the player id stored inside session instance
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player id " + playerId + " does not match the required id " + sessionOpt.get().getPlayerId());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void clearCanvas(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);

        boolean isRequestValid = (request.getRequestType() == ProtectionWallRequest.RequestType.CLEAR_CANVAS);
        boolean sessionExists = isRequestValid && sessionOpt.isPresent();
        boolean isPlayerIdValid = sessionExists && playerId == sessionOpt.get().getPlayerId();

        if (isRequestValid && sessionExists && isPlayerIdValid) {
            ProtectionWallSession session = sessionOpt.get();
            // clearing canvas
            session.clearDrawnSpellsDescriptions();
            responseBuilder.setSuccess(true);

            logger.info("Canvas of session with id " + sessionId + " cleared successfully by player with id " + playerId);
        }
        else if (!isRequestValid) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Request type must be '" + ProtectionWallRequest.RequestType.CLEAR_CANVAS + "'" +
                            ", got '" + request.getRequestType() + "'");
        }
        else if (!sessionExists) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.SESSION_NOT_FOUND,
                    "ProtectionWallSession with id " + sessionId + " not found");
        }
        else {
            // player id is not the same as the player id stored inside session instance
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player id " + playerId + " does not match the required id " + sessionOpt.get().getPlayerId());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void completeEnchantment(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);

        boolean isRequestValid = (request.getRequestType() == ProtectionWallRequest.RequestType.COMPLETE_ENCHANTMENT);
        boolean sessionExists = sessionOpt.isPresent();
        boolean isPlayerIdValid = sessionExists && playerId == sessionOpt.get().getPlayerId();

        if (isRequestValid && sessionExists && isPlayerIdValid) {
            logger.info("Completing enchantment for session with id " + sessionId + " of player with id " + playerId);

            // complete enchantment and save it into protection wall of the tower
            ProtectionWallSession session = sessionOpt.get();
            Tower tower = TowersRegistry.getInstance().getTowerById(session.getTowerId()).get();

            // create enchantment from spell templates
            Enchantment enchantment = new Enchantment(session.getTemplateDescriptions());

            ProtectionWall wall = tower.getProtectionWallById(session.getProtectionWallId()).get();
            wall.setEnchantment(enchantment);

            // tower is no longer under protection wall installation
             tower.setUnderProtectionWallsInstallation(false);

            // TODO: call TowerRegistry.save() to save the updated tower state in DB

            logger.info("New enchantment of protection wall with id " + wall.getId() +
                             " of tower with id " + tower.getId() + " installed");

            responseBuilder.setSuccess(true);
            // closing connection with client
            session.getPlayerResponseObserver().onCompleted();

            // removing session
            sessionManager.remove(session);
        }
        else if (!isRequestValid) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
            "Request type must be '" + ProtectionWallRequest.RequestType.COMPLETE_ENCHANTMENT + "'" +
                    ", got '" + request.getRequestType() + "'");
        }
        else if (!sessionExists) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.SESSION_NOT_FOUND,
            "ProtectionWallSession with id " + sessionId + " not found");
        }
        else {
            // player id is not the same as the player id stored inside session instance
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player id " + playerId + " does not match the required id " + sessionOpt.get().getPlayerId());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    // TODO: create destroyEnchantment method that destroys enchantment of protection wall

    // helper methods
    /**
     * <p>Validates the request and checks the conditions required to enter protection wall creation session.
     * If validation fails then sets the appropriate error into response; otherwise does nothing.</p>
     * <p>Caller should check for an error instance being set in a response model.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Tower with provided id exists</li>
     *     <li>Player is an owner of the tower</li>
     *     <li>Tower is not being attacked</li>
     *     <li>Protection wall with provided id exists inside tower</li>
     *     <li>Protection wall is not enchanted (<b>player must destroy enchantment before creating new one</b>)</li>
     *     <li><b>One of the following is true:</b></li>
     *     <ol>
     *         <li>Tower is under capture lock</li>
     *         <li>Protection wall modification allowed: either tower was not modified by an owner yet or cooldown before subsequence modifications exceeded</li>
     *     </ol>
     * </ol>
     */
    private Optional<ServerError> validateEnteringProtectionWallCreationSession(EnterProtectionWallCreationRequest request) {
        int towerId = request.getTowerId();
        int protectionWallId = request.getProtectionWallId();
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists = towerOpt.isPresent();

        boolean isPlayerOwner = towerExists && towerOpt.get().getOwnerId().isPresent() &&
                towerOpt.get().getOwnerId().get() == request.getPlayerData().getPlayerId();

        boolean isTowerUnderAttack = towerExists && towerOpt.get().isUnderAttack();

        boolean isTowerUnderCaptureLock = towerExists && towerOpt.get().isUnderCaptureLock();

        boolean protectionWallExists = towerExists && towerOpt.get().hasProtectionWallWithId(protectionWallId);

        boolean protectionWallIsEnchanted = protectionWallExists &&
                    towerOpt.get().getProtectionWallById(protectionWallId).get().isEnchanted();

        boolean hasLastProtectionWallModificationTimestamp =
                towerExists && towerOpt.get().getLastProtectionWallModificationTimestamp().isPresent();

        boolean cooldownTimeExceeded = hasLastProtectionWallModificationTimestamp &&
                cooldownTimeBetweenSessionsCreationExceeded(towerOpt.get().getLastProtectionWallModificationTimestamp().get());

        boolean protectionWallModificationAllowed = !hasLastProtectionWallModificationTimestamp || cooldownTimeExceeded;

        logger.info("hasLastProtectionWallModificationTimestamp=" + hasLastProtectionWallModificationTimestamp +
                ", cooldownTimeExceeded=" + cooldownTimeExceeded);

        logger.info("protectionWallModificationAllowed=" + protectionWallModificationAllowed +
                ", isTowerUnderCaptureLock=" + isTowerUnderCaptureLock);

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (!towerExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (!isPlayerOwner) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + request.getPlayerData().getPlayerId() +
                            " is not an owner of tower with id " + towerId);
        }
        else if (isTowerUnderAttack) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is being attacked");
        }
        else if (!protectionWallExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Protection wall with id " + protectionWallId + " of tower with id " + towerId + " not found");
        }
        else if (protectionWallIsEnchanted) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Protection wall with id " + protectionWallId + " of tower with id " + towerId + " already enchanted");
        }
        else if (!protectionWallModificationAllowed && !isTowerUnderCaptureLock) {
            // modification not allowed because cooldown not exceeded AND tower is not under capture lock
            errorOccurred = true;
            var timestamp = towerOpt.get().getLastProtectionWallModificationTimestamp().get();
            long remain = SESSION_CREATION_COOLDOWN_MS - Duration.between(timestamp, Instant.now()).toSeconds();

            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Cooldown after last protection wall installation not exceeded: " + remain +
                            "s remains, and tower is not under capture lock");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }
    }


    /**
     * <p>Validates that request of capturing tower may be fulfilled.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Tower with provided id exists</li>
     *     <li>Player <b>is not</b> an owner of the tower</li>
     *     <li>Tower is not under capture lock of other player</li>
     *     <li>Tower is not protected by any protection walls</li>
     * </ol>
     */
    private Optional<ServerError> validateTowerCapturingRequest(TowerIdRequest request) {
        int towerId = request.getTowerId();
        int playerId = request.getPlayerData().getPlayerId();

        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists = towerOpt.isPresent();

        boolean towerAlreadyOwnedByPlayer =
                towerExists && towerOpt.get().getOwnerId().isPresent() && towerOpt.get().getOwnerId().get() == playerId;

        boolean towerUnderCaptureLockOfOtherPlayer =
                towerExists && !towerAlreadyOwnedByPlayer && towerOpt.get().isUnderCaptureLock();

        boolean towerProtected = towerExists && towerOpt.get().isProtected();


        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (!towerExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (towerAlreadyOwnedByPlayer) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is already owned by player with id " + playerId);
        }
        else if (towerUnderCaptureLockOfOtherPlayer) {
            errorOccurred = true;
            int ownerId = towerOpt.get().getOwnerId().get();
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is already under capture lock of player with id " + ownerId);
        }
        else if (towerProtected) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " protected by player with id " + towerOpt.get().getOwnerId().get());
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }
    }


    private boolean cooldownTimeBetweenSessionsCreationExceeded(Instant timestamp) {
        long elapsedTime_ms = Duration.between(timestamp, Instant.now()).toMillis();
        logger.info("Got timestamp: " + timestamp + ", elapsed time " + elapsedTime_ms + "ms");
        return elapsedTime_ms >= SESSION_CREATION_COOLDOWN_MS;
    }

    /**
     * <p>Removes the under-protection-wall-installation state of the underlying {@link Tower} of the provided {@link ProtectionWallSession}, and removes the {@link ProtectionWallSession} from {@link ProtectionWallSessionManager}.</p>
     * <p>Callback should be called in the case if client closes the connection, i.e. {@link ServerCallStreamObserver#setOnCancelHandler} method of protection wall creator's {@link StreamObserver} should fire this callback.</p>
     */
    private void onProtectionWallCreatorStreamCancellation(ProtectionWallSession session) {
        logger.info("Player with id " + session.getPlayerId() +
                " cancelled stream. Destroying corresponding protection wall session with id " +
                session.getId() + "...");

        Tower tower = TowersRegistry.getInstance().getTowerById(session.getTowerId()).get();
        // unblock other players from attacking the tower
        tower.setUnderProtectionWallsInstallation(false);
        sessionManager.remove(session);
    }

    private void onSessionExpired(int sessionId) {
        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);
        // TODO: better to ignore callback rather than throw
        if (sessionOpt.isEmpty()) {
            throw new NoSuchElementException("SessionExpiredCallback: session with id " + sessionId + " not found");
        }
        ProtectionWallSession session = sessionOpt.get();

        // unsetting protection creation state
        Tower tower = TowersRegistry.getInstance().getTowerById(session.getTowerId()).get();
        // unblock players from attacking the tower
        tower.setUnderProtectionWallsInstallation(false);

        // sending response with session expiration and closing connection
        SessionInfoResponse.Builder responseBuilder = SessionInfoResponse.newBuilder();
        responseBuilder.setType(SessionInfoResponse.ResponseType.SESSION_EXPIRED);
        responseBuilder.getExpirationBuilder().build();

        var responseObserver = session.getPlayerResponseObserver();
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();

        // removing session
        sessionManager.remove(session);
    }
}
