package enchantedtowers.client.components.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.CanvasActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.data.ProtectionWallData;
import enchantedtowers.client.components.dialogs.ProtectionWallActionsDialog;
import enchantedtowers.client.components.dialogs.ProtectionWallGridDialog;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.registry.TowersRegistryManager;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.PlayerData;
import enchantedtowers.common.utils.proto.requests.ProtectionWallIdRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionIdResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.Tower;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


public class TowerStatisticsDialogFragment extends BottomSheetDialogFragment {
    enum ActionButtonType {
        SPECTATE,
        CAPTURE,
        ATTACK,
        SETUP_PROTECTION_WALL,
    }

    private final static Logger logger = Logger.getLogger(TowerStatisticsDialogFragment.class.getName());
    private final TowerAttackServiceGrpc.TowerAttackServiceStub towerAttackAsyncStub;
    private final ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceStub towerProtectAsyncStub;
    private final ManagedChannel channel;
    private final int towerId;
    private ProtectionWallGridDialog protectionWallDialog;
    private Optional<TowersRegistryManager.Subscription> onTowerUpdateSubscription = Optional.empty();

    public static TowerStatisticsDialogFragment newInstance(int towerId) {
        return new TowerStatisticsDialogFragment(towerId);
    }

    private TowerStatisticsDialogFragment(int towerId) {
        this.towerId = towerId;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        towerAttackAsyncStub = TowerAttackServiceGrpc.newStub(channel);
        towerProtectAsyncStub = ProtectionWallSetupServiceGrpc.newStub(channel);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tower_statistics_dialog, container, false);

        // create protection walls dialog
        protectionWallDialog = ProtectionWallGridDialog.newInstance(requireContext(), this::onProtectionWallClick);

        Tower tower = TowersRegistry.getInstance().getTowerById(towerId).get();
        setTowerStatisticsOnView(view, tower);

        // subscribe to tower updates
        logger.info("Register subscription for updates of tower with id " + towerId);
        onTowerUpdateSubscription = Optional.of(updatedTower -> onTowerUpdateCallback(view, updatedTower));
        TowersRegistryManager.getInstance().subscribeOnTowerUpdates(towerId, onTowerUpdateSubscription.get());

        return view;
    }

    private void onTowerUpdateCallback(View view, Tower updatedTower) {
        requireActivity().runOnUiThread(() -> {
            logger.info("Received update of tower with id " + towerId);
            setTowerStatisticsOnView(view, updatedTower);
            view.postInvalidate();
        });
    }

    private void setTowerStatisticsOnView(View view, Tower tower) {
        TextView ownerIdView = view.findViewById(R.id.owner_id);
        TextView mageLevelView = view.findViewById(R.id.mage_level);
        TextView elementsView = view.findViewById(R.id.used_elements);
        TextView wallsCountView = view.findViewById(R.id.walls_count);
        Button actionButton = view.findViewById(R.id.action_button);

        // TODO: replace with owner username later
        // setting owner view
        if (tower.getOwnerId().isPresent()) {
            String username = tower.getOwnerId().get().toString();
            String content = String.format(view.getContext().getString(R.string.username), username);
            ownerIdView.setText(content);
        }
        else {
            ownerIdView.setText(R.string.abandoned);
        }

        // TODO: replace boilerplate with real content later
        // setting mage level view
        mageLevelView.setText("20");

        // TODO: replace with icons
        // setting elements
        elementsView.setText("Fire, Water");

        // setting walls count
        {
            long enchantedProtectionWallsCount = tower.getProtectionWalls().stream().filter(ProtectionWall::isEnchanted).count();
            long protectionWallsCount = tower.getProtectionWalls().size();
            String content = String.format(
                    view.getContext().getString(R.string.protection_wall_count), enchantedProtectionWallsCount, protectionWallsCount);

            wallsCountView.setText(content);
        }

        // setting action button
        ActionButtonType type;
        {
            int playerId = ClientStorage.getInstance().getPlayerId().get();
            boolean isPlayerTowerOwner = tower.getOwnerId().isPresent() && playerId == tower.getOwnerId().get();

            if (isPlayerTowerOwner && tower.isUnderAttack()) {
                type = ActionButtonType.SPECTATE;
            }
            else if (isPlayerTowerOwner) {
                type = ActionButtonType.SETUP_PROTECTION_WALL;
            }
            else if (!tower.isProtected()) {
                type = ActionButtonType.CAPTURE;
            }
            else {
                type = ActionButtonType.ATTACK;
            }
        }

        logger.info("Selected type of button: " + type);

        // if other listeners assigned remove them
        if (actionButton.hasOnClickListeners()) {
            actionButton.setOnClickListener(null);
        }

        switch (type) {
            case SPECTATE -> setSpectateOnClickListener(actionButton);
            // TODO: remove tower from param since towerId is available
            case SETUP_PROTECTION_WALL -> setTrySetupProtectionWallOnClickListener(actionButton);
            case CAPTURE -> setCaptureOnClickListener(actionButton);
            case ATTACK -> setTryAttackOnClickListener(actionButton);
        }
    }

    private void setSpectateOnClickListener(Button actionButton) {
        actionButton.setText("Spectate");

        int playerId = ClientStorage.getInstance().getPlayerId().get();

        TowerIdRequest request = TowerIdRequest.newBuilder()
                .setTowerId(towerId)
                .setPlayerData(PlayerData.newBuilder().setPlayerId(playerId).build())
                .build();

        actionButton.setOnClickListener(v -> towerAttackAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .trySpectateTowerById(request, new StreamObserver<>() {
                    private boolean serverErrorReceived = false;
                    @Override
                    public void onNext(SessionIdResponse response) {
                        if (response.hasError()) {
                            serverErrorReceived = true;
                            String message = "setSpectateOnClickListener error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showError(requireActivity(), message);
                        }
                        else {
                            System.err.println("setSpectateOnClickListener session id: " + response.getSessionId());
                            ClientStorage.getInstance().setSessionId(response.getSessionId());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("setSpectateOnClickListener error: " + t.getMessage());
                        ClientUtils.showError(requireActivity(), t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        // redirecting to spectate canvas
                        if (!serverErrorReceived) {
                            System.out.println("setSpectateOnClickListener completed: redirecting to CanvasActivity intent");
                            Intent intent = new Intent(requireActivity(), CanvasActivity.class);
                            intent.putExtra("isSpectating", true);
                            startActivity(intent);
                        }
                        else {
                            System.out.println("setSpectateOnClickListener completed: server responded with an error");
                        }
                    }
                }));
    }

    private void setTryAttackOnClickListener(Button actionButton) {
        actionButton.setText("Attack!");

        int playerId = ClientStorage.getInstance().getPlayerId().get();

        TowerIdRequest request = TowerIdRequest.newBuilder()
                .setTowerId(towerId)
                .setPlayerData(PlayerData.newBuilder().setPlayerId(playerId).build())
                .build();

        actionButton.setOnClickListener(v -> towerAttackAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .tryAttackTowerById(request, new StreamObserver<>() {
                    boolean serverErrorReceived = false;
                    @Override
                    public void onNext(ActionResultResponse response) {
                        if (response.hasError()) {
                            serverErrorReceived = true;
                            String message = "setAttackOnClickListener error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showError(requireActivity(), message);
                        }
                        else {
                            System.out.println("setAttackOnClickListener::Received response: success=" + response.getSuccess());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("setAttackOnClickListener::Error: " + t.getMessage());
                        ClientUtils.showError(requireActivity(), t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        if (!serverErrorReceived) {
                            ClientStorage.getInstance().setTowerId(towerId);
                            System.out.println("setAttackOnClickListener::Completed: redirecting to CanvasActivity intent");
                            Intent intent = new Intent(requireActivity(), CanvasActivity.class);
                            intent.putExtra("isAttacking", true);
                            startActivity(intent);
                        }
                        else {
                            System.out.println("setAttackOnClickListener::Completed: server responded with an error");
                        }
                    }
                }));
    }

    private void setCaptureOnClickListener(Button actionButton) {
        actionButton.setText("Capture!");

        int playerId = ClientStorage.getInstance().getPlayerId().get();

        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);

        actionButton.setOnClickListener(view -> towerProtectAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .captureTower(requestBuilder.build(), new StreamObserver<>() {
                    @Override
                    public void onNext(ActionResultResponse response) {
                        if (response.hasError()) {
                            String message = "setCaptureOnClickListener::Received error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showError(requireActivity(), message);
                        }
                        else {
                            System.out.println("setCaptureOnClickListener::Received response: success=" + response.getSuccess());
                            ClientStorage.getInstance().setPlayerId(playerId);
                            ClientStorage.getInstance().setTowerId(towerId);
                            ClientUtils.showInfo(requireActivity(), "Captured tower with id " + towerId);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("setCaptureOnClickListener::Error: " + t.getMessage());
                        ClientUtils.showError(requireActivity(), t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        System.err.println("setCaptureOnClickListener::Completed");
                    }
                }));
    }

    private void onProtectionWallClick(ProtectionWallData data) {
        boolean isEnchanted = TowersRegistry.getInstance()
                .getTowerById(data.getTowerId()).get()
                .getProtectionWallById(data.getProtectionWallId()).get()
                .isEnchanted();

        // wall not enchanted -> enter protection wall creation
        // otherwise -> open action dialog
        if (isEnchanted) {
            ProtectionWallActionsDialog dialog = ProtectionWallActionsDialog.newInstance(data);
            dialog.show(getParentFragmentManager(), dialog.getTag());
        }
        else {
            makeTryEnterProtectionWallCreationSessionAsyncCall(data);
        }
    }


    private void makeTryEnterProtectionWallCreationSessionAsyncCall(ProtectionWallData data) {
        int playerId = ClientStorage.getInstance().getPlayerId().get();

        ProtectionWallIdRequest request = ProtectionWallIdRequest.newBuilder()
                .setTowerId(data.getTowerId())
                .setProtectionWallId(data.getProtectionWallId())
                .setPlayerData(PlayerData.newBuilder().setPlayerId(playerId).build())
                .build();

        towerProtectAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .tryEnterProtectionWallCreationSession(request, new StreamObserver<>() {
                    boolean serverErrorReceived = false;
                    @Override
                    public void onNext(ActionResultResponse response) {
                        if (response.hasError()) {
                            serverErrorReceived = true;
                            String message = "onProtectionWallClick error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showError(requireActivity(), message);
                        }
                        else {
                            System.out.println("onProtectionWallClick received response: success=" + response.getSuccess());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("onProtectionWallClick error: " + t.getMessage());
                        ClientUtils.showError(requireActivity(), t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        if (!serverErrorReceived) {
                            // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                            ClientStorage.getInstance().setTowerId(towerId);
                            ClientStorage.getInstance().setProtectionWallId(data.getProtectionWallId());

                            System.out.println("onProtectionWallClick completed: redirecting to CanvasActivity intent");
                            Intent intent = new Intent(requireActivity(), CanvasActivity.class);
                            intent.putExtra("isProtecting", true);
                            startActivity(intent);
                        }
                        else {
                            System.out.println("onProtectionWallClick completed: server responded with an error");
                        }
                    }
                });
    }

    private void setTrySetupProtectionWallOnClickListener(Button actionButton) {
        actionButton.setText("Set up protection wall");

        List<Integer> availableEnchantedWallsImages = List.of(
                R.drawable.protection_wall_frame_1,
                R.drawable.protection_wall_frame_2,
                R.drawable.protection_wall_frame_3,
                R.drawable.protection_wall_frame_4,
                R.drawable.protection_wall_frame_5
        );

        // clear previously added data
        protectionWallDialog.clear();

        Tower tower = TowersRegistry.getInstance().getTowerById(towerId).get();
        for (var wall : tower.getProtectionWalls()) {
            int imageId = R.drawable.protection_wall_frame_empty;
            String title = "Non-enchanted";

            if (wall.isEnchanted()) {
                // choose random image from available ones
                int index = ThreadLocalRandom.current().nextInt(availableEnchantedWallsImages.size());
                imageId = availableEnchantedWallsImages.get(index);
                title = "Enchanted";
            }

            protectionWallDialog.addImage(tower.getId(), wall.getId(), imageId, title);
        }

        actionButton.setOnClickListener(view -> protectionWallDialog.show());
    }

    @Override
    public void onDestroy() {
        logger.info("Unregister subscription for updates of tower with id " + towerId);
        // unregister tower updates subscription
        onTowerUpdateSubscription.ifPresent(
                subscription -> TowersRegistryManager.getInstance().unsubscribeFromTowerUpdates(towerId, subscription));

        // removing listeners from action button
        {
            var activity = getActivity();
            if (activity != null) {
                Button actionButton = activity.findViewById(R.id.action_button);
                if (actionButton != null && actionButton.hasOnClickListeners()) {
                    actionButton.setOnClickListener(null);
                }
            }
        }

        // dismissing protection wall dialog
        if (protectionWallDialog.isShowing()) {
            protectionWallDialog.dismiss();
        }

        try {
            logger.info("Shutting down...");
            channel.shutdownNow();
            channel.awaitTermination(
                    ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
            logger.info("Shut down successfully");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        super.onDestroy();
    }
}
