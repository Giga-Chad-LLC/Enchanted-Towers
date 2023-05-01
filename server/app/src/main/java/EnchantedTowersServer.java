import components.fs.FileReader;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_logic.EnchantmetTemplatesProvider;
import enchantedtowers.game_models.SpellBook;
import enchantedtowers.game_models.SpellTemplate;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.json.JSONException;
import services.ProtectionWallSetupService;
import services.TowerAttackService;
import services.TowersService;


public class EnchantedTowersServer {
    private static final Logger logger = Logger.getLogger(EnchantedTowersServer.class.getName());
    private Server server;

    private void start() throws IOException {
        String host = ServerApiStorage.getInstance().getServerHost();
        int port = ServerApiStorage.getInstance().getPort();

        server = NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
                // GrpcServerBuilder.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new ProtectionWallSetupService())
                .addService(new TowerAttackService())
                .addService(new TowersService())
                .build()
                .start();

        logger.info("Server started: host='" + host + "', port='" + port + "'");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                EnchantedTowersServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
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
     * Loads spell templates from file system.
     * @param resourceUrl - {@link URL} to the JSON file containing description of spell templates
     */
    private static void loadSpellTemplatesFromFile(URL resourceUrl) {
        if (!SpellBook.isInstantiated()) {
            try {
                List<SpellTemplate> data = EnchantmetTemplatesProvider.parseJson(
                    FileReader.readRawFile(resourceUrl)
                );
                SpellBook.instantiate(data);
            } catch (JSONException | IOException e) {
                System.err.println("Error in loadSpellTemplatesFromFile: " + e.getMessage() + "\n" + Arrays.toString(
                    e.getStackTrace()));
            }
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // reading spell templates from json file
        URL url = EnchantedTowersServer.class.getClassLoader().getResource("canvas_templates_config.json");
        loadSpellTemplatesFromFile(url);

        // starting server
        final EnchantedTowersServer server = new EnchantedTowersServer();
        server.start();
        server.blockUntilShutdown();
    }
}
