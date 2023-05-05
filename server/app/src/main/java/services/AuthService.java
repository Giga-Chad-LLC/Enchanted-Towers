package services;

import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.requests.LogoutRequest;
import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.proto.services.ProtectionWallSetupServiceGrpc;
import io.grpc.stub.StreamObserver;

public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {
    @Override
    public void register(RegistrationRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: implement
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        // TODO: implement
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: implement
    }
}
