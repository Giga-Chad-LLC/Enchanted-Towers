package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.TowerIdRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.AttackSessionIdResponse;
import enchantedtowers.common.utils.proto.responses.AttackTowerByIdResponse;
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
        // String target = host + ":" + port;

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        // channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
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

    private void callAsyncTrySpectateTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub.trySpectateTowerById(requestBuilder.build(), new StreamObserver<>() {
            private final Lock lock = new ReentrantLock();
            private final Condition responseReceivedCondition  = lock.newCondition();
            private boolean responseReceived = false;
            private boolean serverReturnedError = false;

            @Override
            public void onNext(AttackSessionIdResponse response) {
                lock.lock();
                try {
                    // Handle the response
                    if (response.hasError()) {
                        serverReturnedError = true;
                        System.err.println("spectateTowerById::Received error: " + response.getError().getMessage());
                    }
                    else {
                        int sessionId = response.getSessionId();
                        System.out.println("spectateTowerById::Received response: sessionId=" + response.getSessionId());
                        // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                        ClientStorage.getInstance().setPlayerId(playerId);
                        ClientStorage.getInstance().setSessionId(sessionId);
                        // ClientStorage.getInstance().setTowerIdUnderSpectate(towerId);
                    }
                    responseReceived = true;
                    responseReceivedCondition.signal();
                }
                finally {
                    lock.unlock();
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("trySpectateTowerById::Error: " + t.getMessage());
                Toast.makeText(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleted() {
                lock.lock();

                try {
                    // awaiting for response processing to determine whether switch intent is allowed
                    while(!responseReceived) {
                        responseReceivedCondition.await();
                    }

                    if (!serverReturnedError) {
                        // Handle the completion
                        System.out.println("trySpectateTowerById::Completed: redirecting to new intent");
                        Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                        intent.putExtra("isSpectating", true);
                        startActivity(intent);
                    }
                    else {
                        System.out.println("trySpectateTowerById::Completed: server responded with an error");
                    }
                }
                catch (InterruptedException err) {
                    throw new RuntimeException(err);
                }
                finally {
                    lock.unlock();
                }
            }
        });
    }

    private void callAsyncTryAttackTowerById(int playerId, int towerId) {
        TowerIdRequest.Builder requestBuilder = TowerIdRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub.tryAttackTowerById(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(ActionResultResponse response) {
                // Handle the response
                if (response.hasError()) {
                    System.err.println("attackTowerById::Received error: " + response.getError().getMessage());
                }
                else {
                    // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                    System.out.println("attackTowerById::Received response: success=" + response.getSuccess());
                    ClientStorage.getInstance().setPlayerId(playerId);
                    ClientStorage.getInstance().setTowerId(towerId);
                    /*
                    int sessionId = response.getSessionId();
                    System.out.println("attackTowerById::Received response: sessionId=" + sessionId);
                    ClientStorage.getInstance().setPlayerId(playerId);
                    ClientStorage.getInstance().setSessionId(sessionId);
                    */
                    // ClientStorage.getInstance().setTowerIdUnderAttack(towerId);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("attackTowerById::Error: " + t.getMessage());
                Toast.makeText(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleted() {
                // Handle the completion
                System.out.println("attackTowerById::Completed");
                Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                intent.putExtra("isAttacking", true);
                startActivity(intent);
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        channel.shutdownNow();
        try {
            channel.awaitTermination(300, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
