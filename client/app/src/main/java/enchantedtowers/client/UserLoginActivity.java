package enchantedtowers.client;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import enchantedtowers.common.utils.proto.requests.LoginRequest;
import enchantedtowers.common.utils.proto.responses.LoginResponse;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class UserLoginActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    public void sendUserDataForLogin(View view) {
        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();

        ManagedChannel channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        AuthServiceGrpc.AuthServiceStub authServiceStub = AuthServiceGrpc.newStub(channel);

        EditText userPasswordTextInput = findViewById(R.id.editTextPasswordForLogin);
        EditText userEmailTextInput = findViewById(R.id.editTextEmailAddressForLogin);

        LoginRequest request = LoginRequest.newBuilder()
                .setEmail(userEmailTextInput.getText().toString())
                .setPassword(userPasswordTextInput.getText().toString())
                .build();

        authServiceStub.login(request, new StreamObserver<LoginResponse>() {
            @Override
            public void onNext(LoginResponse value) {
                if (value.hasError()) {
                    Toast.makeText(UserLoginActivity.this, value.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserLoginActivity.this, "You are registered", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable t) {
                Toast.makeText(UserLoginActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleted() {
                Toast.makeText(UserLoginActivity.this, "You are login", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
