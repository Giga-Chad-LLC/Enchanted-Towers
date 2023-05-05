package services;

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
import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.TemplateDescription;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.registry.TowersRegistry;
import enchantedtowers.game_models.utils.Vector2;
import interactors.ProtectionWallSetupServiceInteractor;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;


public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {
    // TODO: test that logic that involves these constants works correctly
    private static final long CAPTURE_LOCK_TIMEOUT_MS = 5 * 1000; // 5s   /* 30 * 60 * 1000; // 30min */
    private static final long SESSION_CREATION_COOLDOWN_MS = 10 * 1000; // 10s   /* 24 * 60 * 60 * 1000; // 24h */

    private final Logger logger = Logger.getLogger(ProtectionWallSetupService.class.getName());
    private final IntConsumer onSessionExpiredCallback = this::onSessionExpired;
    private final ProtectionWallSessionManager sessionManager = new ProtectionWallSessionManager();
    private final Map<Integer, Timeout> timeouts = new HashMap<>();

    // TODO: check distance between player and tower

    /**
     * <p>Starts timeout before the trigger of which creation of new protection walls is allowed, and tower is set to be under capture lock during which no players are allowed to attack the tower.</p>
     * <p>Once the timeout triggers capture lock is removed and tower can be attacked, and protection walls can be installed only once in {@link ProtectionWallSetupService#SESSION_CREATION_COOLDOWN_MS} time period.</p>
     */
    @Override
    public synchronized void captureTower(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateTowerCapturingRequest(request);

        if (serverError.isEmpty()) {
            int towerId = request.getTowerId();
            int playerId = request.getPlayerData().getPlayerId();

            // tower may be captured
            ProtectionWallSetupServiceInteractor interactor = new ProtectionWallSetupServiceInteractor(towerId);
            // make player an owner of tower and allow to set up protection walls
            interactor.setTowerOwner(playerId);
            interactor.setCaptureLock();

            // setting timeout of protection walls installation
            timeouts.put(towerId, new Timeout(CAPTURE_LOCK_TIMEOUT_MS, () -> {
                logger.info("Remove capture lock for tower with id " + towerId + " of owner with id " + playerId);
                interactor.unsetCaptureLock();
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


    /**
     * <p>Validates request and sends either error if validation fails or success letting client enter protection wall creation session.</p>
     * <p>This method serves as a convenient check of ability to enter the creation session for clients, thus {@link ProtectionWallSetupService#enterProtectionWallCreationSession} is still required to make the appropriate validations.</p>
     */
    @Override
    public synchronized void tryEnterProtectionWallCreationSession(EnterProtectionWallCreationRequest request, StreamObserver<ActionResultResponse> streamObserver) {
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


    /**
     * Creates {@link ProtectionWallSession} associated with provided tower id and player id if the validation of request succeeds.
     */
    @Override
    public synchronized void enterProtectionWallCreationSession(EnterProtectionWallCreationRequest request, StreamObserver<SessionInfoResponse> streamObserver) {
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

            // setting tower in protection wall installation mode
            ProtectionWallSetupServiceInteractor interactor = new ProtectionWallSetupServiceInteractor(towerId);
            interactor.setProtectionWallInstallation();

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


    /**
     * Matches a drawn spell with templates and either sends error if no templates matched or stores the matched template in {@link ProtectionWallSession} and sends template id to the client.
     */
    @Override
    public synchronized void addSpell(ProtectionWallRequest request, StreamObserver<SpellFinishResponse> streamObserver) {
        SpellFinishResponse.Builder responseBuilder = SpellFinishResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, ProtectionWallRequest.RequestType.ADD_SPELL);

        if (serverError.isEmpty()) {
            // find matching spell template
            List<Vector2> spellPoints = request.getSpell().getPointsList().stream()
                    .map(point -> new Vector2(point.getX(), point.getY()))
                    .toList();

            Vector2 offset = new Vector2(
                    request.getSpell().getOffset().getX(),
                    request.getSpell().getOffset().getY()
                );

            logger.info("addSpell: run hausdorff and return id of matched template and offset");

            Optional<TemplateDescription> matchedTemplateDescriptionOpt =
                    SpellsPatternMatchingAlgorithm.getMatchedTemplateWithHausdorffMetric(
                        spellPoints,
                        offset,
                        request.getSpell().getSpellType()
                    );

            // add match spell into canvas state
            if (matchedTemplateDescriptionOpt.isPresent()) {
                TemplateDescription template = matchedTemplateDescriptionOpt.get();
                ProtectionWallSession session = sessionManager.getSessionById(request.getSessionId()).get();

                // adding matched template into session canvas state
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
        else {
            // error occurred
            logger.info("Cannot add spell, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    /**
     * Removes stored spell templates from {@link ProtectionWallSession}.
     */
    @Override
    public synchronized void clearCanvas(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, ProtectionWallRequest.RequestType.CLEAR_CANVAS);

        if (serverError.isEmpty()) {
            ProtectionWallSession session = sessionManager.getSessionById(request.getSessionId()).get();
            // clearing canvas
            session.clearDrawnSpellsDescriptions();
            responseBuilder.setSuccess(true);

            logger.info("Canvas of session with id " + session.getId() +
                    " cleared successfully by player with id " + session.getPlayerId());
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
     * Creates {@link Enchantment} from spell templates stored in {@link ProtectionWallSession}, saves the enchantment in the corresponding {@link Tower}. After that closes the connection with client and removes the session from {@link ProtectionWallSessionManager}.
     */
    @Override
    public synchronized void completeEnchantment(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateCanvasAction(request, ProtectionWallRequest.RequestType.COMPLETE_ENCHANTMENT);

        if (serverError.isEmpty()) {
            ProtectionWallSession session = sessionManager.getSessionById(request.getSessionId()).get();

            logger.info("Completing enchantment for session with id " + session.getId() +
                    " of player with id " + session.getPlayerId());

            ProtectionWallSetupServiceInteractor interactor = new ProtectionWallSetupServiceInteractor(session.getTowerId());
            // create enchantment from spell templates
            interactor.createNewEnchantmentForProtectionWall(session.getTemplateDescriptions(), session.getProtectionWallId());
            // updating modification timestamp
            interactor.updateModificationTimestamp(Instant.now());
            // tower is no longer under protection wall installation
            interactor.unsetProtectionWallInstallation();

            logger.info("New enchantment of protection wall with id " + session.getProtectionWallId() +
                    " of tower with id " + session.getTowerId() + " installed");

            responseBuilder.setSuccess(true);

            // closing connection with client
            session.getPlayerResponseObserver().onCompleted();

            // removing session
            sessionManager.remove(session);
        }
        else {
            // error occurred
            logger.info("Cannot complete enchantment, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
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

        // TODO: remove logging after debugging
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
     * isRequestValid && sessionExists && isPlayerIdValid
     * <p>Validates requests that are related to modifying canvas or changing canvas related state.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Request type is valid</li>
     *     <li>{@link ProtectionWallSession} with provided id exists</li>
     *     <li>Player's id matches with the creator's id stored in {@link ProtectionWallSession}</li>
     * </ol>
     */
    private Optional<ServerError> validateCanvasAction(
            ProtectionWallRequest request, ProtectionWallRequest.RequestType requestType) {
        final int sessionId = request.getSessionId();
        final int playerId  = request.getPlayerData().getPlayerId();

        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);

        boolean additionalRequestCheck = switch (requestType) {
            case ADD_SPELL -> request.hasSpell();
            case CLEAR_CANVAS, COMPLETE_ENCHANTMENT -> true;
            case UNRECOGNIZED -> false;
        };

        boolean isRequestValid = (request.getRequestType() == requestType) && additionalRequestCheck;
        boolean sessionExists = isRequestValid && sessionOpt.isPresent();
        boolean isPlayerIdValid = sessionExists && playerId == sessionOpt.get().getPlayerId();

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (!isRequestValid) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Request type must be '" + requestType + "'" +
                            ", got '" + request.getRequestType() + "'" +
                            (!additionalRequestCheck ? ", additional request check failed" : ""));
        }
        else if (!sessionExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.SESSION_NOT_FOUND,
                    "ProtectionWallSession with id " + sessionId + " not found");
        }
        else if (!isPlayerIdValid) {
            errorOccurred = true;
            // player id is not the same as the player id stored inside session instance
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player id " + playerId + " does not match the required id " + sessionOpt.get().getPlayerId());
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
     *     <li>Tower is not under protection wall installation lock of other player</li>
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

        boolean towerUnderProtectionWallInstallationOfOtherPlayer =
                towerExists && !towerAlreadyOwnedByPlayer && towerOpt.get().isUnderProtectionWallsInstallation();

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
        else if (towerUnderProtectionWallInstallationOfOtherPlayer) {
            errorOccurred = true;
            int ownerId = towerOpt.get().getOwnerId().get();
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId +
                            " is under protection wall installation lock of player with id " + ownerId);
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
        // TODO: remove logging after debugging
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

        ProtectionWallSetupServiceInteractor interactor = new ProtectionWallSetupServiceInteractor(session.getTowerId());
        // unblock other players from attacking the tower
        interactor.unsetProtectionWallInstallation();

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
        ProtectionWallSetupServiceInteractor interactor = new ProtectionWallSetupServiceInteractor(session.getTowerId());
        // unblock players from attacking the tower
        interactor.unsetProtectionWallInstallation();

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
