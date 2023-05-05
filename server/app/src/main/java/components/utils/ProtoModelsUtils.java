package components.utils;

import enchantedtowers.common.utils.proto.responses.ServerError;

public class ProtoModelsUtils {
    private ProtoModelsUtils() {}

    public static void buildServerError(ServerError.Builder builder, ServerError.ErrorType type, String message) {
        builder.setType(type).setMessage(message).build();
    }
}
