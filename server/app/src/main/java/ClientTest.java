import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// proto
import enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest;
import enchantedtowers.common.utils.proto.responses.TowerResponse;
import enchantedtowers.common.utils.proto.responses.TowersAggregationResponse;
import enchantedtowers.common.utils.proto.services.TowersServiceGrpc;


public class ClientTest {
    private static final Logger logger = Logger.getLogger(ClientTest.class.getName());
    private final TowersServiceGrpc.TowersServiceBlockingStub blockingStub;
    public ClientTest(Channel channel) {
        blockingStub = TowersServiceGrpc.newBlockingStub(channel);
    }

    public void getTowersCoordinates() {
        PlayerCoordinatesRequest request = PlayerCoordinatesRequest.newBuilder().setX(0).setY(0).build();
        try {
            TowersAggregationResponse response = blockingStub.getTowersCoordinates(request);

            for (TowerResponse tower : response.getTowersList()) {
                logger.log(Level.INFO, "Tower[ x=" + tower.getX() + "; y=" + tower.getY() + "]");
            }
        }
        catch(StatusRuntimeException err) {
            logger.log(Level.WARNING, "RPC failed: {0}", err.getStatus());
        }
    }

    public static void main(String[] args) throws Exception {
        String target = "localhost:50051";
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();

        try {
            ClientTest client = new ClientTest(channel);
            client.getTowersCoordinates();
        }
        finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}