package enchantedtowers.client.components.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_models.Tower;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class TowersRegistryManager {
    private static class Holder {
        private static final TowersRegistryManager INSTANCE = new TowersRegistryManager();
    }

    public interface Callback {
        void onCompleted();
        void onError(Throwable t);
    }

    @FunctionalInterface
    public interface Subscription {
        void run(Tower tower);
    }

    private final static Logger logger = Logger.getLogger(TowersRegistryManager.class.getName());
    private final Map<Integer, List<Subscription>> subscriptions = new HashMap<>();
    private ManagedChannel channel;
    private TowersServiceGrpc.TowersServiceStub asyncStub;
    private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);

    public static TowersRegistryManager getInstance() {
        return Holder.INSTANCE;
    }

    private TowersRegistryManager() {
        logger.info("Calling setup from constructor...");
        setup();
    }

    private void setup() {
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
        if (shutdownCalled.get()) {
            setup();
            shutdownCalled.set(false);
        }

        asyncStub.getTowers(Empty.newBuilder().build(), new StreamObserver<>() {
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

    public void subscribeOnTowerUpdates(int towerId, Subscription subscription) {
        synchronized (subscriptions) {
            if (!subscriptions.containsKey(towerId)) {
                subscriptions.put(towerId, new ArrayList<>());
            }

            subscriptions.get(towerId).add(subscription);
        }
    }

    public void unsubscribeFromTowerUpdates(int towerId, Subscription subscription) {
        synchronized (subscriptions) {
            // if subscription is not registered
            if (!(subscriptions.containsKey(towerId) &&
                  Objects.requireNonNull(subscriptions.get(towerId)).contains(subscription))) {
                throw new NoSuchElementException("Subscription not found: " + subscription);
            }
            // remove subscription
            Objects.requireNonNull(subscriptions.get(towerId)).remove(subscription);
        }
    }

    private void listenTowersUpdates() {
        logger.info("Listening for towers updates");
        asyncStub.listenTowersUpdates(Empty.newBuilder().build(), new StreamObserver<>() {
            @Override
            public void onNext(TowersAggregationResponse response) {
                // updating towers in towers registry
                logger.info("Towers update received: towers count " + response.getTowersList().size());
                TowersRegistry.getInstance().updateTowersFromResponse(response);

                List<Integer> towerIds = new ArrayList<>();
                for (var tower : response.getTowersList()) {
                    towerIds.add(tower.getTowerId());
                }
                notifySubscribers(towerIds);
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Error occurred: " + t.getMessage());
                if (!(t instanceof StatusRuntimeException)) {
                    throw new RuntimeException(t);
                }
            }

            @Override
            public void onCompleted() {
                logger.info("TowersService completed connection of listenTowersUpdates request");
            }
        });
    }

    private void notifySubscribers(List<Integer> towerIds) {
        synchronized (subscriptions) {
            logger.info("Notifying subscribers of update of towers: " + towerIds);

            for (int id : towerIds) {
                if (subscriptions.containsKey(id)) {
                    List<Subscription> subscribers = subscriptions.get(id);
                    Objects.requireNonNull(subscribers);

                    for (var subscriber : subscribers) {
                        subscriber.run(TowersRegistry.getInstance().getTowerById(id).get());
                    }
                }
            }
        }
    }

    public void shutdown() {
        try {
            logger.info("Shutting down...");
            channel.shutdownNow();
            channel.awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
            logger.info("Shut down successfully");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            shutdownCalled.set(true);
        }
    }
}
