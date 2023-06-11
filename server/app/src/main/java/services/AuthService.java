package services;

import components.db.UsersDao;
import components.db.models.User;
import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.requests.LogoutRequest;
import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.logging.Logger;

public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {
    private final Logger logger = Logger.getLogger(AuthService.class.getName());

    @Override
    public synchronized void register(RegistrationRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: add confirmation password into request
        ActionResultResponse.Builder responseBuilder = ActionResultResponse.newBuilder();

        UsersDao usersDao = new UsersDao();
        Optional<ServerError> serverError = validateRegistrationRequest(request, usersDao);

        if (serverError.isEmpty()) {
            String email = request.getEmail();
            String username = request.getUsername();
            String hashedPassword = hashPassword(request.getPassword());

            User user = new User(email, username, hashedPassword);
            usersDao.save(user);

            responseBuilder.setSuccess(true);
        }
        else {
            // error occurred
            logger.info("Cannot register user, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        LoginResponse.Builder responseBuilder = LoginResponse.newBuilder();

        UsersDao usersDao = new UsersDao();
        Optional<ServerError> serverError = validateLoginRequest(request, usersDao);

        if (serverError.isEmpty()) {
            // retrieve user
            Optional<User> user = usersDao.findByEmail(request.getEmail());
            assert user.isPresent();

            // TODO: set token
            responseBuilder.setId(user.get().getId())
                    .setUsername(user.get().getUsername());
        }
        else {
            // error occurred
            logger.info("Cannot login user, reason: '" + serverError.get().getMessage() + "'");
            responseBuilder.setError(serverError.get());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void logout(LogoutRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: implement
    }

    private Optional<ServerError> validateRegistrationRequest(RegistrationRequest request, UsersDao dao) {
        ServerError.Builder builder = ServerError.newBuilder();

        String email = request.getEmail();
        String username = request.getUsername();
        String password = request.getPassword();
        String confirmationPassword = request.getConfirmationPassword();

        // if there are empty fields
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                    .setMessage("All fields must be filled")
                    .build());
        }

        // if passwords do not match
        if (!password.equals(confirmationPassword)) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                    .setMessage("Passwords do not match")
                    .build());
        }

        // if email already registered
        if (dao.existsByEmail(email)) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                            .setMessage("User with provided email already exists")
                            .build());
        }

        return Optional.empty();
    }

    private Optional<ServerError> validateLoginRequest(LoginRequest request, UsersDao dao) {
        ServerError.Builder builder = ServerError.newBuilder();

        String email = request.getEmail();
        String password = request.getPassword();

        logger.info("validateLoginRequest: email=" + email + ", password=" + password);

        // if there are empty fields
        if (email.isEmpty() || password.isEmpty()) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                    .setMessage("All fields must be filled")
                    .build());
        }

        Optional<User> user = dao.findByEmail(email);

        // user with provided email does not exist
        if (user.isEmpty()) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                    .setMessage("User with provided email does not exist")
                    .build());
        }

        // passwords do not match
        boolean passwordMatch = BCrypt.checkpw(password, user.get().getPassword());

        if (!passwordMatch) {
            return Optional.of(builder.setType(ServerError.ErrorType.INVALID_REQUEST)
                    .setMessage("Provided password is incorrect")
                    .build());
        }

        return Optional.empty();
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
