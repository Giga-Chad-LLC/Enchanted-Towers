package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SessionIdResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


// TODO: rename/remove this activity (created only for testing)
public class AttackTowerMenuActivity extends AppCompatActivity {
    private TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    private ManagedChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_tower_menu);

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        /*String target = host + ":" + port;
         channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();*/
        asyncStub = TowerAttackServiceGrpc.newStub(channel);

        // buttons
        Button attackButton   = findViewById(R.id.attackButton);
        Button spectateButton = findViewById(R.id.spectateButton);

        // text inputs
        EditText playerIdTextInput = findViewById(R.id.playerIdTextInput);
        EditText towerIdTextInput = findViewById(R.id.towerIdTextInput);

        attackButton.setOnClickListener(view -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                int towerId  = Integer.parseInt(towerIdTextInput.getText().toString());
                callAsyncTryAttackTowerById(playerId, towerId);
            }
            catch(NumberFormatException err) {
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
            catch(NumberFormatException err) {
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
            showToastOnUIThread(extras.getString("toastMessage", ""), Toast.LENGTH_SHORT);
        }

        super.onStart();
    }


    private void callAsyncTryAttackTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub
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
                    showToastOnUIThread(message, Toast.LENGTH_LONG);
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
                showToastOnUIThread(t.getMessage(), Toast.LENGTH_SHORT);
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



    private void callAsyncTrySpectateTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .trySpectateTowerById(requestBuilder.build(), new StreamObserver<>() {
            boolean serverErrorReceived = false;
            @Override
            public void onNext(SessionIdResponse response) {
                // Handle the response
                if (response.hasError()) {
                    serverErrorReceived = true;
                    String message = "spectateTowerById::Received error: " + response.getError().getMessage();
                    System.err.println(message);
                    showToastOnUIThread(message, Toast.LENGTH_LONG);
                }
                else {
                    int sessionId = response.getSessionId();
                    System.out.println("spectateTowerById::Received response: sessionId=" + response.getSessionId());
                    // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                    ClientStorage.getInstance().setPlayerId(playerId);
                    ClientStorage.getInstance().setSessionId(sessionId);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("trySpectateTowerById::Error: " + t.getMessage());
                showToastOnUIThread(t.getMessage(), Toast.LENGTH_SHORT);
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

    private void showToastOnUIThread(String message, int type) {
        this.runOnUiThread(() -> Toast.makeText(this, message, type).show());
    }

    @Override
    protected void onDestroy() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(300, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        super.onDestroy();
    }
}
