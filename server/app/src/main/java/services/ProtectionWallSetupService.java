package services;

import components.session.AttackSession;
import components.session.ProtectionWallSession;
import components.session.ProtectionWallSessionManager;
import components.time.Timeout;
import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.requests.EnterProtectionWallCreationRequest;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.responses.SpellFinishResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.game_logic.TemplateDescription;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
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


public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {
    private static final long SESSION_CREATION_TIMEOUT_MS = 30 * 60 * 1000; // 30min
    private static final long SESSION_CREATION_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24h

    private final Logger logger = Logger.getLogger(ProtectionWallSetupService.class.getName());
    private final IntConsumer onSessionExpiredCallback = this::onSessionExpired;
    private final ProtectionWallSessionManager sessionManager = new ProtectionWallSessionManager();
    private final Map<Integer, Timeout> timeouts = new HashMap<>();


    @Override
    public void captureTower(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        /**
         * Запустить таймер, связанный с playerId, который будет определять, что сессию формирования стены можно создать.
         * Пока данный таймер не закончился, сессию можно создавать.
         * Если таймер закончился, то нужно сделать состояние башни "доступна для захвата для других игроков" +
         * нельзя больше давать игроку возможность устанавливать заклинания на стену (сутки);
         * сделать это через timestamp в базе данных.

         tower = towerRegistry.getTowerById(id);
         tower.ownerId = playerId;
         tower.setModificationTimestamp(-1);
         timer.setup(30min, () -> {
            tower.updateModificationTimestamp(now);
         });
         */
        // TODO: add lock on the whole captureTower method

        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        int towerId = request.getTowerId();
        int playerId = request.getPlayerData().getPlayerId();

        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists    = towerOpt.isPresent();
        boolean towerProtected = towerExists && towerOpt.get().isProtected();
        boolean towerAlreadyOwnedByPlayer = towerExists &&
                                            towerOpt.get().getOwnerId().isPresent() &&
                                            towerOpt.get().getOwnerId().get() == playerId;

        if (towerExists && !towerProtected && !towerAlreadyOwnedByPlayer) {
            // tower may be captured
            Tower tower = towerOpt.get();

            // make player an owner of tower and allow to set up protection walls
            tower.setOwnerId(playerId);
            tower.resetLastProtectionWallModificationTimestamp();
            // block other players from attacking the tower
            tower.setUnderProtectionWallsInstallation(true);

            // setting timeout of protection walls installation
            timeouts.put(towerId, new Timeout(SESSION_CREATION_TIMEOUT_MS, () -> {
                logger.info("timeout: towerId=" + tower.getId() + " playerId=" + playerId);
                tower.setLastProtectionWallModificationTimestamp(Instant.now());
                tower.setUnderProtectionWallsInstallation(false);
                // removing timeout from map
                timeouts.remove(towerId);
            }));

            responseBuilder.setSuccess(true);
        }
        else if (!towerExists) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (towerAlreadyOwnedByPlayer) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " is already owned by player with id " + playerId);
        }
        else {
            // tower is protected
            assert(towerOpt.get().getOwnerId().isPresent());
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Tower with id " + towerId + " protected by player with id " + towerOpt.get().getOwnerId().get());
        }

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }


    @Override
    public void tryEnterProtectionWallCreationSession(EnterProtectionWallCreationRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        Optional<ServerError> serverError = validateEnteringProtectionWallCreationSession(request);

        if (serverError.isEmpty()) {
            responseBuilder.setSuccess(true);
        }
        else {
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

            ProtectionWallSession session = sessionManager.createSession(
                    playerId, towerId, protectionWallId, streamObserver, onSessionExpiredCallback);

            int sessionId = session.getId();

            // create cancel handler to hook the event of client closing the connection
            {
                var callObserver = (ServerCallStreamObserver<SessionInfoResponse>) streamObserver;
                // `setOnCancelHandler` must be called before any `onNext` calls
                callObserver.setOnCancelHandler(() -> {
                    logger.info("Player with id " + playerId +
                            " cancelled stream. Destroying corresponding protection wall session with id " + sessionId + "...");
                    sessionManager.remove(session);
                });
            }

            // send session id to client
            responseBuilder.setType(SessionInfoResponse.ResponseType.SESSION_ID);
            responseBuilder.getSessionBuilder().setSessionId(sessionId);

            streamObserver.onNext(responseBuilder.build());
        }
        else {
            // error
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
                    request.getSpell().getColorId()
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
                            .setColorId(template.colorId());

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


    // helper methods
    /**
     * <p>Validates the request and checks the conditions required to enter protection wall creation session.
     * If validation fails then sets the appropriate error into response; otherwise does nothing.</p>
     * <p>Caller should check for an error instance being set in a response model.</p>
     * <p><b>Required conditions:</b></p>
     * <ol>
     *     <li>Tower with provided id exists</li>
     *     <li>Protection wall with provided id exists inside tower</li>
     *     <li>Player is an owner of a tower</li>
     *     <li>Either {@link Tower#lastProtectionWallModificationTimestamp} is empty (which means that tower has been captured by a player, and he has time to set up 1st protection wall) or cooldown between protection wall updates exceeded</li>
     * </ol>
     */
    private Optional<ServerError> validateEnteringProtectionWallCreationSession(EnterProtectionWallCreationRequest request) {
        int towerId = request.getTowerId();
        int protectionWallId = request.getProtectionWallId();
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists = towerOpt.isPresent();

        boolean ProtectionWallExists = towerExists && towerOpt.get().hasProtectionWallWithId(protectionWallId);

        boolean isPlayerOwner = towerExists && towerOpt.get().getOwnerId().isPresent() &&
                towerOpt.get().getOwnerId().get() == request.getPlayerData().getPlayerId();

        boolean hasLastProtectionWallModificationTimestamp =
                towerExists && towerOpt.get().getLastProtectionWallModificationTimestamp().isPresent();

        boolean cooldownTimeExceeded = hasLastProtectionWallModificationTimestamp &&
                cooldownTimeBetweenSessionsCreationExceeded(towerOpt.get().getLastProtectionWallModificationTimestamp().get());

        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        if (!towerExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (!ProtectionWallExists) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Protection wall with id " + protectionWallId + " of tower with id " + towerId + " not found");
        }
        else if (!isPlayerOwner) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + request.getPlayerData().getPlayerId() +
                            " is not an owner of tower with id " + towerId);
        }
        else if (hasLastProtectionWallModificationTimestamp && !cooldownTimeExceeded) {
            // cooldown not exceeded
            errorOccurred = true;
            var timestamp = towerOpt.get().getLastProtectionWallModificationTimestamp().get();
            long remain = SESSION_CREATION_COOLDOWN_MS - Duration.between(timestamp, Instant.now()).toSeconds();

            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Cooldown after last protection wall installation not exceeded. Wait for: " + remain + "s");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }

        /*
        // either cooldown exceeded or modification timestamp was reset after capturing
        towerExists && (!hasLastProtectionWallModificationTimestamp || cooldownTimeExceeded)
         */
    }


    private boolean cooldownTimeBetweenSessionsCreationExceeded(Instant timestamp) {
        return Duration.between(timestamp, Instant.now()).toMillis() >= SESSION_CREATION_COOLDOWN_MS;
    }

    private void onSessionExpired(int sessionId) {
        Optional<ProtectionWallSession> sessionOpt = sessionManager.getSessionById(sessionId);
        // TODO: better to ignore callback rather than throw
        if (sessionOpt.isEmpty()) {
            throw new NoSuchElementException("SessionExpiredCallback: session with id " + sessionId + " not found");
        }
        ProtectionWallSession session = sessionOpt.get();

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
