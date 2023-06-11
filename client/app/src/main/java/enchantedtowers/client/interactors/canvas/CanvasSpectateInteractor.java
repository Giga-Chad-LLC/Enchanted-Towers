package enchantedtowers.client.interactors.canvas;


import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.MapActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.canvas.CanvasFragment;
import enchantedtowers.client.components.canvas.CanvasSessionTimer;
import enchantedtowers.client.components.canvas.CanvasSpellDecorator;
import enchantedtowers.client.components.canvas.CanvasState;
import enchantedtowers.client.components.canvas.CanvasWidget;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.SpellStat;
import enchantedtowers.common.utils.proto.requests.SessionIdRequest;
import enchantedtowers.common.utils.proto.requests.ToggleAttackerRequest;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.SessionIdResponse;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Spell;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.utils.Vector2;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

// TODO: replace canvasWidget.invalidate() with canvasWidget.postInvalidate() where needed (aka calls from non-UI threads)

public class CanvasSpectateInteractor implements CanvasInteractor {
    private final TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    private final ManagedChannel channel;
    // TODO: unite the functionality of Attack and Spectate canvas interactors
    private final Path currentPath = new Path();
    private final Paint brush;
    private final CanvasFragment canvasFragment;
    private Optional<CanvasSessionTimer> timer = Optional.empty();
    private final Logger logger = Logger.getLogger(AttackEventWorker.class.getName());

    // TODO: watch for the race conditions in this class
    // TODO: consider scaling the points that we retrieve from server (solution: make canvas size fixed or send to the server the canvas size of attacker and recalculate real path size on spectators)

    public CanvasSpectateInteractor(CanvasFragment canvasFragment, CanvasState state, CanvasWidget canvasWidget) {
        // copy brush settings from CanvasState
        this.brush = state.getBrushCopy();
        this.canvasFragment = canvasFragment;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        asyncStub = TowerAttackServiceGrpc.newStub(channel);

        var playerId = ClientStorage.getInstance().getPlayerId();
        var sessionId = ClientStorage.getInstance().getSessionId();

        if (!(playerId.isPresent() && sessionId.isPresent())) {
            logger.warning("CanvasSpectateInteractor interactor constructor failure: present playerId=" + playerId.isPresent() + ", present sessionId=" + sessionId.isPresent());
            throw new RuntimeException("CanvasSpectateInteractor interactor constructor failure: playerId or sessionId is not present");
        }

        SessionIdRequest.Builder requestBuilder = SessionIdRequest.newBuilder();
        requestBuilder.setSessionId(sessionId.get());
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId.get())
                .build();

        asyncStub.spectateTowerBySessionId(requestBuilder.build(), new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();
            private boolean protectionWallDestroyed = false;

            @Override
            public void onNext(SpectateTowerAttackResponse response) {
                if (response.hasError()) {
                    logger.info("spectateTowerBySessionId::onReceived: " + response.getError().getMessage());
                    if (response.getError().getType() == ServerError.ErrorType.SPELL_TEMPLATE_NOT_FOUND) {
                        // attacker did not manage to create a spell, then we just delete his drawing
                        currentPath.reset();
                        canvasWidget.postInvalidate();
                    }
                    else {
                        // general error occurred
                        serverError = Optional.of(response.getError());
                    }
                }
                else {
                    switch (response.getResponseType()) {
                        case CURRENT_CANVAS_STATE -> {
                            logger.info("Received CURRENT_CANVAS_STATE");
                            onCurrentCanvasStateReceived(response, state, canvasWidget);
                        }
                        case SELECT_SPELL_TYPE -> {
                            logger.info("Received SELECT_SPELL_TYPE");
                            onSelectSpellTypeReceived(response);
                        }
                        case DRAW_SPELL -> {
                            logger.info("Received DRAW_SPELL");
                            onDrawSpellReceived(response, canvasWidget);
                        }
                        case FINISH_SPELL -> {
                            logger.info("Received FINISH_SPELL");
                            onFinishSpellReceived(response, state, canvasWidget);
                        }
                        case CLEAR_CANVAS -> {
                            logger.info("Received CLEAR_CANVAS");
                            onClearCanvasReceived(state, canvasWidget);
                        }
                        case COMPARE_ENCHANTMENTS -> {
                            logger.info("Received COMPARE_ENCHANTMENTS");
                            protectionWallDestroyed = response.getProtectionWallDestroyed();
                            onCompareEnchantmentsReceived(response);
                            // clearing canvas
                            onClearCanvasReceived(state, canvasWidget);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("spectateTowerBySessionId::onError: " + t.getMessage());
                // TODO: figure out how to prevent double redirect (when clicked "back" button we redirect,
                //  but also server generates onError event, so double redirecting occurs, which we don't want
                timer.ifPresent(CanvasSessionTimer::cancel);
            }

            @Override
            public void onCompleted() {
                logger.info("spectateTowerBySessionId::onCompleted");
                String message = serverError.map(ServerError::getMessage).orElse(
                        protectionWallDestroyed ? "Protection wall destroyed by the attacker" : "Spectating session has ended");
                tearDown(message);
            }

            private void tearDown(String message) {
                timer.ifPresent(CanvasSessionTimer::cancel);
                ClientUtils.redirectToActivityAndPopHistory((Activity) canvasWidget.getContext(), MapActivity.class, message);
            }
        });
    }

    @Override
    public void onDraw(CanvasState state, Canvas canvas) {
        canvas.drawPath(currentPath, brush);
    }

    @Override
    public boolean onToggleSpectatingAttacker(ToggleAttackerRequest.RequestType requestType, CanvasState state) {
        logger.info("Show new attacker, showNext=" + requestType.name());

        var playerId = ClientStorage.getInstance().getPlayerId();
        var sessionId = ClientStorage.getInstance().getSessionId();

        if (!(playerId.isPresent() && sessionId.isPresent())) {
            logger.warning("CanvasSpectateInteractor interactor onToggleSpectatingAttacker failure: present playerId=" + playerId.isPresent() + ", present sessionId=" + sessionId.isPresent());
            throw new RuntimeException("CanvasSpectateInteractor interactor onToggleSpectatingAttacker failure: playerId or sessionId is not present");
        }

        ToggleAttackerRequest.Builder requestBuilder = ToggleAttackerRequest.newBuilder();
        requestBuilder.setSessionId(sessionId.get())
                .setRequestType(requestType);
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId.get())
                .build();

        asyncStub.toggleAttacker(requestBuilder.build(), new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();

            @Override
            public void onNext(SessionIdResponse response) {
                if (response.hasError()) {
                    logger.warning("toggleAttacker::onError: message=" + response.getError().getMessage());
                    serverError = Optional.of(response.getError());
                }
                else {
                    // setting new session
                    ClientStorage.getInstance().setSessionId(response.getSessionId());
                    // clearing cast matches
                    defaultAttackerCastMatches();
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("toggleAttacker::onError: message='" + t.getMessage() + "'");
                ClientUtils.showSnackbar(canvasFragment.requireView(), "Error occurred: " + t.getMessage(), Snackbar.LENGTH_LONG);
            }

            @Override
            public void onCompleted() {
                logger.info("toggleAttacker::onCompleted");
                serverError.ifPresent(error ->
                        ClientUtils.showSnackbar(canvasFragment.requireView(), "Error occurred: " + error.getMessage(), Snackbar.LENGTH_LONG));
            }
        });

        return true;
    }

    @Override
    public void onExecutionInterrupt() {
        logger.info("Shutting down grpc channel...");
        channel.shutdownNow();
        try {
            channel.awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void onCurrentCanvasStateReceived(SpectateTowerAttackResponse response, CanvasState state, CanvasWidget canvasWidget) {
        // clear current state
        state.clear();
        currentPath.reset();

        // append new drawings to the state
        var canvasHistory = response.getCanvasState();

        // fill current spell state
        if (canvasHistory.hasCurrentSpellState()) {
            var currentSpellState = canvasHistory.getCurrentSpellState();
            // extract data

            var currentSpellType = currentSpellState.getSpellType();
            var currentSpellPoints = currentSpellState.getPointsList();

            Path newSpellPath = new Path();
            if (!currentSpellPoints.isEmpty()) {
                newSpellPath.moveTo((float)currentSpellPoints.get(0).getX(), (float)currentSpellPoints.get(0).getY());
            }
            for (var point : currentSpellPoints) {
                newSpellPath.lineTo((float)point.getX(), (float)point.getY());
            }

            // TODO: add mutex (in order to prevent data race with onDraw method)
            // set class members values
            brush.setColor(ClientUtils.getColorIdBySpellType(currentSpellType));
            currentPath.set(newSpellPath);
        }


        // add already drawn spells
        var spellDescriptions = canvasHistory.getSpellDescriptionsList();
        for (var description : spellDescriptions) {
            var templateType = description.getSpellType();
            Vector2 templateOffset = new Vector2(
                    description.getSpellTemplateOffset().getX(),
                    description.getSpellTemplateOffset().getY()
            );
            Spell templateSpell = SpellBook.getTemplateById(description.getSpellTemplateId());

            if (templateSpell != null) {
                templateSpell.setOffset(templateOffset);

                state.addItem(new CanvasSpellDecorator(
                        templateSpell
                ));
            }
            else {
                logger.warning("onCurrentCanvasStateReceived(): template spell not found, matched spell id is " + description.getSpellTemplateId());
            }
        }

        // trigger the rendering
        canvasWidget.postInvalidate();

        // restarting canvas timer with left session time
        restartTimer(response.getLeftTimeMs());
    }

    /**
     * Restarts previously created timer on canvas view or creates new one.
     */
    private void restartTimer(long leftTime_ms) {
        logger.info("Left execution time: " + leftTime_ms + "ms");
        if (timer.isPresent()) {
            logger.info("Restarting timer...");
            timer.get().restart(leftTime_ms);
        }
        else {
            logger.warning("Timer does not exist. Creating new timer...");
            TextView leftTimeView = canvasFragment.requireActivity().findViewById(R.id.left_time_timer);
            timer = Optional.of(
                    new CanvasSessionTimer(canvasFragment.requireActivity(), leftTimeView, leftTime_ms));
        }
    }

    private void defaultAttackerCastMatches() {
        List<TextView> views = List.of(
                canvasFragment.requireActivity().findViewById(R.id.fire_cast_match_percent),
                canvasFragment.requireActivity().findViewById(R.id.wind_cast_match_percent),
                canvasFragment.requireActivity().findViewById(R.id.earth_cast_match_percent),
                canvasFragment.requireActivity().findViewById(R.id.water_cast_match_percent)
        );

        canvasFragment.requireActivity().runOnUiThread(() -> {
            for (var view : views) {
                view.setText("N/A");
            }
        });
    }

    // TODO: method is copy-paste of CanvasAttackerInteractor.setAttackerCastMatches
    private void onCompareEnchantmentsReceived(SpectateTowerAttackResponse response) {
        TextView fireMatch = canvasFragment.requireActivity().findViewById(R.id.fire_cast_match_percent);
        TextView windMatch = canvasFragment.requireActivity().findViewById(R.id.wind_cast_match_percent);
        TextView earthMatch = canvasFragment.requireActivity().findViewById(R.id.earth_cast_match_percent);
        TextView waterMatch = canvasFragment.requireActivity().findViewById(R.id.water_cast_match_percent);

        canvasFragment.requireActivity().runOnUiThread(() -> {
            for (var spellStat : response.getSpellMatchStatsList()) {
                String percent = Math.round(spellStat.getMatch() * 100) + "%";
                switch (spellStat.getSpellType()) {
                    case FIRE_SPELL -> fireMatch.setText(percent);
                    case WIND_SPELL -> windMatch.setText(percent);
                    case EARTH_SPELL -> earthMatch.setText(percent);
                    case WATER_SPELL -> waterMatch.setText(percent);
                    case UNRECOGNIZED -> logger.warning("Unrecognized spell type encountered: " + spellStat.getSpellType());
                }
            }
        });
    }

    private void onSelectSpellTypeReceived(SpectateTowerAttackResponse response) {
        // TODO: add mutex (in order to prevent data race with onDraw method)
        currentPath.reset();
        brush.setColor(
            ClientUtils.getColorIdBySpellType(response.getSpellType())
        );
        logger.info("onSelectSpellTypeReceived: newSpellType=" + response.getSpellType() + ", newSpellColor=" + brush.getColor());
    }

    private void onDrawSpellReceived(SpectateTowerAttackResponse value, CanvasWidget canvasWidget) {
        var newPoint = value.getSpellPoint();
        if (currentPath.isEmpty()) {
            currentPath.moveTo((float) newPoint.getX(), (float) newPoint.getY());
        }
        else {
            currentPath.lineTo((float) newPoint.getX(), (float) newPoint.getY());
        }

        // trigger rendering
        canvasWidget.postInvalidate();
    }

    private void onFinishSpellReceived(SpectateTowerAttackResponse response, CanvasState state, CanvasWidget canvasWidget) {
        // reset current drawing because attacked already finished it
        currentPath.reset();

        // here spell template was definitely found
        // case when it is not found is handled when server responded with SPELL_TEMPLATE_NOT_FOUND error (see above)
        var description = response.getSpellDescription();
        var templateOffset = description.getSpellTemplateOffset();
        var templateType = description.getSpellType();
        Spell templateSpell = SpellBook.getTemplateById(description.getSpellTemplateId());
        templateSpell.setOffset(new Vector2(
                templateOffset.getX(),
                templateOffset.getY()
        ));

        state.addItem(new CanvasSpellDecorator(
                templateSpell
        ));

        canvasWidget.invalidate();
    }

    private void onClearCanvasReceived(CanvasState state, CanvasWidget canvasWidget) {
        state.clear();
        currentPath.reset();
        // trigger rendering
        canvasWidget.postInvalidate();
    }
}
