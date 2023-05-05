package enchantedtowers.client.interactors.canvas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import enchantedtowers.client.AttackTowerMenuActivity;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.common.utils.proto.requests.ProtectionWallIdRequest;
import enchantedtowers.common.utils.proto.requests.ProtectionWallRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.responses.SpellFinishResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
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

        public static Event createEventWithSpellRequest(List<Vector2> points, Vector2 offset, SpellType spellType) {
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

            // set spell color

            requestBuilder.getSpellBuilder()
                    .addAllPoints(protoPoints)
                    .setOffset(protoOffset)
                    .setSpellType(spellType)
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

        public static Event createEventWithCompleteEnchantmentRequest() {
            int playerId = ClientStorage.getInstance().getPlayerId().get();
            int sessionId = ClientStorage.getInstance().getSessionId().get();

            ProtectionWallRequest.Builder requestBuilder = ProtectionWallRequest.newBuilder();

            requestBuilder
                    .setRequestType(ProtectionWallRequest.RequestType.COMPLETE_ENCHANTMENT)
                    .setSessionId(sessionId);
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(playerId)
                    .build();

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
                            SpellFinishResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .addSpell(event.getRequest());

                            if (response.hasError()) {
                                logger.warning("Got error from addSpell: message='" + response.getError().getMessage() + "'");
                            }
                            else {
                                var description = response.getSpellDescription();
                                var offset = description.getSpellTemplateOffset();
                                var templateType = description.getSpellType();
                                var templateId = description.getSpellTemplateId();

                                logger.info(
                                "Got response from finishSpell: matchedSpellTemplateId='" + templateId + "'\n" +
                                        "matchedTemplateOffset='[" + offset.getX() + ", " + offset.getY() + "]'\n" +
                                        "matchedTemplateColor='" + templateType + "'"
                                );


                                // substitute current spell with template
                                Spell template = SpellBook.getTemplateById(templateId);

                                if (template != null) {
                                    template.setOffset(new Vector2(offset.getX(), offset.getY()));
                                    CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                                            templateType,
                                            template
                                    );

                                    canvasWidget.getState().addItem(canvasMatchedEnchantment);
                                    canvasWidget.postInvalidate();
                                    System.out.println("Canvas widget state size: " + canvasWidget.getState().getItems().size());
                                }
                            }
                        }
                        case CLEAR_CANVAS -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .clearCanvas(event.getRequest());

                            if (response.hasError()) {
                                logger.warning("Got error from clearCanvas: message='" + response.getError().getMessage() + "'");
                            }
                            else {
                                logger.info(
                                "Got response from clearCanvas: success=" + response.getSuccess()
                                );
                            }
                        }
                        case COMPLETE_ENCHANTMENT -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .completeEnchantment(event.getRequest());

                            if (response.hasError()) {
                                logger.warning("Got error from completeEnchantment: message='" + response.getError().getMessage() + "'");
                            }
                            else {
                                logger.info(
                                    "Got response from completeEnchantment: success=" + response.getSuccess()
                                );
                            }
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
    public boolean onSubmitCanvas(CanvasState state) {
        if (!worker.enqueueEvent(ProtectionEventWorker.Event.createEventWithCompleteEnchantmentRequest())) {
            logger.warning("'Add spell' event lost");
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return switch (motionEventType) {
            case MotionEvent.ACTION_DOWN -> onActionDownStartNewPath(state, x, y);
            case MotionEvent.ACTION_UP -> onActionUpFinishPathAndSubstitute(state, x, y);
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
        ProtectionWallIdRequest.Builder requestBuilder = ProtectionWallIdRequest.newBuilder();
        // creating request
        {
            int playerId = ClientStorage.getInstance().getPlayerId().get();
            int towerId = ClientStorage.getInstance().getTowerId().get();
            int wallId = ClientStorage.getInstance().getProtectionWallId().get();

            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(playerId)
                    .build();
            requestBuilder.setTowerId(towerId);
            requestBuilder.setProtectionWallId(wallId);
        }

        // make async call here
        asyncStub.enterProtectionWallCreationSession(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(SessionInfoResponse response) {
                if (response.hasError()) {
                    // TODO: leave protect session
                    logger.warning("enterProtectionWallCreationSession::onNext: error='" + response.getError().getMessage() + "'");
                    ClientUtils.showToastOnUIThread((Activity) canvasWidget.getContext(), response.getError().getMessage(), Toast.LENGTH_LONG);
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
                            logger.info("Protect wall session expired!");
                            // TODO: leave protect session
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

                ClientUtils.redirectToActivityAndPopHistory(
                        (Activity) canvasWidget.getContext(),
                        AttackTowerMenuActivity.class,
                        "Protection wall was set successfully!"
                );
            }
        });
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

    private boolean onActionUpFinishPathAndSubstitute(CanvasState state, float x, float y) {
        path.lineTo(x, y);
        pathPoints.add(new Vector2(x, y));

        if (!worker.enqueueEvent(ProtectionEventWorker.Event.createEventWithSpellRequest(pathPoints, ClientUtils.getPathOffset(path), state.getSelectedSpellType()))) {
            logger.warning("'Add spell' event lost");
        }

        path.reset();
        pathPoints.clear();
        return true;
    }
}
