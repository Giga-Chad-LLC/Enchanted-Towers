// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: services/towers-service.proto

package enchantedtowers.common.utils.proto.services;

public final class TowersServiceOuterClass {
  private TowersServiceOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\035services/towers-service.proto\022\010service" +
      "s\032)requests/player-coordinates-request.p" +
      "roto\032\036responses/tower-response.proto2s\n\r" +
      "TowersService\022b\n\024GetTowersCoordinates\022(." +
      "request_models.PlayerCoordinatesRequest\032" +
      "\036.response_models.TowerResponse\"\000B/\n+enc" +
      "hantedtowers.common.utils.proto.services" +
      "P\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.getDescriptor(),
          enchantedtowers.common.utils.proto.responses.TowerResponseOuterClass.getDescriptor(),
        });
    enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.getDescriptor();
    enchantedtowers.common.utils.proto.responses.TowerResponseOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
