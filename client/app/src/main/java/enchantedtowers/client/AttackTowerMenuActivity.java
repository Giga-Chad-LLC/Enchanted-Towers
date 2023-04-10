package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.SpectateTowerAttackResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

// utils
import enchantedtowers.common.utils.storage.ServerApiStorage;


// TODO: rename/remove this activity (created only for testing)
public class AttackTowerMenuActivity extends AppCompatActivity {
    TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    ManagedChannel channel;

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
                callAsyncAttackTowerById(playerId, towerId);
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
                callAsyncEnterSpectateTowerById(playerId, towerId);
            }
            catch(NumberFormatException err) {
                System.out.println(err.getMessage());
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callAsyncEnterSpectateTowerById(int playerId, int towerId) {
        TowerAttackRequest.Builder requestBuilder = TowerAttackRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub.enterSpectatingTowerById(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(ActionResultResponse response) {
                // Handle the response
                System.out.println("spectateTowerById::Received response: " + response.getSuccess());
                // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                ClientStorage.getInstance().setPlayerId(playerId);
                ClientStorage.getInstance().setTowerIdUnderSpectate(towerId);
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("spectateTowerById::Error: " + t.getMessage());
                Toast.makeText(AttackTowerMenuActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleted() {
                // Handle the completion
                System.out.println("spectateTowerById::Completed");
                Intent intent = new Intent(AttackTowerMenuActivity.this, CanvasActivity.class);
                intent.putExtra("isSpectating", true);
                startActivity(intent);
            }
        });
    }

    private void callAsyncAttackTowerById(int playerId, int towerId) {
        TowerAttackRequest.Builder requestBuilder = TowerAttackRequest.newBuilder();
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(playerId)
                .build();
        requestBuilder.setTowerId(towerId);
        asyncStub.attackTowerById(requestBuilder.build(), new StreamObserver<>() {
            @Override
            public void onNext(ActionResultResponse response) {
                // Handle the response
                System.out.println("attackTowerById::Received response: " + response.getSuccess());
                // TODO: part with setting playerId will be done on login/register activity when the authentication will be done
                ClientStorage.getInstance().setPlayerId(playerId);
                ClientStorage.getInstance().setTowerIdUnderAttack(towerId);
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
