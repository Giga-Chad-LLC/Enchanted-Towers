package enchantedtowers.client;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

    private ManagedChannel channel;
    private AuthServiceGrpc.AuthServiceStub asyncStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        asyncStub = AuthServiceGrpc.newStub(channel);
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

        asyncStub.withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .login(request, new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();

            @Override
            public void onNext(LoginResponse response) {
                // TODO: save token somewhere
                if (response.hasError()) {
                    serverError = Optional.of(response.getError());
                    logger.info("Error occurred: " + response.getError().getMessage());
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
                    ClientUtils.showSnackbar(view, "Successful login. Redirecting to map...", Snackbar.LENGTH_LONG);
                    // TODO: need to set player id (aka id of User model)
                    // TODO: redirect to map activity
                    /*Intent intent = new Intent(UserLoginActivity.this, MapActivity.class);
                    startActivity(intent);*/
                }
            }
        });
    }

}
