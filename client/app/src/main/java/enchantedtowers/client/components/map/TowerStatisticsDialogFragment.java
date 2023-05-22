package enchantedtowers.client.components.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.CanvasActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
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
    // TODO: add SPECTATE state to give ability to spectate for the owner
    enum ActionButtonType {
        CAPTURE,
        ATTACK,
        SETUP_PROTECTION_WALL,
    }

    private final static Logger logger = Logger.getLogger(TowerStatisticsDialogFragment.class.getName());
    private final TowerAttackServiceGrpc.TowerAttackServiceStub towerAttackAsyncStub;
    private final ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceStub towerProtectAsyncStub;
    private final ManagedChannel channel;
    private final int towerId;

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
        // get the views and attach the listener

        Tower tower = TowersRegistry.getInstance().getTowerById(towerId).get();
        setTowerStatisticsOnView(view, tower);

        // tower has no enchanted protection walls -> capture
        // player is owner -> set protection wall
        // player is not owner -> attack

        return view;
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
            ownerIdView.setText(tower.getOwnerId().get());
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
                    getString(R.string.protection_wall_count), enchantedProtectionWallsCount, protectionWallsCount);

            wallsCountView.setText(content);
        }

        // setting action button
        ActionButtonType type;
        {
            int playerId = ClientStorage.getInstance().getPlayerId().get();

            if (!tower.isProtected()) {
                type = ActionButtonType.CAPTURE;
            }
            else if (tower.getOwnerId().isPresent() && playerId == tower.getOwnerId().get()) {
                type = ActionButtonType.SETUP_PROTECTION_WALL;
            }
            else {
                type = ActionButtonType.ATTACK;
            }
        }

        switch (type) {
            case ATTACK -> setTryAttackOnClickListener(actionButton);
            case CAPTURE -> setCaptureOnClickListener(actionButton);
            case SETUP_PROTECTION_WALL -> setTrySetupProtectionWallOnClickListener(actionButton);
        }
    }

    private void setTryAttackOnClickListener(Button actionButton) {
        actionButton.setText("Attack!");

        int playerId = ClientStorage.getInstance().getPlayerId().get();

        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);

        actionButton.setOnClickListener(view -> towerAttackAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .tryAttackTowerById(requestBuilder.build(), new StreamObserver<>() {
                    boolean serverErrorReceived = false;
                    @Override
                    public void onNext(ActionResultResponse response) {
                        // Handle the response
                        if (response.hasError()) {
                            serverErrorReceived = true;
                            // TODO: pop up notification modal
                            String message = "setAttackOnClickListener error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showToastOnUIThread(requireActivity(), message, Toast.LENGTH_LONG);
                        }
                        else {
                            System.out.println("setAttackOnClickListener::Received response: success=" + response.getSuccess());
                            ClientStorage.getInstance().setTowerId(towerId);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Handle the error
                        System.err.println("setAttackOnClickListener::Error: " + t.getMessage());
                        ClientUtils.showToastOnUIThread(requireActivity(), t.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onCompleted() {
                        // Handle the completion
                        if (!serverErrorReceived) {
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
                        // Handle the response
                        if (response.hasError()) {
                            String message = "setCaptureOnClickListener::Received error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showToastOnUIThread(requireActivity(), message, Toast.LENGTH_LONG);
                        }
                        else {
                            System.out.println("setCaptureOnClickListener::Received response: success=" + response.getSuccess());
                            ClientStorage.getInstance().setPlayerId(playerId);
                            ClientStorage.getInstance().setTowerId(towerId);
                            ClientUtils.showToastOnUIThread(requireActivity(), "Captured tower with id " + towerId, Toast.LENGTH_LONG);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Handle the error
                        System.err.println("setCaptureOnClickListener::Error: " + t.getMessage());
                        ClientUtils.showToastOnUIThread(requireActivity(), t.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onCompleted() {
                        System.err.println("setCaptureOnClickListener::Completed");
                    }
                }));
    }

    private void setTrySetupProtectionWallOnClickListener(Button actionButton) {
        actionButton.setText("Set up protection wall");
        // TODO: show modal with protection wall selection
    }

    @Override
    public void onDestroy() {
        logger.info("Shutting down...");
        channel.shutdownNow();
        try {
            // TODO: move 300 to named constant
            channel.awaitTermination(300, TimeUnit.MILLISECONDS);
            logger.info("Shut down successfully");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        super.onDestroy();
    }
}
