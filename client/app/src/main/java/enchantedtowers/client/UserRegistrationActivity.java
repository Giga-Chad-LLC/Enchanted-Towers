package enchantedtowers.client;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class UserRegistrationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
    }

    public void sendUserDataForRegistration(View view) {

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        ManagedChannel channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        AuthServiceGrpc.AuthServiceStub authServiceStub = AuthServiceGrpc.newStub(channel);

        EditText userNameTextInput = findViewById(R.id.editTextPersonName);
        EditText userPasswordTextInput = findViewById(R.id.editTextPassword);
        EditText userEmailTextInput = findViewById(R.id.editTextEmailAddress);

        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setUsername(userNameTextInput.getText().toString())
                .setPassword(userPasswordTextInput.getText().toString())
                .setEmail(userEmailTextInput.getText().toString())
                .build();

        authServiceStub
                .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                .register(request, new StreamObserver<>() {
            @Override
            public void onNext(ActionResultResponse value) {
                // TODO: Toast can be made only on UI thread
                runOnUiThread(() -> {
                    if (value.hasError()) {
                        Toast.makeText(UserRegistrationActivity.this, value.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UserRegistrationActivity.this, "You are registered", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> Toast.makeText(UserRegistrationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCompleted() {
                runOnUiThread(() -> Toast.makeText(UserRegistrationActivity.this, "You are registered", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
