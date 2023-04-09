import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

// utils
import enchantedtowers.common.utils.storage.ServerApiStorage;
// services
import services.TowerAttackService;



public class EnchantedTowersServer {
    private static final Logger logger = Logger.getLogger(EnchantedTowersServer.class.getName());
    private Server server;

    private void start() throws IOException {
        String host = ServerApiStorage.getInstance().getServerHost();
        int port = ServerApiStorage.getInstance().getPort();

        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                // GrpcServerBuilder.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new TowerAttackService())
                .build()
                .start();

        logger.info("Server started: host='" + host + "', port='" + port + "'");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    EnchantedTowersServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final EnchantedTowersServer server = new EnchantedTowersServer();
        server.start();
        server.blockUntilShutdown();
    }
}
