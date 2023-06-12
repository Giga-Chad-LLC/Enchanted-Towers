package services;

import components.db.dao.GameSessionTokensDao;
import components.db.dao.JwtTokensDao;
import components.db.dao.UsersDao;
import components.db.models.GameSessionToken;
import components.db.models.JwtToken;
import components.db.models.User;
import components.utils.GameSessionTokenUtils;
import components.utils.JwtTokenUtils;
import components.utils.ProtoModelsUtils;
import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.requests.JwtTokenRequest;
import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.GameSessionTokenResponse;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.JwtException;
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

            final int userId = user.get().getId();

            String jwsToken = JwtTokenUtils.generateJWSToken(request.getEmail());

            JwtTokensDao jwtTokensDao = new JwtTokensDao();

            // delete existing tokens
            jwtTokensDao.deleteByUserId(userId);

            // save new jwtToken in db
            JwtToken jwtToken = new JwtToken(user.get(), jwsToken);
            jwtTokensDao.save(jwtToken);

            responseBuilder.setId(userId)
                    .setToken(jwsToken)
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

    // TODO: remove this rpc method
    @Override
    public synchronized void logout(JwtTokenRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        String jws = request.getToken();
        logger.info("logout: token=" + jws);

        try {
            logger.info("token subject: '" + JwtTokenUtils.validate(jws) + "'");
            responseObserver.onNext(ActionResultResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (JwtException err) {
            logger.warning("Error: " + err);
        }
    }

    @Override
    public synchronized void createGameSessionToken(
            JwtTokenRequest request, StreamObserver<GameSessionTokenResponse> responseObserver) {
        GameSessionTokenResponse.Builder responseBuilder = GameSessionTokenResponse.newBuilder();

        GameSessionTokensDao sessionTokensDao = new GameSessionTokensDao();
        Optional<ServerError> serverError = validateGameSessionTokenCreationRequest(request);

        try {
            if (serverError.isEmpty()) {
                String jwtToken = request.getToken();
                String userEmail = JwtTokenUtils.validate(jwtToken);

                // retrieve user
                UsersDao usersDao = new UsersDao();
                Optional<User> user = usersDao.findByEmail(userEmail);

                if (user.isEmpty()) {
                    throw new Exception("User with email '" + userEmail + "' not found");
                }

                // remove game session token if any
                sessionTokensDao.deleteByUserId(user.get().getId());

                // create new game session token
                GameSessionToken sessionToken = new GameSessionToken(user.get(), GameSessionTokenUtils.generateGameSessionToken());
                sessionTokensDao.save(sessionToken);

                // setting data into response
                responseBuilder.setGameSessionToken(sessionToken.getToken())
                        .setUsername(user.get().getUsername())
                        .setPlayerId(user.get().getId());
            }
            else {
                // error occurred
                logger.info("Cannot create game session token, reason: '" + serverError.get().getMessage() + "'");
                responseBuilder.setError(serverError.get());
            }
        }
        catch (Exception err) {
            logger.info("Error occurred: " + err.getMessage());
            err.printStackTrace();

            ServerError.Builder errorBuilder = ServerError.newBuilder();
            ProtoModelsUtils.buildServerError(errorBuilder, ServerError.ErrorType.UNEXPECTED_ERROR, "Unexpected error occurred");
            responseBuilder.setError(errorBuilder.build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private Optional<ServerError> validateGameSessionTokenCreationRequest(JwtTokenRequest request) {
        ServerError.Builder errorBuilder = ServerError.newBuilder();
        boolean errorOccurred = false;

        try {
            JwtTokenUtils.validate(request.getToken());
        }
        catch (Exception err) {
            errorOccurred = true;
            ProtoModelsUtils.buildServerError(errorBuilder, ServerError.ErrorType.INVALID_REQUEST, "Invalid JWT token");
        }

        if (errorOccurred) {
            return Optional.of(errorBuilder.build());
        }
        else {
            return Optional.empty();
        }
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
