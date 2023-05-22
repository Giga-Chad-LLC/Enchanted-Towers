package enchantedtowers.client.components.registry;

import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.stub.StreamObserver;

public class TowersRegistryManager {
    public interface Callback {
        void onCompleted();
        void onError(Throwable t);
    }

    private final TowersServiceGrpc.TowersServiceStub asyncStub;

    public TowersRegistryManager() {
        // creating client stub
        String host   = ServerApiStorage.getInstance().getClientHost();
        int port      = ServerApiStorage.getInstance().getPort();
        String target = host + ":" + port;

        asyncStub = TowersServiceGrpc.newStub(
                Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build());
    }

    public void requestTowers(Callback callback) {
        asyncStub.withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                 .getTowers(Empty.newBuilder().build(), new StreamObserver<>() {
            @Override
            public void onNext(TowersAggregationResponse response) {
                // storing towers in towers registry
                TowersRegistry.getInstance().createTowersFromResponse(response);
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }

            @Override
            public void onCompleted() {
                callback.onCompleted();
            }
        });
    }
}
