package interceptors;

import components.utils.JwtUtils;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.*;

import java.util.logging.Logger;


public class AuthRequestInterceptor implements ServerInterceptor {

    private final Logger logger = Logger.getLogger(AuthRequestInterceptor.class.getName());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {

        var userToken = metadata.get(ServerApiStorage.AUTHORIZATION_METADATA_KEY);

        logger.info("Validating user token: '" + userToken + "'");

        try {
            JwtUtils.validate(userToken);
        }
        catch (Exception err) {
            logger.info("Invalid token: " + err.getMessage());
            err.printStackTrace();

            serverCall.close(Status.UNAUTHENTICATED.withDescription(err.getMessage()), metadata);
            return new NoopListener<>();
        }

        return next.startCall(serverCall, metadata);
    }

    private static class NoopListener<T> extends ServerCall.Listener<T> {
        // No-op implementation of the listener, to stop processing the request
    }
}
