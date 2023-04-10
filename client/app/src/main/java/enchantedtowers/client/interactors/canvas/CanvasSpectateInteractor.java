package enchantedtowers.client.interactors.canvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
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

    // TODO: watch for the race conditions in this class
    // TODO: consider scaling the points that we retrieve from server (solution: make canvas size fixed or send to the server the canvas size of attacker and recalculate real path size on spectators)

    // TODO: change System.out.println's to a logger (which print the calling calss and method names automatically);
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
            throw new RuntimeException("CanvasSpectateInteractor() interactor constructor failure. PlayerId or TowerId is not present");
        }

        TowerAttackRequest.Builder requestBuilder = TowerAttackRequest.newBuilder();
        requestBuilder.setTowerId(towerId.get());
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId.get())
                .build();

        asyncStub.spectateTowerById(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(SpectateTowerAttackResponse value) {
                if (value.hasError()) {
                    System.err.println("CanvasSpectateInteractor::Received: " + value.getError().getMessage());
                    // TODO: deal with error
                    return;
                }

                switch (value.getResponseType()) {
                    case CURRENT_CANVAS_STATE -> {
                        System.out.println("CanvasSpectateInteractor::Received CURRENT_CANVAS_STATE");

                        onCurrentCanvasStateReceived(value, state, canvasWidget);
                    }
                    case SELECT_SPELL_COLOR -> {
                        System.out.println("CanvasSpectateInteractor::Received SELECT_SPELL_COLOR");

                        onSelectSpellColorReceived(value);
                    }
                    case DRAW_SPELL -> {
                        System.out.println("CanvasSpectateInteractor::Received DRAW_SPELL");

                        onDrawSpellReceived(value, canvasWidget);
                    }
                    case FINISH_SPELL -> {
                        System.out.println("CanvasSpectateInteractor::Received FINISH_SPELL");
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("CanvasSpectateInteractor::onError: " + t.getMessage());
                // TODO: deal with error
            }

            @Override
            public void onCompleted() {
                System.out.println("CanvasSpectateInteractor::onCompleted");
                // TODO: redirect to another actitity
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
        System.out.println("CanvasSpectateInteractor::onSelectSpellColorReceived: newSpellColor=" + brush.getColor());
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
}

