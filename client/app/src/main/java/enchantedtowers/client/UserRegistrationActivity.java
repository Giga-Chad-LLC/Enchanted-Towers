package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class UserRegistrationActivity extends BaseActivity {
    private final static Logger logger = Logger.getLogger(UserRegistrationActivity.class.getName());
    private ManagedChannel channel;
    AuthServiceGrpc.AuthServiceStub asyncStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // set up grpc service stub
        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        asyncStub = AuthServiceGrpc.newStub(channel);
    }

    public void sendUserDataForRegistration(View view) {
        EditText userEmailTextInput = findViewById(R.id.editTextEmailAddress);
        EditText userNameTextInput = findViewById(R.id.editTextUsername);
        EditText userPasswordTextInput = findViewById(R.id.editTextPassword);
        EditText userConfirmationPasswordTextInput = findViewById(R.id.editTextConfirmationPassword);

        String email = userEmailTextInput.getText().toString();
        String username = userNameTextInput.getText().toString();
        String password = userPasswordTextInput.getText().toString();
        String confirmationPassword = userConfirmationPasswordTextInput.getText().toString();

        logger.info("email=" + email + ", username=" + username +
                ", password=" + password + ",  confirmationPassword=" + confirmationPassword);

        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setEmail(email)
                .setUsername(username)
                .setPassword(password)
                .setConfirmationPassword(confirmationPassword)
                .build();

        asyncStub.register(request, new StreamObserver<>() {
            private Optional<ServerError> serverError = Optional.empty();

            @Override
            public void onNext(ActionResultResponse response) {
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
                    Intent intent = new Intent(UserRegistrationActivity.this, UserLoginActivity.class);
                    ClientUtils.setIntentMessage(intent, "Account created successfully! Log in into your account");
                    startActivity(intent);
                }
            }
        });
    }
}
