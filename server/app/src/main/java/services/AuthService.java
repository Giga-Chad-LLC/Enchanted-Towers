package services;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import components.db.User;
import components.db.UsersDataBase;
import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.requests.LogoutRequest;
import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import io.grpc.stub.StreamObserver;

public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {
    private final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final UsersDataBase usersDataBase = new UsersDataBase();

    //List<User> users = new ArrayList<>();

    @Override
    public void register(RegistrationRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        User newUser = new User();

        newUser.setEmail(request.getEmail());
        newUser.setName(request.getUsername());
        newUser.setHashedPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

        if (usersDataBase.findUserRecordByEmail(newUser.getEmail()) != null) {
            ServerError error = ServerError.newBuilder().setMessage("User's name already exist").setType(ServerError.ErrorType.INVALID_REQUEST).build();
            ActionResultResponse response = ActionResultResponse.newBuilder().setError(error).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.log(Level.INFO, "Name of new user already exist");
        }

        if (usersDataBase.findUserRecordByName(newUser.getName()) != null) {
            ServerError error = ServerError.newBuilder().setMessage("User's email already exist").setType(ServerError.ErrorType.INVALID_REQUEST).build();
            ActionResultResponse response = ActionResultResponse.newBuilder().setError(error).build();
            responseObserver.onNext(response);
            logger.log(Level.INFO, "Email of new user already exist");
            responseObserver.onCompleted();
        }


        //newUser.eMail = request.getEmail();
        //newUser.userName = request.getUsername();
        //newUser.password = request.getPassword();

        /*for (var user: users) {
            if (user.userName.equals(newUser.userName)) {
                ServerError error = ServerError.newBuilder().setMessage("User already exist").setType(ServerError.ErrorType.INVALID_REQUEST).build();
                ActionResultResponse response = ActionResultResponse.newBuilder().setError(error).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                logger.log(Level.INFO, "Name of new user already exist");
                return;
            }

            if (user.eMail.equals(newUser.eMail)) {
                ServerError error = ServerError.newBuilder().setMessage("Email already exist").setType(ServerError.ErrorType.INVALID_REQUEST).build();
                ActionResultResponse response = ActionResultResponse.newBuilder().setError(error).build();
                responseObserver.onNext(response);
                logger.log(Level.INFO, "Email of new user already exist");
                responseObserver.onCompleted();
                return;
            }
        }*/

        responseObserver.onNext(ActionResultResponse.newBuilder().build());
        usersDataBase.saveUserRecord(newUser);
        //users.add(newUser);
        logger.log(Level.INFO, "New user(Email: " + newUser.getEmail() + ", Name: " + newUser.getName() + ", Password:" +newUser.getHashedPassword() + ")");

        responseObserver.onCompleted();

        // TODO: rewrite
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        User newUser = new User();

        //newUser.eMail = request.getEmail();
        //newUser.password = request.getPassword();
        newUser.setEmail(request.getEmail());
        newUser.setHashedPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        logger.log(Level.INFO, "Next user(Email: " + newUser.getEmail()  + ", Password:" +newUser.getHashedPassword() + ")");

        if (usersDataBase.findUserRecordByEmail(newUser.getEmail()) != null) {
            LoginResponse response = LoginResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.log(Level.INFO, "User is find");
            return;
        }

        /*for (var user: users) {
            if (user.eMail.equals(newUser.eMail) && user.password.equals(newUser.password)) {
                LoginResponse response = LoginResponse.newBuilder().build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                logger.log(Level.INFO, "User is find");
                return;
            }
        }*/

        logger.log(Level.INFO, "User not find");
        ServerError error = ServerError.newBuilder().setMessage("User not exist").setType(ServerError.ErrorType.INVALID_REQUEST).build();
        LoginResponse response = LoginResponse.newBuilder().setError(error).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        // TODO: rewrite
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<ActionResultResponse> responseObserver) {
        // TODO: implement
    }
}
