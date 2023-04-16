package enchantedtowers.client.interactors.canvas;

import static enchantedtowers.common.utils.proto.responses.GameError.ErrorType;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.logging.Logger;

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.stub.StreamObserver;

public class CanvasSpectateInteractor implements CanvasInteractor {
    private final TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    // TODO: unite the functionality of Attack and Spectate canvas interactors
    private final Path currentPath = new Path();
    private final Paint brush;
    private final Logger logger = Logger.getLogger(AttackEventWorker.class.getName());

    // TODO: watch for the race conditions in this class
    // TODO: consider scaling the points that we retrieve from server (solution: make canvas size fixed or send to the server the canvas size of attacker and recalculate real path size on spectators)

    public CanvasSpectateInteractor(CanvasState state, CanvasWidget canvasWidget) {
        // copy brush settings from CanvasState
        brush = state.getBrushCopy();

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();
        Channel channel = Grpc.newChannelBuilderForAddress(
                host,
                port,
                InsecureChannelCredentials.create()
        ).build();
        asyncStub = TowerAttackServiceGrpc.newStub(channel);

        var playerId = ClientStorage.getInstance().getPlayerId();
        var towerId = ClientStorage.getInstance().getTowerIdUnderSpectate();
        if (!(playerId.isPresent() && towerId.isPresent())) {
            // TODO: refactor later
            logger.warning("CanvasSpectateInteractor interactor constructor failure: present playerId=" + playerId.isPresent() + ", towerId=" + towerId.isPresent());
            throw new RuntimeException("CanvasSpectateInteractor interactor constructor failure: playerId or towerId is not present");
        }

        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.setTowerId(towerId.get());
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId.get())
                .build();

        asyncStub.spectateTowerById(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(SpectateTowerAttackResponse value) {
                if (value.hasError()) {
                    logger.info("CanvasSpectateInteractor::Received: " + value.getError().getMessage());
                    // TODO: deal with error somehow
                    if (value.getError().getType() == ErrorType.SPELL_TEMPLATE_NOT_FOUND) {
                        // attacker did not manage to create a spell, then we just delete his drawing
                        currentPath.reset();
                        canvasWidget.invalidate();
                    }

                    return;
                }

                switch (value.getResponseType()) {
                    case CURRENT_CANVAS_STATE -> {
                        logger.info("Received CURRENT_CANVAS_STATE");
                        onCurrentCanvasStateReceived(value, state, canvasWidget);
                    }
                    case SELECT_SPELL_COLOR -> {
                        logger.info("Received SELECT_SPELL_COLOR");
                        onSelectSpellColorReceived(value);
                    }
                    case DRAW_SPELL -> {
                        logger.info("Received DRAW_SPELL");
                        onDrawSpellReceived(value, canvasWidget);
                    }
                    case FINISH_SPELL -> {
                        logger.info("Received FINISH_SPELL");
                        onFinishSpellReceived(value, state, canvasWidget);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("onError: " + t.getMessage());
                // TODO: deal with error

            }

            @Override
            public void onCompleted() {
                logger.info("onCompleted");
                // TODO: redirect to another activity
            }
        });
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(currentPath, brush);
    }

    @Override
    public boolean onTouchEvent(CanvasState state, float x, float y, int motionEventType) {
        return false;
    }

    private void onCurrentCanvasStateReceived(SpectateTowerAttackResponse value, CanvasState state, CanvasWidget canvasWidget) {
        var canvasHistory = value.getCanvasState();

        // fill current spell state
        if (canvasHistory.hasCurrentSpellState()) {
            var currentSpellState = canvasHistory.getCurrentSpellState();
            // extract data

            var currentSpellColor = currentSpellState.getColorId();
            var currentSpellPoints = currentSpellState.getPointsList();

            Path newSpellPath = new Path();
            if (!currentSpellPoints.isEmpty()) {
                newSpellPath.moveTo((float)currentSpellPoints.get(0).getX(), (float)currentSpellPoints.get(0).getY());
            }
            for (var point : currentSpellPoints) {
                newSpellPath.lineTo((float)point.getX(), (float)point.getY());
            }

            // TODO: add mutex of smth (in order to prevent data race with onDraw method)
            // set class members values
            brush.setColor(currentSpellColor);
            currentPath.set(newSpellPath);
        }


        // add already drawn spells
        var spellDescriptions = canvasHistory.getSpellDescriptionsList();
        for (var description : spellDescriptions) {
            int templateColor = description.getColorId();
            Vector2 templateOffset = new Vector2(
                    description.getSpellTemplateOffset().getX(),
                    description.getSpellTemplateOffset().getY()
            );
            Spell templateSpell = SpellBook.getTemplateById(description.getSpellTemplateId());
            templateSpell.setOffset(templateOffset);

            // TODO: state.addItem should be thread safe actually, make sure it is
            state.addItem(new CanvasSpellDecorator(
                    templateColor,
                    templateSpell
            ));
        }

        // trigger the rendering
        canvasWidget.invalidate();
    }

    private void onSelectSpellColorReceived(SpectateTowerAttackResponse value) {
        // TODO: think about data race typa-shit...
        currentPath.reset();
        brush.setColor(value.getSpellColor().getColorId());
        logger.info("onSelectSpellColorReceived: newSpellColor=" + brush.getColor());
    }

    private void onDrawSpellReceived(SpectateTowerAttackResponse value, CanvasWidget canvasWidget) {
        var newPoint = value.getSpellPoint();
        if (currentPath.isEmpty()) {
            currentPath.moveTo((float) newPoint.getX(), (float) newPoint.getY());
        }
        else {
            currentPath.lineTo((float) newPoint.getX(), (float) newPoint.getY());
        }

        // trigger the rendering
        canvasWidget.invalidate();
    }

    private void onFinishSpellReceived(SpectateTowerAttackResponse value, CanvasState state, CanvasWidget canvasWidget) {
        // reset current drawing because attacked already finished it
        currentPath.reset();

        // here spell template was definitely found
        // case when it is not found is handled when server responded with SPELL_TEMPLATE_NOT_FOUND error (see above)
        var description = value.getSpellDescription();
        var templateOffset = description.getSpellTemplateOffset();
        int templateColor = description.getColorId();
        Spell templateSpell = SpellBook.getTemplateById(description.getSpellTemplateId());
        templateSpell.setOffset(new Vector2(
                templateOffset.getX(),
                templateOffset.getY()
        ));

        state.addItem(new CanvasSpellDecorator(
                templateColor,
                templateSpell
        ));

        canvasWidget.invalidate();
    }
}
