package com.example.simple_server_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import enchantedtowers.sample.Sample;
// generated
import generated.proto.java.HelloWorld.HelloRequest;
import generated.proto.java.HelloWorld.HelloReply;
import generated.proto.java.GreeterGrpc;





class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /** Construct client for accessing HelloWorld server using the existing channel. */
    public HelloWorldClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /** Say hello to server. */
    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        System.out.println("Will try to greet " + name + " ...");

        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            System.out.println("Calling sayHello method...");
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            System.out.println("RPC failed: " + e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
        System.out.println("Greeting: " + response.getMessage());
    }

    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting. The second argument is the target server.
     */
    public static void greetWithServer(String[] args) throws Exception {
        String user = "world";
        // Access a service running on the local machine on port 50051
        // See: https://stackoverflow.com/questions/25354723/econnrefused-connection-refused-android-connect-to-webservice
        String target = "10.0.2.2:50051";// "localhost:50051";

        // Allow passing in the user and target strings as command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0])) {
                System.out.println("Usage: [name [target]]");
                System.out.println("");
                System.out.println("  name    The name you wish to be greeted by. Defaults to " + user);
                System.out.println("  target  The server to connect to. Defaults to " + target);
                // System.exit(1);
            }
            // user = args[0];
        }
        /*if (args.length > 1) {
            target = args[1];
        }*/

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .idleTimeout(10, TimeUnit.SECONDS)
                .keepAliveTime(5, TimeUnit.MINUTES)
                .build();
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            client.greet(user);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}





public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());*/

        String[] args = { "--help" };
        try {
            System.out.println("Using HelloWorldClient...");
            HelloWorldClient.greetWithServer(args);
        }
        catch (Exception err) {
            System.out.println(err.getMessage());
        }

    }
}
