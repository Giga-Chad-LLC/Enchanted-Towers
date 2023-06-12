package interceptors;

import components.utils.GameSessionTokenUtils;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.*;

import java.util.logging.Logger;


public class GameSessionTokenRequestInterceptor implements ServerInterceptor {

    private final Logger logger = Logger.getLogger(GameSessionTokenRequestInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {

        var gameSessionToken = metadata.get(ServerApiStorage.GAME_SESSION_TOKEN_METADATA_KEY);

        logger.info("Validating game session token: '" + gameSessionToken + "'");

        if (!GameSessionTokenUtils.isValid(gameSessionToken)) {
            serverCall.close(Status.UNAUTHENTICATED.withDescription("Invalid game session token"), metadata);
            return new NoopListener<>();
        }

        return next.startCall(serverCall, metadata);
    }

    private static class NoopListener<T> extends ServerCall.Listener<T> {
        // No-op implementation of the listener, to stop processing the request
    }
}
