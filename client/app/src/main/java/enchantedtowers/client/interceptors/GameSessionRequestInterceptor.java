package enchantedtowers.client.interceptors;

import java.util.logging.Logger;

import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class GameSessionRequestInterceptor implements ClientInterceptor {
    private final static Logger logger = Logger.getLogger(GameSessionRequestInterceptor.class.getName());

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            final MethodDescriptor<ReqT, RespT> methodDescriptor,
            final CallOptions callOptions,
            final Channel channel) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata metadata) {
                String gameSessionToken = ClientStorage.getInstance().getGameSessionToken().get();
                logger.info("Setting game session token into request");

                metadata.put(ServerApiStorage.GAME_SESSION_TOKEN_METADATA_KEY, gameSessionToken);
                super.start(responseListener, metadata);
            }
        };
    }
}
