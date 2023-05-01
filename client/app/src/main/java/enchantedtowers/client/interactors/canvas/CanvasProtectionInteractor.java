package enchantedtowers.client.interactors.canvas;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import enchantedtowers.client.AttackTowerMenuActivity;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

class ProtectionEventWorker extends Thread {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceBlockingStub blockingStub;
    private final Logger logger = Logger.getLogger(AttackEventWorker.class.getName());
    private final CanvasWidget canvasWidget;

    // Event class
    public static class Event {
        ProtectionWallRequest request;

        Event(ProtectionWallRequest request) {
            this.request = request;
        }

        public ProtectionWallRequest.RequestType requestType() {
            return request.getRequestType();
        }

        public ProtectionWallRequest getRequest() {
            return request;
        }

        public static Event createEventWithSpellRequest(Vector2 offset, List<Vector2> points) {
            ProtectionWallRequest.Builder requestBuilder = ProtectionWallRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(ProtectionWallRequest.RequestType.ADD_SPELL);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get()).build();

            // creating draw spell request
            var vector2Builder = enchantedtowers.common.utils.proto.common.Vector2.newBuilder();
            var protoOffset = vector2Builder.setX(offset.x).setY(offset.y).build();
            List <enchantedtowers.common.utils.proto.common.Vector2> protoPoints = new ArrayList<>();
            for (var point : points) {
                vector2Builder.setX(point.x);
                vector2Builder.setY(point.y);
                protoPoints.add(vector2Builder.build());
            }

            requestBuilder.getSpellBuilder()
                    .addAllPoints(protoPoints)
                    .setOffset(protoOffset)
                    .build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithClearCanvasRequest() {
            ProtectionWallRequest.Builder requestBuilder = ProtectionWallRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(ProtectionWallRequest.RequestType.CLEAR_CANVAS);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get()).build();

            return new Event(requestBuilder.build());
        }
    }

    public ProtectionEventWorker(CanvasWidget canvasWidget) {
        this.canvasWidget = canvasWidget;
        logger.info("Creating blocking stub");

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        logger.info("Creating blocking stub");
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = ProtectionWallSetupServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {
        isRunning.set(true);

        while (isRunning.get()) {
            try {
                Event event = eventQueue.poll(30, TimeUnit.MILLISECONDS);
                if (event != null) {
                    switch (event.requestType()) {
                        case ADD_SPELL -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .addSpell(event.getRequest());

                            logger.info(
                            "Got response from addSpell: success=" + response.getSuccess() +
                                "\nmessage='" + response.getError().getMessage() + "'"
                            );
                        }
                        case CLEAR_CANVAS -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .clearCanvas(event.getRequest());

                            logger.info(
                            "Got response from clearCanvas: success=" + response.getSuccess() +
                                "\nmessage='" + response.getError().getMessage() + "'"
                            );
                        }
                    }
                }
            } catch (Exception e) {
                // Thread interrupted, exit the loop
                isRunning.set(false);

                logger.warning("CanvasProtectionInteractor error while blocking stub '" + e.getMessage() + "'");

                // redirect to base activity
                Intent intent = new Intent(canvasWidget.getContext(), AttackTowerMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                intent.putExtra("showToastOnStart", true);
                intent.putExtra("toastMessage", e.getMessage());

                logger.info("redirect to base activity: from=" + canvasWidget.getContext() + ", to=" + AttackTowerMenuActivity.class + ", intent=" + intent);

                canvasWidget.getContext().startActivity(intent);
            }
        }

        logger.info("Stopped worker!");
    }

    public boolean enqueueEvent(Event event) {
        return eventQueue.offer(event);
    }

    public void finish() {
        isRunning.set(false);
    }
}



public class CanvasProtectionInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final List<Vector2> pathPoints = new ArrayList<>();
    private final Paint brush;
    private ProtectionEventWorker worker;

    private static final Logger logger = Logger.getLogger(CanvasAttackInteractor.class.getName());
    private final ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceStub asyncStub;
    private final ManagedChannel channel;

    public CanvasProtectionInteractor(CanvasState state, CanvasWidget canvasWidget) {
        brush = state.getBrushCopy();

        // configuring async client stub
        {
            String host = ServerApiStorage.getInstance().getClientHost();
            int port = ServerApiStorage.getInstance().getPort();
            channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
            asyncStub = ProtectionWallSetupServiceGrpc.newStub(channel);
        }

        callAsyncEnterProtectionWall(canvasWidget);
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(path, brush);
    }

    @Override
    public boolean onClearCanvas(CanvasState state) {
        logger.info("onClearCanvas");

        if (!worker.enqueueEvent(ProtectionEventWorker.Event.createEventWithClearCanvasRequest())) {
            logger.warning("'Clear canvas' event lost");
        }

        state.clear();
        path.reset();
        pathPoints.clear();

        return true;
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return switch (motionEventType) {
            case MotionEvent.ACTION_DOWN -> onActionDownStartNewPath(state, x, y);
            case MotionEvent.ACTION_UP -> onActionUpFinishPathAndSubstitute(x, y);
            case MotionEvent.ACTION_MOVE -> onActionMoveContinuePath(x, y);
            default -> false;
        };
    }

    @Override
    public void onExecutionInterrupt() {
        // TODO: ? wrap worker with Optional<T>
        if (worker != null) {
            logger.info("Finishing worker...");
            worker.finish();
        }

        logger.info("Shutting down grpc channel...");
        channel.shutdownNow();
        try {
            channel.awaitTermination(300, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void callAsyncEnterProtectionWall(CanvasWidget canvasWidget) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        // creating request
        {
            int playerId = ClientStorage.getInstance().getPlayerId().get();
            int towerId = ClientStorage.getInstance().getTowerId().get();
            requestBuilder.setTowerId(towerId);
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(playerId)
                    .build();
        }

        // make async call here
        asyncStub.enterProtectionWallCreationSession(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(SessionInfoResponse response) {
                if (response.hasError()) {
                    // TODO: leave attack session
                    logger.warning("enterProtectionWallCreationSession::onNext: error='" + response.getError().getMessage() + "'");
                }
                else {
                    logger.info("enterProtectionWallCreationSession::onNext: type=" + response.getType());

                    switch (response.getType()) {
                        case SESSION_ID -> {
                            int sessionId = response.getSession().getSessionId();
                            ClientStorage.getInstance().setSessionId(sessionId);

                            logger.info("Start worker");
                            worker = new ProtectionEventWorker(canvasWidget);
                            worker.start();
                        }
                        case SESSION_EXPIRED -> {
                            // TODO: leave attack session
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO: leave attack session
                logger.warning("onError: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                // TODO: leave attack session
                logger.warning("onCompleted: finished");
            }
        });
    }

    private Vector2 getPathOffset(Path path) {
        // calculate bounding box for the path
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        return new Vector2(bounds.left, bounds.top);
    }

    private boolean onActionDownStartNewPath(CanvasState state, float x, float y) {
        path.moveTo(x, y);
        pathPoints.add(new Vector2(x, y));
        // update color only when started the new shape
        brush.setColor(state.getBrushColor());
        return true;
    }

    private boolean onActionMoveContinuePath(float x, float y) {
        path.lineTo(x, y);
        pathPoints.add(new Vector2(x, y));
        return true;
    }

    private boolean onActionUpFinishPathAndSubstitute(float x, float y) {
        path.lineTo(x, y);
        pathPoints.add(new Vector2(x, y));

        // TODO: send brushColor, pathOffset, and pathPoints to server here
        if (!worker.enqueueEvent(ProtectionEventWorker.Event.createEventWithSpellRequest(getPathOffset(path), pathPoints))) {
            logger.warning("'Add spell' event lost");
        }

        path.reset();
        return true;
    }
}
