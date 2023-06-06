package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.requests.ProtectionWallIdRequest;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionIdResponse;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


// TODO: rename/remove this activity (created only for testing)
public class AttackTowerMenuActivity extends AppCompatActivity {
    private TowerAttackServiceGrpc.TowerAttackServiceStub towerAttackAsyncStub;
    private ProtectionWallSetupServiceGrpc.ProtectionWallSetupServiceStub towerProtectAsyncStub;
    private ManagedChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_tower_menu);

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        towerAttackAsyncStub = TowerAttackServiceGrpc.newStub(channel);
        towerProtectAsyncStub = ProtectionWallSetupServiceGrpc.newStub(channel);

        // buttons
        Button attackButton   = findViewById(R.id.attackButton);
        Button spectateButton = findViewById(R.id.spectateButton);
        Button captureButton = findViewById(R.id.captureButton);
        Button protectButton = findViewById(R.id.protectButton);

        // text inputs
        EditText playerIdTextInput = findViewById(R.id.playerIdTextInput);
        EditText towerIdTextInput = findViewById(R.id.towerIdTextInput);
        EditText wallIdTextInput = findViewById(R.id.wallIdTextInput);

        attackButton.setOnClickListener(view -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                int towerId  = Integer.parseInt(towerIdTextInput.getText().toString());
                callAsyncTryAttackTowerById(playerId, towerId);
            }
            catch(Exception err) {
                System.out.println(err.getMessage());
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        spectateButton.setOnClickListener(view -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                int towerId  = Integer.parseInt(towerIdTextInput.getText().toString());
                callAsyncTrySpectateTowerById(playerId, towerId);
            }
            catch(Exception err) {
                System.out.println(err.getMessage());
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        captureButton.setOnClickListener(view -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                int towerId  = Integer.parseInt(towerIdTextInput.getText().toString());
                callAsyncCaptureTower(playerId, towerId);
            }
            catch(Exception err) {
                System.out.println(err.getMessage());
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        protectButton.setOnClickListener(view -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                int towerId  = Integer.parseInt(towerIdTextInput.getText().toString());
                int wallId   = Integer.parseInt(wallIdTextInput.getText().toString());
                callAsyncTryEnterProtectionWallCreationSession(playerId, towerId, wallId);
            }
            catch(Exception err) {
                System.out.println(err.getMessage());
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        Bundle extras = getIntent().getExtras();
        System.out.println("AttackTowerMenuActivity onStart extras: " + extras);

        if (extras != null && extras.getBoolean("showToastOnStart", false)) {
            ClientUtils.showToastOnUIThread(this, extras.getString("toastMessage", ""), Toast.LENGTH_SHORT);
        }

        super.onStart();
    }


    @Override
    protected void onDestroy() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        super.onDestroy();
    }

    // Attacker
    private void callAsyncTryAttackTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        towerAttackAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .tryAttackTowerById(requestBuilder.build(), new StreamObserver<>() {
            boolean serverErrorReceived = false;
            @Override
            public void onNext(ActionResultResponse response) {
                // Handle the response
                if (response.hasError()) {
                    serverErrorReceived = true;
                    String message = "attackTowerById::Received error: " + response.getError().getMessage();
                    System.err.println(message);
                    ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, message, Toast.LENGTH_LONG);
                }
                else {
                    // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                    System.out.println("attackTowerById::Received response: success=" + response.getSuccess());
                    ClientStorage.getInstance().setPlayerId(playerId);
                    ClientStorage.getInstance().setTowerId(towerId);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("attackTowerById::Error: " + t.getMessage());
                ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT);
            }

            @Override
            public void onCompleted() {
                // Handle the completion
                if (!serverErrorReceived) {
                    System.out.println("attackTowerById::Completed: redirecting to CanvasActivity intent");
                    Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                    intent.putExtra("isAttacking", true);
                    startActivity(intent);
                }
                else {
                    System.out.println("tryAttackTowerById::Completed: server responded with an error");
                }
            }
        });

    }

    // Spectator
    private void callAsyncTrySpectateTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        towerAttackAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .trySpectateTowerById(requestBuilder.build(), new StreamObserver<>() {
            boolean serverErrorReceived = false;
            @Override
            public void onNext(SessionIdResponse response) {
                // Handle the response
                if (response.hasError()) {
                    serverErrorReceived = true;
                    String message = "trySpectateTowerById::Received error: " + response.getError().getMessage();
                    System.err.println(message);
                    ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, message, Toast.LENGTH_LONG);
                }
                else {
                    int sessionId = response.getSessionId();
                    System.out.println("trySpectateTowerById::Received response: sessionId=" + response.getSessionId());
                    // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                    ClientStorage.getInstance().setPlayerId(playerId);
                    ClientStorage.getInstance().setSessionId(sessionId);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("trySpectateTowerById::Error: " + t.getMessage());
                ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT);
            }

            @Override
            public void onCompleted() {
                if (!serverErrorReceived) {
                    // Handle the completion
                    System.out.println("trySpectateTowerById::Completed: redirecting to CanvasActivity intent");
                    Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                    intent.putExtra("isSpectating", true);
                    startActivity(intent);
                }
                else {
                    System.out.println("trySpectateTowerById::Completed: server responded with an error");
                }
            }
        });
    }

    // Protector
    private void callAsyncCaptureTower(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        towerProtectAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .captureTower(requestBuilder.build(), new StreamObserver<>() {
                    @Override
                    public void onNext(ActionResultResponse response) {
                        // Handle the response
                        if (response.hasError()) {
                            String message = "captureTowerById::Received error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, message, Toast.LENGTH_LONG);
                        }
                        else {
                            System.out.println("captureTowerById::Received response: success=" + response.getSuccess());
                            // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                            ClientStorage.getInstance().setPlayerId(playerId);
                            ClientStorage.getInstance().setTowerId(towerId);
                            ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, "Captured tower with id " + towerId, Toast.LENGTH_LONG);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Handle the error
                        System.err.println("captureTowerById::Error: " + t.getMessage());
                        ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onCompleted() {
                        System.err.println("captureTowerById::Completed");
                    }
                });
    }

    private void callAsyncTryEnterProtectionWallCreationSession(int playerId, int towerId, int wallId) {
        ProtectionWallIdRequest.Builder requestBuilder = ProtectionWallIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        requestBuilder.setProtectionWallId(wallId);

        towerProtectAsyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .tryEnterProtectionWallCreationSession(requestBuilder.build(), new StreamObserver<>() {
                    boolean serverErrorReceived = false;
                    @Override
                    public void onNext(ActionResultResponse response) {
                        // Handle the response
                        if (response.hasError()) {
                            serverErrorReceived = true;
                            String message = "tryProtectTowerById::Received error: " + response.getError().getMessage();
                            System.err.println(message);
                            ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, message, Toast.LENGTH_LONG);
                        }
                        else {
                            System.out.println("tryProtectTowerById::Received response: success=" + response.getSuccess());
                            // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                            ClientStorage.getInstance().setPlayerId(playerId);
                            ClientStorage.getInstance().setTowerId(towerId);
                            ClientStorage.getInstance().setProtectionWallId(wallId);

                            ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, "Can setup protection wall", Toast.LENGTH_LONG);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Handle the error
                        System.err.println("tryProtectTowerById::Error: " + t.getMessage());
                        ClientUtils.showToastOnUIThread(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onCompleted() {
                        if (!serverErrorReceived) {
                            // Handle the completion
                            System.out.println("tryProtectTowerById::Completed: redirecting to CanvasActivity intent");
                            Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                            intent.putExtra("isProtecting", true);
                            startActivity(intent);
                        }
                        else {
                            System.out.println("tryProtectTowerById::Completed: server responded with an error");
                        }
                    }
                });
    }
}
