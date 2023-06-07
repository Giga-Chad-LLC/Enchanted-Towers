package enchantedtowers.client.interactors.canvas;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import enchantedtowers.client.AttackTowerMenuActivity;
import enchantedtowers.client.MapActivity;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.SpellType;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.MatchedSpellStatsResponse;
import enchantedtowers.common.utils.proto.responses.SessionInfoResponse;
import enchantedtowers.common.utils.proto.responses.SpellFinishResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

class AttackEventWorker extends Thread {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final TowerAttackServiceGrpc.TowerAttackServiceBlockingStub blockingStub;
    private final Logger logger = Logger.getLogger(AttackEventWorker.class.getName());
    private final CanvasWidget canvasWidget;

    // Event class
    public static class Event {
        SpellRequest request;

        Event(SpellRequest request) {
            this.request = request;
        }

        public SpellRequest.RequestType requestType() {
            return request.getRequestType();
        }

        public SpellRequest getRequest() {
            return request;
        }

        public static Event createEventWithSpellTypeRequest(SpellType spellType) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.SELECT_SPELL_TYPE);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get())
                    .build();

            // creating spell type request
            var spellTypeRequestBuilder = requestBuilder.getSpellTypeBuilder();
            spellTypeRequestBuilder.setSpellType(spellType);

            // building spell type request
            spellTypeRequestBuilder.build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithDrawSpellRequest(Vector2 point) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.DRAW_SPELL);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get()).build();

            // creating draw spell request
            var drawSpellRequestBuilder = requestBuilder.getDrawSpellBuilder();
            drawSpellRequestBuilder.getPositionBuilder()
                    .setX(point.x)
                    .setY(point.y)
                    .build();

            // building draw spell request
            drawSpellRequestBuilder.build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithFinishSpellRequest(Vector2 point) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.FINISH_SPELL);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get()).build();

            // creating draw spell request
            var finishSpellBuilder = requestBuilder.getFinishSpellBuilder();
            finishSpellBuilder.getPositionBuilder()
                    .setX(point.x)
                    .setY(point.y)
                    .build();

            // building draw spell request
            finishSpellBuilder.build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithClearCanvasRequest() {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.CLEAR_CANVAS);

            // TODO: check that playerId and sessionId exists
            // set session id
            requestBuilder.setSessionId(ClientStorage.getInstance().getSessionId().get());
            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(ClientStorage.getInstance().getPlayerId().get()).build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithCompareDrawnSpellsRequest() {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.COMPARE_DRAWN_SPELLS);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get())
                    .build();

            return new Event(requestBuilder.build());
        }
    }

    public AttackEventWorker(CanvasWidget canvasWidget) {
        this.canvasWidget = canvasWidget;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        logger.info("Creating blocking stub");
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = TowerAttackServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void run() {
        isRunning.set(true);

        while (isRunning.get()) {
            try {
                Event event = eventQueue.poll(30, TimeUnit.MILLISECONDS);
                if (event != null) {
                    logger.info("Sending request of type: " + event.requestType().toString());
                    switch (event.requestType()) {
                        case SELECT_SPELL_TYPE -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .selectSpellType(event.getRequest());

                            logger.info("Got response from selectSpellType: success=" + response.getSuccess() +
                                        "\nmessage='" + response.getError().getMessage() + "'");

                        }
                        case DRAW_SPELL -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .drawSpell(event.getRequest());

                            logger.info("Got response from drawSpell: success=" + response.getSuccess() +
                                    "\nmessage='" + response.getError().getMessage() + "'");
                        }
                        case FINISH_SPELL -> {
                            SpellFinishResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .finishSpell(event.getRequest());

                            logger.info("Got FINISH_SPELL response");

                            if (response.hasError()) {
                                logger.warning("Got error from finishSpell: message='" + response.getError().getMessage() + "'\n");
                            }
                            else {
                                var description = response.getSpellDescription();
                                var offset = description.getSpellTemplateOffset();
                                var templateType = description.getSpellType();
                                var templateId = description.getSpellTemplateId();

                                logger.info(
                                        "Got response from finishSpell: matchedSpellTemplateId='" + templateId + "'\n" +
                                                "matchedTemplateOffset='[" + offset.getX() + ", " + offset.getY() + "]'\n" +
                                                "matchedTemplateType='" + templateType + "'");


                                // substitute current spell with template
                                Spell template = SpellBook.getSpellTemplateById(templateId);

                                if (template != null) {
                                    template.setOffset(new Vector2(offset.getX(), offset.getY()));
                                    CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                                            templateType,
                                            template
                                    );

                                    canvasWidget.getState().addItem(canvasMatchedEnchantment);
                                    canvasWidget.postInvalidate();
                                }
                            }
                        }
                        case CLEAR_CANVAS -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .clearCanvas(event.getRequest());
                            logger.info("Got response from clearCanvas: success=" + response.getSuccess() +
                                    "\nmessage='" + response.getError().getMessage() + "'");
                        }
                        case COMPARE_DRAWN_SPELLS -> {
                            MatchedSpellStatsResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .compareDrawnSpells(event.getRequest());

                            logger.info("Got response from compareDrawnSpells: error=" + response.hasError() + ", message='" + response.getError().getMessage() + "', protection wall destroyed=" + response.getProtectionWallDestroyed());

                            // TODO: show matches stats

                            // protection wall destroyed -> redirect to MapActivity
                            if (response.getProtectionWallDestroyed()) {
                                logger.info("Protection wall successfully destroyed!");
                                ClientUtils.redirectToActivityAndPopHistory((Activity) canvasWidget.getContext(), MapActivity.class, "Protection wall successfully destroyed!");
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                // Thread interrupted, exit the loop
                isRunning.set(false);

                logger.warning("CanvasAttackInteractor error while blocking stub: '" + e.getMessage() + "'");
                logger.info("redirect to base activity: from=" + canvasWidget.getContext() + ", to=" + MapActivity.class);

                // redirect to base activity
                ClientUtils.redirectToActivityAndPopHistory(
                        (Activity) canvasWidget.getContext(), MapActivity.class, e.getMessage());

                /*Intent intent = new Intent(canvasWidget.getContext(), MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                intent.putExtra("showToastOnStart", true);
                intent.putExtra("toastMessage", e.getMessage());

                canvasWidget.getContext().startActivity(intent);*/
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



public class CanvasAttackInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final Paint brush;
    private AttackEventWorker worker;

    private static final Logger logger = Logger.getLogger(CanvasAttackInteractor.class.getName());
    private final TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    private final ManagedChannel channel;

    public CanvasAttackInteractor(CanvasState state, CanvasWidget canvasWidget) {
        brush = state.getBrushCopy();

        // configuring async client stub
        {
            String host = ServerApiStorage.getInstance().getClientHost();
            int port = ServerApiStorage.getInstance().getPort();
            channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
            asyncStub = TowerAttackServiceGrpc.newStub(channel);
        }

        callAsyncAttackTowerById(canvasWidget);
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(path, brush);
    }

    @Override
    public boolean onClearCanvas(CanvasState state) {
        if (worker == null) {
            return false;
        }

        state.clear();
        logger.info("onClearCanvas");
        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithClearCanvasRequest())) {
            logger.warning("'Clear canvas' event lost");
        }
        // notifying that event was processes
        return true;
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        if (worker == null) {
            return false;
        }

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
            channel.awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onSubmitCanvas(CanvasState state) {
        if (worker == null) {
            return false;
        }

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithCompareDrawnSpellsRequest())) {
            logger.warning("'Compare drawn spells' event lost");
        }
        state.clear();

        return true;
    }

    private void callAsyncAttackTowerById(CanvasWidget canvasWidget) {
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

        asyncStub.attackTowerById(requestBuilder.build(), new StreamObserver<>() {
            private String errorMessage = null;
            private boolean errorReceived = false;

            @Override
            public void onNext(SessionInfoResponse response) {

                if (response.hasError()) {
                    // TODO: leave attack session
                    errorReceived = true;
                    errorMessage = response.getError().getMessage();
                    logger.warning("attackTowerById::onNext: error='" + response.getError().getMessage() + "'");
                }
                else {
                    logger.info("attackTowerById::onNext: type=" + response.getType());

                    switch (response.getType()) {
                        case SESSION_ID -> {
                            int sessionId = response.getSession().getSessionId();
                            ClientStorage.getInstance().setSessionId(sessionId);

                            logger.info("Start worker");
                            worker = new AttackEventWorker(canvasWidget);
                            worker.start();
                        }
                        case SESSION_EXPIRED -> {
                            logger.info("Attack session expired!");
                            // TODO: leave attack session
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO: leave attack session
                logger.warning("attackTowerById::onError: message='" + t.getMessage() + "'");
            }

            @Override
            public void onCompleted() {
                logger.warning("attackTowerById::onCompleted: finished");
                ClientUtils.redirectToActivityAndPopHistory(
                        (Activity) canvasWidget.getContext(),
                        MapActivity.class,
                        (errorReceived ? errorMessage : "Attack session ended")
                );
            }
        });
    }

    private boolean onActionDownStartNewPath(CanvasState state, float x, float y) {
        path.moveTo(x, y);
        Vector2 point = new Vector2(x, y);
        // update color only when started the new shape
        brush.setColor(state.getBrushColor());

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithSpellTypeRequest(state.getSelectedSpellType()))) {
            logger.warning("'Change spell type' event lost");
        }

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Move to' event lost");
        }

        return true;
    }

    private boolean onActionMoveContinuePath(float x, float y) {
        path.lineTo(x, y);
        Vector2 point = new Vector2(x, y);

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        return true;
    }

    private boolean onActionUpFinishPathAndSubstitute(float x, float y) {
        path.lineTo(x, y);
        Vector2 point = new Vector2(x, y);

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        Vector2 pathOffset = ClientUtils.getPathOffset(path);
        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithFinishSpellRequest(pathOffset))) {
            logger.warning("'Offset' event lost");
        }

        logger.info("Run hausdorff on server!");

        path.reset();

        return true;
    }
}
