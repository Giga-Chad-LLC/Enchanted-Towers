package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import enchantedtowers.common.utils.proto.requests.RegistrationRequest;
import enchantedtowers.common.utils.proto.responses.ActionResultResponse;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class UserRegistrationActivity extends AppCompatActivity {
    private AuthServiceGrpc.AuthServiceStub authServiceStub;
    //private AuthServiceGrpc.AuthServiceBlockingStub authStub;
    private ManagedChannel channel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
    }

    public void sendUserData(View view) {

        String host = ServerApiStorage.getInstance().getClientHost();
        int port = ServerApiStorage.getInstance().getPort();
        channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
        //authStub = AuthServiceGrpc.newBlockingStub(channel);
        authServiceStub = AuthServiceGrpc.newStub(channel);

        EditText userNameTextInput = findViewById(R.id.editTextPersonName);
        EditText userPasswordTextInput = findViewById(R.id.editTextPassword);
        EditText userEmailTextInput = findViewById(R.id.editTextEmailAddress);

        RegistrationRequest request = RegistrationRequest.newBuilder()
                .setUsername(userNameTextInput.getText().toString())
                .setPassword(userPasswordTextInput.getText().toString())
                .setEmail(userEmailTextInput.getText().toString())
                .build();

        //try {

        authServiceStub.register(request, new StreamObserver<>() {
            @Override
            public void onNext(ActionResultResponse value) {
                if (value.hasError()) {
                    Toast.makeText(UserRegistrationActivity.this, value.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(UserRegistrationActivity.this, "You are registered", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                Toast.makeText(UserRegistrationActivity.this, "You are registered", Toast.LENGTH_SHORT).show();
            }
        });

            /*ActionResultResponse response = authStub.register(request);
            if (response.hasError()) {
                Toast.makeText(this, response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "you registered", Toast.LENGTH_SHORT).show();
            }*/
        /*} catch (Exception err) {
            System.out.println(err.getMessage());
            Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
        }*/
    }
}
