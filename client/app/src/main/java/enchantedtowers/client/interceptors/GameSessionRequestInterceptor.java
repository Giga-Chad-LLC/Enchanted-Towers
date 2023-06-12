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

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next
    ) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String gameSessionToken = ClientStorage.getInstance().getGameSessionToken().get();
                logger.info("Setting game session token into request: " + gameSessionToken);

                headers.put(ServerApiStorage.GAME_SESSION_TOKEN_METADATA_KEY, gameSessionToken);
                super.start(responseListener, headers);
            }
        };
    }
}
