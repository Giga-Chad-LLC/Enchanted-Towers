package enchantedtowers.client.interactors.canvas;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

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
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.AttackTowerByIdResponse;
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

        public static Event createEventWithSpellColorRequest(int colorId) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.SELECT_SPELL_COLOR);

            var storage = ClientStorage.getInstance();
            assert(storage.getPlayerId().isPresent() && storage.getSessionId().isPresent());

            // set session id
            requestBuilder.setSessionId(storage.getSessionId().get());

            // set player id
            requestBuilder.getPlayerDataBuilder()
                    .setPlayerId(storage.getPlayerId().get())
                    .build();

            System.out.println("PLAYER_ID: " + storage.getPlayerId().get() + ", SESSION_ID: " + storage.getSessionId().get());

            // creating spell color request
            var spellColorRequestBuilder = requestBuilder.getSpellColorBuilder();
            spellColorRequestBuilder.setColorId(colorId);

            // building spell color request
            spellColorRequestBuilder.build();

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

            System.out.println("PLAYER_ID: " + storage.getPlayerId().get() + ", SESSION_ID: " + storage.getSessionId().get());

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
                        case SELECT_SPELL_COLOR -> {
                            ActionResultResponse response = blockingStub
                                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                                    .selectSpellColor(event.getRequest());

                            logger.info(
                                    "Got response from selectSpellColor: success=" + response.getSuccess() +
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
                                var colorId = description.getColorId();
                                var templateId = description.getSpellTemplateId();

                                logger.info(
                                        "Got response from finishSpell: matchedSpellTemplateId='" + templateId + "'\n" +
                                                "matchedTemplateOffset='[" + offset.getX() + ", " + offset.getY() + "]'\n" +
                                                "matchedTemplateColor='" + colorId + "'");


                                // substitute current spell with template
                                Spell template = SpellBook.getTemplateById(templateId);

                                if (template != null) {
                                    template.setOffset(new Vector2(offset.getX(), offset.getY()));
                                    CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                                            colorId,
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
                    }
                }
            } catch (Exception e) {
                // Thread interrupted, exit the loop
                isRunning.set(false);

                logger.warning("CanvasDrawSpellInteractor error while blocking stub '" + e.getMessage() + "'");

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
        state.clear();
        System.out.println("CanvasDrawSpellInteractor.onClearCanvas");
        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithClearCanvasRequest())) {
            logger.warning("'Clear canvas' event lost");
        }
        // notifying that event was processes
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
            @Override
            public void onNext(AttackTowerByIdResponse response) {
                if (response.hasError()) {
                    // TODO: leave attack session
                    System.out.println("attackTowerById::onNext: error='" + response.getError().getMessage() + "'");
                }
                else {
                    System.out.println("attackTowerById::onNext: type=" + response.getType());

                    switch (response.getType()) {
                        case ATTACK_SESSION_ID -> {
                            int sessionId = response.getSession().getSessionId();
                            ClientStorage.getInstance().setSessionId(sessionId);

                            logger.info("Start worker");
                            worker = new AttackEventWorker(canvasWidget);
                            worker.start();
                        }
                        case ATTACK_SESSION_EXPIRED -> {
                            // TODO: leave attack session
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO: leave attack session
                System.out.println("attackTowerById::onError: message='" + t.getMessage() + "'");
            }

            @Override
            public void onCompleted() {
                // TODO: leave attack session
                System.out.println("attackTowerById::onCompleted: finished");
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
        Vector2 point = new Vector2(x, y);
        // update color only when started the new shape
        brush.setColor(state.getBrushColor());

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithSpellColorRequest(brush.getColor()))) {
            logger.warning("'Change color' event lost");
        }

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Move to' event lost");
        }

        return true;
    }

    private boolean onActionUpFinishPathAndSubstitute(float x, float y) {
        path.lineTo(x, y);
        Vector2 point = new Vector2(x, y);

        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        Vector2 pathOffset = getPathOffset(path);
        if (!worker.enqueueEvent(AttackEventWorker.Event.createEventWithFinishSpellRequest(pathOffset))) {
            logger.warning("'Offset' event lost");
        }

        logger.info("Run hausdorff on server!");

        path.reset();

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
}
