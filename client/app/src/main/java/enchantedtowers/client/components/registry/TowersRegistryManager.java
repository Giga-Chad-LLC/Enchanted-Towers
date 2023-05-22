package enchantedtowers.client.components.registry;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class TowersRegistryManager {
    private final static Logger logger = Logger.getLogger(TowersRegistryManager.class.getName());

    public interface Callback {
        void onCompleted();
        void onError(Throwable t);
    }

    private final ManagedChannel channel;
    private final TowersServiceGrpc.TowersServiceStub asyncStub;

    public TowersRegistryManager() {
        // creating client stub
        String host   = ServerApiStorage.getInstance().getClientHost();
        int port      = ServerApiStorage.getInstance().getPort();
        String target = host + ":" + port;

        channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
        asyncStub = TowersServiceGrpc.newStub(channel);

        // registering stream observer for tower updates
        listenTowersUpdates();
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

    private void listenTowersUpdates() {
        logger.info("Listening for towers updates");
        asyncStub.listenTowersUpdates(Empty.newBuilder().build(), new StreamObserver<>() {
            @Override
            public void onNext(TowersAggregationResponse response) {
                // updating towers in towers registry
                logger.info("Towers update received");
                TowersRegistry.getInstance().updateTowersFromResponse(response);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Error occured: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                logger.info("TowersService completed connection of listenTowersUpdates request");
            }
        });
    }

    public void shutdown() {
        logger.info("Shutting down...");
        channel.shutdownNow();
        try {
            // TODO: move 300 to named constant
            channel.awaitTermination(300, TimeUnit.MILLISECONDS);
            logger.info("Shut down successfully");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
