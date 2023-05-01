package services;

import components.session.ProtectionWallSession;
import components.session.ProtectionWallSessionManager;
import components.time.Timeout;
import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.SessionIdResponse;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.game_logic.MatchedTemplateDescription;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_models.Tower;
import enchantedtowers.game_models.registry.TowersRegistry;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.logging.Logger;


public class ProtectionWallSetupService extends ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceImplBase {
    private static final long SESSION_CREATION_TIMEOUT_MS = 30 * 60 * 1000; // 30min
    private static final long SESSION_CREATION_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24h

    private final Logger logger = Logger.getLogger(ProtectionWallSetupService.class.getName());
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
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists    = towerOpt.isPresent();
        boolean towerProtected = towerExists && towerOpt.get().isProtected();

        if (towerExists && !towerProtected) {
            // tower may be captured
            int playerId = request.getPlayerData().getPlayerId();
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


    /**
     * Validates the request and checks the conditions required to enter protection wall creation session.
     * If validation fails then sets the appropriate error into response; otherwise does nothing.
     * Caller should check for an error instance being set in a response model.
     */
    private void validateEnteringProtectionWallCreationSession(TowerIdRequest request, ServerError.Builder errorBuilder) {
        int towerId = request.getTowerId();
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists = towerOpt.isPresent();

        boolean isPlayerOwner = towerExists && towerOpt.get().getOwnerId().isPresent() &&
                                towerOpt.get().getOwnerId().get() == request.getPlayerData().getPlayerId();

        boolean hasLastProtectionWallModificationTimestamp =
                towerExists && towerOpt.get().getLastProtectionWallModificationTimestamp().isPresent();

        boolean cooldownTimeExceeded = hasLastProtectionWallModificationTimestamp &&
                cooldownTimeBetweenSessionsCreationExceeded(towerOpt.get().getLastProtectionWallModificationTimestamp().get());

        if (!towerExists) {
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else if (!isPlayerOwner) {
            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Player with id " + request.getPlayerData().getPlayerId() +
                            " is not an owner of tower with id " + towerId);
        }
        else if (hasLastProtectionWallModificationTimestamp && !cooldownTimeExceeded) {
            // cooldown not exceeded
            var timestamp = towerOpt.get().getLastProtectionWallModificationTimestamp().get();
            long remain = SESSION_CREATION_COOLDOWN_MS - Duration.between(timestamp, Instant.now()).toSeconds();

            ProtoModelsUtils.buildServerError(errorBuilder,
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Cooldown after last protection wall installation not exceeded. Wait for: " + remain + "s");
        }
        /*
        // either cooldown exceeded or modification timestamp was reset after capturing
        towerExists && (!hasLastProtectionWallModificationTimestamp || cooldownTimeExceeded)
         */
    }

    @Override
    public void tryEnterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<ActionResultResponse> streamObserver) {
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();
        validateEnteringProtectionWallCreationSession(request, responseBuilder.getErrorBuilder());

        // either cooldown exceeded or modification timestamp was reset after capturing
        if (!responseBuilder.hasError()) {
            responseBuilder.setSuccess(true);
        }

        /*
        int towerId = request.getTowerId();
        Optional<Tower> towerOpt = TowersRegistry.getInstance().getTowerById(towerId);

        boolean towerExists = towerOpt.isPresent();
        boolean hasLastProtectionWallModificationTimestamp =
                towerExists && towerOpt.get().getLastProtectionWallModificationTimestamp().isPresent();
        boolean cooldownTimeExceeded = hasLastProtectionWallModificationTimestamp &&
                cooldownTimeBetweenSessionsCreationExceeded(towerOpt.get().getLastProtectionWallModificationTimestamp().get());

        if (towerExists && (!hasLastProtectionWallModificationTimestamp || cooldownTimeExceeded)) {
            responseBuilder.setSuccess(true);
        }
        else if (!towerExists) {
            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.TOWER_NOT_FOUND,
                    "Tower with id " + towerId + " not found");
        }
        else {
            // cooldown not exceeded
            var timestamp = towerOpt.get().getLastProtectionWallModificationTimestamp().get();
            long remain = SESSION_CREATION_COOLDOWN_MS - Duration.between(timestamp, Instant.now()).toSeconds();

            ProtoModelsUtils.buildServerError(responseBuilder.getErrorBuilder(),
                    ServerError.ErrorType.INVALID_REQUEST,
                    "Cooldown after last protection wall installation not exceeded. Wait for: " + remain + "s");
        }
         */

        streamObserver.onNext(responseBuilder.build());
        streamObserver.onCompleted();
    }

    @Override
    public void enterProtectionWallCreationSession(TowerIdRequest request, StreamObserver<SessionInfoResponse> streamObserver) {
        SessionInfoResponse.Builder responseBuilder = SessionInfoResponse.newBuilder();
        validateEnteringProtectionWallCreationSession(request, responseBuilder.getErrorBuilder());

        if (!responseBuilder.hasError()) {
            // create protection wall creation session
            int playerId = request.getPlayerData().getPlayerId();
            int towerId = request.getTowerId();

            // TODO: pass appropriate callback
            IntConsumer dummyCallback = (int value) -> {};
            ProtectionWallSession session = sessionManager.createSession(playerId, towerId, streamObserver, dummyCallback);

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
            // send error to client
            streamObserver.onNext(responseBuilder.build());
            streamObserver.onCompleted();
        }
    }

    @Override
    public void addSpell(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    @Override
    public void clearCanvas(ProtectionWallRequest request, StreamObserver<ActionResultResponse> streamObserver) {}

    // helper methods
    private boolean cooldownTimeBetweenSessionsCreationExceeded(Instant timestamp) {
        return Duration.between(timestamp, Instant.now()).toMillis() >= SESSION_CREATION_COOLDOWN_MS;
    }
}
