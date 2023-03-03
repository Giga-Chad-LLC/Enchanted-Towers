// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: requests/player-coordinates-request.proto

package enchantedtowers.common.utils.proto.requests;

/**
 * Protobuf type {@code request_models.PlayerCoordinatesRequest}
 */
public final class PlayerCoordinatesRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:request_models.PlayerCoordinatesRequest)
    PlayerCoordinatesRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use PlayerCoordinatesRequest.newBuilder() to construct.
  private PlayerCoordinatesRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PlayerCoordinatesRequest() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new PlayerCoordinatesRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.internal_static_request_models_PlayerCoordinatesRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.internal_static_request_models_PlayerCoordinatesRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.class, enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.Builder.class);
  }

  public static final int X_FIELD_NUMBER = 1;
  private double x_;
  /**
   * <code>double x = 1;</code>
   * @return The x.
   */
  @java.lang.Override
  public double getX() {
    return x_;
  }

  public static final int Y_FIELD_NUMBER = 2;
  private double y_;
  /**
   * <code>double y = 2;</code>
   * @return The y.
   */
  @java.lang.Override
  public double getY() {
    return y_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (java.lang.Double.doubleToRawLongBits(x_) != 0) {
      output.writeDouble(1, x_);
    }
    if (java.lang.Double.doubleToRawLongBits(y_) != 0) {
      output.writeDouble(2, y_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (java.lang.Double.doubleToRawLongBits(x_) != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeDoubleSize(1, x_);
    }
    if (java.lang.Double.doubleToRawLongBits(y_) != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeDoubleSize(2, y_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest)) {
      return super.equals(obj);
    }
    enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest other = (enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest) obj;

    if (java.lang.Double.doubleToLongBits(getX())
        != java.lang.Double.doubleToLongBits(
            other.getX())) return false;
    if (java.lang.Double.doubleToLongBits(getY())
        != java.lang.Double.doubleToLongBits(
            other.getY())) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + X_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        java.lang.Double.doubleToLongBits(getX()));
    hash = (37 * hash) + Y_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        java.lang.Double.doubleToLongBits(getY()));
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code request_models.PlayerCoordinatesRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:request_models.PlayerCoordinatesRequest)
      enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.internal_static_request_models_PlayerCoordinatesRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.internal_static_request_models_PlayerCoordinatesRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.class, enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.Builder.class);
    }

    // Construct using enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      x_ = 0D;

      y_ = 0D;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequestOuterClass.internal_static_request_models_PlayerCoordinatesRequest_descriptor;
    }

    @java.lang.Override
    public enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest getDefaultInstanceForType() {
      return enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.getDefaultInstance();
    }

    @java.lang.Override
    public enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest build() {
      enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest buildPartial() {
      enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest result = new enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest(this);
      result.x_ = x_;
      result.y_ = y_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest) {
        return mergeFrom((enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest other) {
      if (other == enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest.getDefaultInstance()) return this;
      if (other.getX() != 0D) {
        setX(other.getX());
      }
      if (other.getY() != 0D) {
        setY(other.getY());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 9: {
              x_ = input.readDouble();

              break;
            } // case 9
            case 17: {
              y_ = input.readDouble();

              break;
            } // case 17
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }

    private double x_ ;
    /**
     * <code>double x = 1;</code>
     * @return The x.
     */
    @java.lang.Override
    public double getX() {
      return x_;
    }
    /**
     * <code>double x = 1;</code>
     * @param value The x to set.
     * @return This builder for chaining.
     */
    public Builder setX(double value) {
      
      x_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>double x = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearX() {
      
      x_ = 0D;
      onChanged();
      return this;
    }

    private double y_ ;
    /**
     * <code>double y = 2;</code>
     * @return The y.
     */
    @java.lang.Override
    public double getY() {
      return y_;
    }
    /**
     * <code>double y = 2;</code>
     * @param value The y to set.
     * @return This builder for chaining.
     */
    public Builder setY(double value) {
      
      y_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>double y = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearY() {
      
      y_ = 0D;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:request_models.PlayerCoordinatesRequest)
  }

  // @@protoc_insertion_point(class_scope:request_models.PlayerCoordinatesRequest)
  private static final enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest();
  }

  public static enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PlayerCoordinatesRequest>
      PARSER = new com.google.protobuf.AbstractParser<PlayerCoordinatesRequest>() {
    @java.lang.Override
    public PlayerCoordinatesRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<PlayerCoordinatesRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PlayerCoordinatesRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public enchantedtowers.common.utils.proto.requests.PlayerCoordinatesRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

