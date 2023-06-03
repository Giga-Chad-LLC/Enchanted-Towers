package enchantedtowers.client.components.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.CanvasActivity;
import enchantedtowers.client.R;
import enchantedtowers.client.components.data.ProtectionWallData;
import enchantedtowers.client.components.map.TowerStatisticsDialogFragment;
import enchantedtowers.client.components.registry.TowersRegistry;
import enchantedtowers.client.components.registry.TowersRegistryManager;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.PlayerData;
import enchantedtowers.common.utils.proto.requests.ProtectionWallIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Tower;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


public class ProtectionWallActionsDialog extends BottomSheetDialogFragment {
    private final static Logger logger = Logger.getLogger(TowerStatisticsDialogFragment.class.getName());
    private final ProtectionWallData protectionWallData;
    private final ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceStub towerProtectAsyncStub;
    private final ManagedChannel channel;

    public static ProtectionWallActionsDialog newInstance(ProtectionWallData data) {
        return new ProtectionWallActionsDialog(data);
    }

    private ProtectionWallActionsDialog(ProtectionWallData data) {
        protectionWallData = data;

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        towerProtectAsyncStub = ProtectionWallSetupServiceGrpc.newStub(channel);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.protection_wall_actions_dialog, container, false);

        Button viewProtectionWallEnchantmentButton = view.findViewById(R.id.view_protection_wall_enchantment_button);
        Button destroyProtectionWallEnchantmentButton = view.findViewById(R.id.destroy_protection_wall_enchantment_button);

        viewProtectionWallEnchantmentButton.setOnClickListener(this::onViewProtectionWallEnchantmentClickCallback);
        destroyProtectionWallEnchantmentButton.setOnClickListener(this::onDestroyProtectionWallEnchantmentButtonClickCallback);

        return view;
    }

    private void onViewProtectionWallEnchantmentClickCallback(View view) {
        // TODO: implement view functionality
    }

    private void onDestroyProtectionWallEnchantmentButtonClickCallback(View view) {
        int playerId = ClientStorage.getInstance().getPlayerId().get();

        ProtectionWallIdRequest request = ProtectionWallIdRequest.newBuilder()
                .setTowerId(protectionWallData.getTowerId())
                .setProtectionWallId(protectionWallData.getProtectionWallId())
                .setPlayerData(PlayerData.newBuilder().setPlayerId(playerId).build())
                .build();

        towerProtectAsyncStub
            .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
            .destroyEnchantment(request, new StreamObserver<>() {
                private boolean serverErrorReceived = false;

                @Override
                public void onNext(ActionResultResponse response) {
                    if (response.hasError()) {
                        serverErrorReceived = true;
                        ClientUtils.showError(requireActivity(), response.getError().getMessage());
                    }
                    else {
                        logger.info("onDestroyProtectionWallEnchantmentButtonClickCallback: success=" + response.getSuccess());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.info("onDestroyProtectionWallEnchantmentButtonClickCallback error: " + t.getMessage());
                    ClientUtils.showError(requireActivity(), t.getMessage());
                }

                @Override
                public void onCompleted() {
                    // closing dialog & notifying of successful enchantment deletion
                    if (!serverErrorReceived) {
                        ClientUtils.showInfo(requireActivity(), "Enchantment successfully destroyed!");
                        ProtectionWallActionsDialog.this.dismiss();
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
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
