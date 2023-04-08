package enchantedtowers.client;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;


public class AttackTowerMenuActivity extends AppCompatActivity {
    TowerAttackServiceGrpc.TowerAttackServiceStub asyncStub;
    ManagedChannel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_tower_menu);

        /*
        "10.0.2.2:8080"  - use for emulators
        "localhost:8080" - use for android device
        "192.168.0.103:8080" - use for Wi-Fi LAN (if server must listen to 192.168.0.103:8080)
        */
        String target = "10.0.2.2:8080";
        // Grpc.newChannelBuilderForAddress("localhost", 50051, InsecureChannelCredentials.create());
        channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
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
            }

            @Override
            public void onError(Throwable t) {
                // Handle the error
                System.err.println("attackTowerById::Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                // Handle the completion
                System.out.println("attackTowerById::Completed");
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
