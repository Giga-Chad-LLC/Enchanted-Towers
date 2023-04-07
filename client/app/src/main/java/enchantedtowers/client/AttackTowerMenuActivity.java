package enchantedtowers.client;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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

        TowerAttackRequest.Builder requestBuilder = TowerAttackRequest.newBuilder();
        requestBuilder.setTowerId(0);
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(0)
                .build();

        /*
        "10.0.2.2:8080"  - emulators
        "localhost:8080" - android device
        */
        String target = "10.0.2.2:8080";

        System.out.println("target: " + target);

        // Grpc.newChannelBuilderForAddress("localhost", 50051, InsecureChannelCredentials.create());
        channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
        asyncStub = TowerAttackServiceGrpc.newStub(channel);

        asyncStub.attackTowerById(requestBuilder.build(), new StreamObserver<ActionResultResponse>() {
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
}
