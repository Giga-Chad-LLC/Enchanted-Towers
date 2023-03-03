package services;

import java.util.logging.Logger;

import io.grpc.stub.StreamObserver;

// proto
import enchantedtowers.common.utils.generated.proto.GreeterGrpc;
import enchantedtowers.common.utils.generated.proto.HelloWorld;


public class GreeterService extends GreeterGrpc.GreeterImplBase {
    private static final Logger logger = Logger.getLogger(GreeterService.class.getName());
    @Override
    public void sayHello(HelloWorld.HelloRequest req, StreamObserver<HelloWorld.HelloReply> responseObserver) {
        logger.info("Got request. Name: '" + req.getName() + "'");

        HelloWorld.HelloReply reply = HelloWorld.HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
