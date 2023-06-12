package enchantedtowers.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import enchantedtowers.client.components.fs.JwtFileManager;
import enchantedtowers.client.components.storage.ClientStorage;
import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.requests.JwtTokenRequest;
import enchantedtowers.common.utils.proto.responses.GameSessionTokenResponse;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.services.AuthServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


public class MainActivity extends AppCompatActivity {
    private static final Logger logger = Logger.getLogger(MainActivity.class.getName());
    private ManagedChannel channel;
    private AuthServiceGrpc.AuthServiceStub asyncStub;
    private final AtomicBoolean authServiceCallFinished = new AtomicBoolean(false);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JwtFileManager jwtFileManager = new JwtFileManager(this);
        Optional<String> token = jwtFileManager.getJwtToken();

        if (token.isPresent()) {
            // make call to AuthService to retrieve game session token
            String host = ServerApiStorage.getInstance().getClientHost();
            int port = ServerApiStorage.getInstance().getPort();

            channel = Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()).build();
            asyncStub = AuthServiceGrpc.newStub(channel);

            JwtTokenRequest request = JwtTokenRequest.newBuilder().setToken(token.get()).build();

            asyncStub.withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                    .createGameSessionToken(request, new StreamObserver<>() {
                private Optional<ServerError> serverError = Optional.empty();

                @Override
                public void onNext(GameSessionTokenResponse response) {
                    if (response.hasError()) {
                        serverError = Optional.of(response.getError());
                    }
                    else {
                        // setting data into client storage
                        String gameSessionToken = response.getGameSessionToken();
                        String username = response.getUsername();
                        int playerId = response.getPlayerId();

                        ClientStorage.getInstance().setGameSessionToken(gameSessionToken);
                        ClientStorage.getInstance().setUsername(username);
                        ClientStorage.getInstance().setPlayerId(playerId);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    logger.warning("Error occurred: " + t.getMessage());
                    t.printStackTrace();
                    View view = findViewById(R.id.mainActivityContainer);
                    ClientUtils.showSnackbar(view, t.getMessage(), Snackbar.LENGTH_LONG);

                    authServiceCallFinished.set(true);
                }

                @Override
                public void onCompleted() {
                    if (serverError.isPresent()) {
                        View view = findViewById(R.id.mainActivityContainer);
                        ClientUtils.showSnackbar(view, "Login required: " + serverError.get().getMessage(), Snackbar.LENGTH_LONG);
                    }
                    else {
                        // redirect to map activity
                        Intent intent = new Intent(MainActivity.this, MapActivity.class);
                        startActivity(intent);
                    }

                    authServiceCallFinished.set(true);
                }
            });
        }
        else {
            authServiceCallFinished.set(true);
        }

        /*
        // TODO: temporary solution (must be done after registration/login)
        Button button = findViewById(R.id.main_activity_set_player_id_button);
        EditText playerIdTextInput = findViewById(R.id.main_activity_player_id_text_input);
        button.setOnClickListener(v -> {
            try {
                int playerId = Integer.parseInt(playerIdTextInput.getText().toString());
                ClientStorage.getInstance().setPlayerId(playerId);

                ClientUtils.showSnackbar(playerIdTextInput, "Player id set to " + playerId, Snackbar.LENGTH_SHORT);
            } catch (NumberFormatException err) {
                ClientUtils.showSnackbar(playerIdTextInput, err.getMessage(), Snackbar.LENGTH_SHORT);
            }
        });
        */
    }

    public void changeActivity(View view) {
        /*
        if (view.getId() == R.id.changeToAttackTowerMenu) {
            Intent intent = new Intent(MainActivity.this, AttackTowerMenuActivity.class);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToCanvasActivity) {
            Intent intent = new Intent(MainActivity.this, CanvasActivity.class);
            // intent.putExtra("isAttacking", true);
            startActivity(intent);
        }
        else if (view.getId() == R.id.changeToMapActivity) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        }
        */

        if (authServiceCallFinished.get()) {
            if (view.getId() == R.id.changeToSignUpActivity) {
                Intent intent = new Intent(MainActivity.this, UserRegistrationActivity.class);
                startActivity(intent);
            }
            else if (view.getId() == R.id.changeToLoginActivity) {
                Intent intent = new Intent(MainActivity.this, UserLoginActivity.class);
                startActivity(intent);
            }
            else {
                System.err.println("Unknown view emitted `MainActivity::changeActivity`: " + view);
                ClientUtils.showSnackbar(view, "Unknown view emitted", Snackbar.LENGTH_SHORT);
            }
        }
        else {
            ClientUtils.showSnackbar(view, "Please, wait until the game preparations end", Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onDestroy() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(ServerApiStorage.getInstance().getChannelTerminationAwaitingTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException err) {
            err.printStackTrace();
        }

        super.onDestroy();
    }
}
