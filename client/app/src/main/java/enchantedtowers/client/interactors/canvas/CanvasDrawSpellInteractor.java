package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

// client.components
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
// utils
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.storage.ServerApiStorage;
// game-models
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_logic.SpellsPatternMatchingAlgorithm;
import enchantedtowers.game_logic.HausdorffMetric;
import enchantedtowers.game_models.utils.Vector2;
// requests
import enchantedtowers.common.utils.proto.requests.SpellRequest;
// services
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


class EventWorker extends Thread {
    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final TowerAttackServiceGrpc.TowerAttackServiceBlockingStub blockingStub;
    private final Logger logger = Logger.getLogger(EventWorker.class.getName());


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

    public EventWorker() {
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
                            ActionResultResponse response = blockingStub.finishSpell(event.getRequest());
                            logger.info("Got response from finishSpell: success=" + response.getSuccess() +
                                    "\nmessage='" + response.getError().getMessage() + "'");
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
    private final EventWorker worker = new EventWorker();

    private static final Logger logger = Logger.getLogger(CanvasDrawSpellInteractor.class.getName());


    private boolean isValidPath() {
        // The condition is required for the correct metric distance calculation
        if (pathPoints.size() < 2 || (pathPoints.size() == 2 && pathPoints.get(0).equals(pathPoints.get(1)))) {
            return false;
        }
        return true;
    }

    public CanvasDrawSpellInteractor(CanvasState state) {
        brush = state.getBrush();
        logger.info("Start worker");
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

    // returns new list of points that are relative to their bounding-box
    private List<Vector2> getNormalizedPoints(List<Vector2> points) {
        Vector2 offset = getPathOffset(path);
        List<Vector2> translatedPoints = new ArrayList<>(points);

        // translate each point
        for (Vector2 p : translatedPoints) {
            p.move(-offset.x, -offset.y);
        }

        return translatedPoints;
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

        if (!worker.enqueueEvent(EventWorker.Event.createEventWithFinishSpellRequest(point))) {
            logger.warning("'Line to' event lost");
        }

        logger.info("Run hausdorff on server!");

        if (isValidPath()) {
            Spell pattern = new Spell(
                    getNormalizedPoints(pathPoints),
                    getPathOffset(path)
            );

            Optional<Spell> matchedSpell = SpellsPatternMatchingAlgorithm.getMatchedTemplate(
                    SpellBook.getTemplates(),
                    pattern,
                    new HausdorffMetric()
            );

            if (matchedSpell.isPresent()) {
                CanvasSpellDecorator canvasMatchedEnchantment = new CanvasSpellDecorator(
                        brush.getColor(),
                        matchedSpell.get()
                );

                state.addItem(canvasMatchedEnchantment);
            }
        }

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