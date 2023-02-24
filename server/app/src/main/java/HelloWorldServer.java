import enchantedtowers.sample.Sample;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import io.grpc.stub.StreamObserver;


class HelloWorldServiceImpl extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {

    @Override
    public void hello(
            HelloWorld.HelloRequest request,
            StreamObserver<HelloWorld.HelloResponse> responseObserver) {
        System.out.println(
                "Handling hello endpoint: " + request.toString());


        String text = request.getText() + " World";
        HelloWorld.HelloResponse response =
                HelloWorld.HelloResponse.newBuilder()
                        .setText(text).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}


public class HelloWorldServer {
    private static final int PORT = 50051;
    private Server server;

    public void start() throws IOException {
        server = ServerBuilder.forPort(PORT)
                .addService(new HelloWorldServiceImpl())
                .build()
                .start();
        System.out.println("Server started on port " + server.getPort());
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server == null) {
            return;
        }

        server.awaitTermination();
    }

    public static void main(String[] args)
            throws InterruptedException, IOException {
        Sample.print();
        HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }
}


/*public class HelloWorldServer {
    public static void main(String[] args) {
        Sample.print();
    }
}*/
