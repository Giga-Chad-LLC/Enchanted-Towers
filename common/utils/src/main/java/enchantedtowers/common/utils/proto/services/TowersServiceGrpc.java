package enchantedtowers.common.utils.proto.services;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.53.0)",
    comments = "Source: services/towers-service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class TowersServiceGrpc {

  private TowersServiceGrpc() {}

  public static final String SERVICE_NAME = "services.TowersService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest,
      enchantedtowers.common.utils.proto.responses.TowerResponse> getGetTowersCoordinatesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTowersCoordinates",
      requestType = enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.class,
      responseType = enchantedtowers.common.utils.proto.responses.TowerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest,
      enchantedtowers.common.utils.proto.responses.TowerResponse> getGetTowersCoordinatesMethod() {
    io.grpc.MethodDescriptor<enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest, enchantedtowers.common.utils.proto.responses.TowerResponse> getGetTowersCoordinatesMethod;
    if ((getGetTowersCoordinatesMethod = TowersServiceGrpc.getGetTowersCoordinatesMethod) == null) {
      synchronized (TowersServiceGrpc.class) {
        if ((getGetTowersCoordinatesMethod = TowersServiceGrpc.getGetTowersCoordinatesMethod) == null) {
          TowersServiceGrpc.getGetTowersCoordinatesMethod = getGetTowersCoordinatesMethod =
              io.grpc.MethodDescriptor.<enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest, enchantedtowers.common.utils.proto.responses.TowerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTowersCoordinates"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  enchantedtowers.common.utils.proto.responses.TowerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TowersServiceMethodDescriptorSupplier("GetTowersCoordinates"))
              .build();
        }
      }
    }
    return getGetTowersCoordinatesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TowersServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TowersServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TowersServiceStub>() {
        @java.lang.Override
        public TowersServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TowersServiceStub(channel, callOptions);
        }
      };
    return TowersServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TowersServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TowersServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TowersServiceBlockingStub>() {
        @java.lang.Override
        public TowersServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TowersServiceBlockingStub(channel, callOptions);
        }
      };
    return TowersServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TowersServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TowersServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TowersServiceFutureStub>() {
        @java.lang.Override
        public TowersServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TowersServiceFutureStub(channel, callOptions);
        }
      };
    return TowersServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class TowersServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void getTowersCoordinates(enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest request,
        io.grpc.stub.StreamObserver<enchantedtowers.common.utils.proto.responses.TowerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTowersCoordinatesMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetTowersCoordinatesMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest,
                enchantedtowers.common.utils.proto.responses.TowerResponse>(
                  this, METHODID_GET_TOWERS_COORDINATES)))
          .build();
    }
  }

  /**
   */
  public static final class TowersServiceStub extends io.grpc.stub.AbstractAsyncStub<TowersServiceStub> {
    private TowersServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TowersServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TowersServiceStub(channel, callOptions);
    }

    /**
     */
    public void getTowersCoordinates(enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest request,
        io.grpc.stub.StreamObserver<enchantedtowers.common.utils.proto.responses.TowerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetTowersCoordinatesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class TowersServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<TowersServiceBlockingStub> {
    private TowersServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TowersServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TowersServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<enchantedtowers.common.utils.proto.responses.TowerResponse> getTowersCoordinates(
        enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetTowersCoordinatesMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class TowersServiceFutureStub extends io.grpc.stub.AbstractFutureStub<TowersServiceFutureStub> {
    private TowersServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TowersServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TowersServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_GET_TOWERS_COORDINATES = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TowersServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(TowersServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TOWERS_COORDINATES:
          serviceImpl.getTowersCoordinates((enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest) request,
              (io.grpc.stub.StreamObserver<enchantedtowers.common.utils.proto.responses.TowerResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class TowersServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TowersServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return enchantedtowers.common.utils.proto.services.TowersServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TowersService");
    }
  }

  private static final class TowersServiceFileDescriptorSupplier
      extends TowersServiceBaseDescriptorSupplier {
    TowersServiceFileDescriptorSupplier() {}
  }

  private static final class TowersServiceMethodDescriptorSupplier
      extends TowersServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    TowersServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TowersServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TowersServiceFileDescriptorSupplier())
              .addMethod(getGetTowersCoordinatesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
