package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.components.fs.JwtFileManager;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class UserLoginActivity extends BaseActivity {
    private final static Logger logger = Logger.getLogger(UserLoginActivity.class.getName());

    private Optional<ManagedChannel> channel = Optional.empty();
    private AuthServiceGrpc.AuthServiceStub asyncStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Optional.of(Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build());
        asyncStub = AuthServiceGrpc.newStub(channel.get());

        // set up redirection link
        View redirectToSignUpActivityView = findViewById(R.id.redirectToSignUpActivity);
        redirectToSignUpActivityView.setOnClickListener(v -> {
            // redirect to sign up activity
            Intent intent = new Intent(this, UserRegistrationActivity.class);
            startActivity(intent);
        });
    }

    public void sendUserDataForLogin(View view) {
        EditText userEmailTextInput = findViewById(R.id.editTextEmailAddressForLogin);
        EditText userPasswordTextInput = findViewById(R.id.editTextPasswordForLogin);

        String email = userEmailTextInput.getText().toString();
        String password = userPasswordTextInput.getText().toString();

        logger.info("email=" + email + ", password=" + password);

        LoginRequest request = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        asyncStub.login(request, new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();

            @Override
            public void onNext(LoginResponse response) {
                if (response.hasError()) {
                    serverError = Optional.of(response.getError());
                    logger.info("Error occurred: " + response.getError().getMessage());
                }
                else {
                    // saving data
                    ClientStorage.getInstance().setPlayerId(response.getId());
                    ClientStorage.getInstance().setUsername(response.getUsername());
                    ClientStorage.getInstance().setJWTToken(response.getToken());

                    // saving jwt token into file
                    JwtFileManager jwtFileManager = new JwtFileManager(UserLoginActivity.this);
                    if (!jwtFileManager.storeJwtToken(response.getToken())) {
                        logger.warning("Couldn't store jwt token into file");
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.warning("Unexpected error: " + t.getMessage() + ": " + t);
                ClientUtils.showSnackbar(view, "Unexpected error occurred. Try again!", Snackbar.LENGTH_LONG);
            }

            @Override
            public void onCompleted() {
                if (serverError.isPresent()) {
                    ClientUtils.showSnackbar(view, "Error occurred: " + serverError.get().getMessage(), Snackbar.LENGTH_LONG);
                }
                else {
                    Intent intent = new Intent(UserLoginActivity.this, MapActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (channel.isPresent()) {
            channel.get().shutdownNow();
            try {
                channel.get().awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException err) {
                err.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
