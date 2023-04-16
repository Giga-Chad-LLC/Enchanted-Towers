package enchantedtowers.client.interactors.canvas;

import static enchantedtowers.common.utils.proto.responses.GameError.ErrorType;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Optional;
import java.util.logging.Logger;

import enchantedtowers.client.AttackTowerMenuActivity;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.AttackSessionIdRequest;
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
    private final CanvasWidget canvasWidget;
    private final Logger logger = Logger.getLogger(AttackEventWorker.class.getName());

    // TODO: watch for the race conditions in this class
    // TODO: consider scaling the points that we retrieve from server (solution: make canvas size fixed or send to the server the canvas size of attacker and recalculate real path size on spectators)

    public CanvasSpectateInteractor(CanvasState state, CanvasWidget canvasWidget) {
        // copy brush settings from CanvasState
        this.brush = state.getBrushCopy();
        this.canvasWidget = canvasWidget;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();
        Channel channel = Grpc.newChannelBuilderForAddress(
                host,
                port,
                InsecureChannelCredentials.create()
        ).build();
        asyncStub = TowerAttackServiceGrpc.newStub(channel);

        var playerId = ClientStorage.getInstance().getPlayerId();
        var sessionId = ClientStorage.getInstance().getSessionId();

        if (!(playerId.isPresent() && sessionId.isPresent())) {
            logger.warning("CanvasSpectateInteractor interactor constructor failure: present playerId=" + playerId.isPresent() + ", present sessionId=" + sessionId.isPresent());
            throw new RuntimeException("CanvasSpectateInteractor interactor constructor failure: playerId or sessionId is not present");
        }

        AttackSessionIdRequest.Builder requestBuilder = AttackSessionIdRequest.newBuilder();
        requestBuilder.setSessionId(sessionId.get());
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId.get())
                .build();

        asyncStub.spectateTowerBySessionId(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(SpectateTowerAttackResponse response) {
                if (response.hasError()) {
                    logger.info("CanvasSpectateInteractor::Received: " + response.getError().getMessage());
                    if (response.getError().getType() == ErrorType.SPELL_TEMPLATE_NOT_FOUND) {
                        // attacker did not manage to create a spell, then we just delete his drawing
                        currentPath.reset();
                        canvasWidget.invalidate();
                    }
                    else {
                        redirectToBaseActivity(Optional.of(response.getError().getMessage()));
                    }
                    return;
                }

                switch (response.getResponseType()) {
                    case CURRENT_CANVAS_STATE -> {
                        logger.info("Received CURRENT_CANVAS_STATE");
                        onCurrentCanvasStateReceived(response, state, canvasWidget);
                    }
                    case SELECT_SPELL_COLOR -> {
                        logger.info("Received SELECT_SPELL_COLOR");
                        onSelectSpellColorReceived(response);
                    }
                    case DRAW_SPELL -> {
                        logger.info("Received DRAW_SPELL");
                        onDrawSpellReceived(response, canvasWidget);
                    }
                    case FINISH_SPELL -> {
                        logger.info("Received FINISH_SPELL");
                        onFinishSpellReceived(response, state, canvasWidget);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("onError: " + t.getMessage());
                redirectToBaseActivity(Optional.ofNullable(t.getMessage()));
            }

            @Override
            public void onCompleted() {
                logger.info("onCompleted");
                redirectToBaseActivity(Optional.empty());
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

    /**
     * Goes back in activity history and removes all activities that do not match the {@code AttackTowerMenuActivity} class.
     */
    private void redirectToBaseActivity(Optional<String> message) {
        Intent intent = new Intent(canvasWidget.getContext(), AttackTowerMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (message.isPresent()) {
            intent.putExtra("showToastOnStart", true);
            intent.putExtra("toastMessage", message.get());
        }

        logger.info("redirectToBaseActivity(): from=" + canvasWidget.getContext() + ", to=" + AttackTowerMenuActivity.class + ", intent=" + intent);
        canvasWidget.getContext().startActivity(intent);
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
