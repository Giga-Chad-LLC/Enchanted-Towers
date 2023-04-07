package enchantedtowers.client;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

// responses
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
// requests
import enchantedtowers.common.utils.proto.requests.TowerAttackRequest;
// services
import enchantedtowers.common.utils.proto.services.TowerAttackServiceGrpc;



public class AttackTowerMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attack_tower_menu);

        String target = "localhost:8080";// "10.0.2.2:50051";
        // Grpc.newChannelBuilderForAddress("localhost", 50051, InsecureChannelCredentials.create());

        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
        TowerAttackServiceGrpc.TowerAttackServiceBlockingStub blockingStub = TowerAttackServiceGrpc.newBlockingStub(channel);

        TowerAttackRequest.Builder requestBuilder = TowerAttackRequest.newBuilder();
        requestBuilder.setTowerId(0);
        requestBuilder.getPlayerDataBuilder()
                .setPlayerId(0)
                .build();

        ActionResultResponse response = blockingStub.attackTowerById(requestBuilder.build());
        System.out.println("Result: " + response.getSuccess());
    }
}
