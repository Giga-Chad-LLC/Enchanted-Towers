package enchantedtowers.client.interactors.canvas;

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

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.SpellRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SpellFinishResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


class EventWorker extends Thread {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final TowerAttackServiceGrpc.TowerAttackServiceBlockingStub blockingStub;
    private final Logger logger = Logger.getLogger(EventWorker.class.getName());
    private final CanvasState state;
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

            // creating spell color request
            var spellColorRequestBuilder = requestBuilder.getSpellColorBuilder();
            spellColorRequestBuilder.setColorId(colorId);

            System.out.println("PLAYER_ID: " + ClientStorage.getInstance().getPlayerId().get());
            // building player data
            // TODO: check that playerId exists
            spellColorRequestBuilder.getPlayerDataBuilder()
                    .setPlayerId(ClientStorage.getInstance().getPlayerId().get())
                    .build();
            // building spell color request
            spellColorRequestBuilder.build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithDrawSpellRequest(Vector2 point) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.DRAW_SPELL);

            // creating draw spell request
            var drawSpellRequestBuilder = requestBuilder.getDrawSpellBuilder();
            drawSpellRequestBuilder.getPositionBuilder()
                    .setX(point.x)
                    .setY(point.y)
                    .build();

            // building player data
            // TODO: check that playerId exists
            drawSpellRequestBuilder.getPlayerDataBuilder()
                    .setPlayerId(ClientStorage.getInstance().getPlayerId().get())
                    .build();
            // building draw spell request
            drawSpellRequestBuilder.build();

            return new Event(requestBuilder.build());
        }

        public static Event createEventWithFinishSpellRequest(Vector2 point) {
            SpellRequest.Builder requestBuilder = SpellRequest.newBuilder();
            // set request type
            requestBuilder.setRequestType(SpellRequest.RequestType.FINISH_SPELL);

            // creating draw spell request
            var finishSpellBuilder = requestBuilder.getFinishSpellBuilder();
            finishSpellBuilder.getPositionBuilder()
                    .setX(point.x)
                    .setY(point.y)
                    .build();

            // building player data
            // TODO: check that playerId exists
            finishSpellBuilder.getPlayerDataBuilder()
                    .setPlayerId(ClientStorage.getInstance().getPlayerId().get())
                    .build();
            // building draw spell request
            finishSpellBuilder.build();

            return new Event(requestBuilder.build());
        }
    };

    public EventWorker(CanvasState state, CanvasWidget canvasWidget) {
        this.state = state;
        this.canvasWidget = canvasWidget;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        logger.info("Creating blocking stub");
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = TowerAttackServiceGrpc.newBlockingStub(channel);
    }

    public void run() {
        isRunning.set(true);

        while (isRunning.get()) {
            try {
                Event event = eventQueue.poll(30, TimeUnit.MILLISECONDS);
                if (event != null) {
                    logger.info("Sending request of type: " + event.requestType().toString());
                    switch (event.requestType()) {
                        case SELECT_SPELL_COLOR -> {
                            ActionResultResponse response = blockingStub.selectSpellColor(event.getRequest());
                            logger.info(
                                    "Got response from selectSpellColor: success=" + response.getSuccess() +
                                        "\nmessage='" + response.getError().getMessage() + "'");

                        }
                        case DRAW_SPELL -> {
                            ActionResultResponse response = blockingStub.drawSpell(event.getRequest());
                            logger.info("Got response from drawSpell: success=" + response.getSuccess() +
                                    "\nmessage='" + response.getError().getMessage() + "'");
                        }
                        case FINISH_SPELL -> {
                            SpellFinishResponse response = blockingStub.finishSpell(event.getRequest());
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

                                    state.addItem(canvasMatchedEnchantment);
                                    canvasWidget.invalidate();
                                }

                                // TODO: remove this assert
                                assert(template != null);

                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Thread interrupted, exit the loop
                isRunning.set(false);
                break;
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



public class CanvasDrawSpellInteractor implements CanvasInteractor {
    private final Path path = new Path();
    private final List<Vector2> pathPoints = new ArrayList<>();
    private final Paint brush;
    private final EventWorker worker;

    private static final Logger logger = Logger.getLogger(CanvasDrawSpellInteractor.class.getName());

    public CanvasDrawSpellInteractor(CanvasState state, CanvasWidget canvasWidget) {
        brush = state.getBrush();
        logger.info("Start worker");

        worker = new EventWorker(state, canvasWidget);
        worker.start();
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(path, brush);
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
    public void onDestroy() {
        logger.info("Finishing worker...");
        worker.finish();
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
        pathPoints.add(point);
        // update color only when started the new shape
        brush.setColor(state.getBrushColor());

        if (!worker.enqueueEvent(EventWorker.Event.createEventWithSpellColorRequest(brush.getColor()))) {
            logger.warning("'Change color' event lost");
        }

        if (!worker.enqueueEvent(EventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Move to' event lost");
        }

        return true;
    }

    private boolean onActionUpFinishPathAndSubstitute(CanvasState state, float x, float y) {
        path.lineTo(x, y);
        Vector2 point = new Vector2(x, y);
        pathPoints.add(point);

        if (!worker.enqueueEvent(EventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        Vector2 pathOffset = getPathOffset(path);
        if (!worker.enqueueEvent(EventWorker.Event.createEventWithFinishSpellRequest(pathOffset))) {
            logger.warning("'Offset' event lost");
        }

        logger.info("Run hausdorff on server!");

        path.reset();
        pathPoints.clear();

        return true;
    }

    private boolean onActionMoveContinuePath(float x, float y) {
        path.lineTo(x, y);
        Vector2 point = new Vector2(x, y);
        pathPoints.add(point);

        if (!worker.enqueueEvent(EventWorker.Event.createEventWithDrawSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        return true;
    }
}