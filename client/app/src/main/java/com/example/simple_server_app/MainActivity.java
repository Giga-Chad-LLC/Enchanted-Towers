package com.example.simple_server_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import android.view.View;

import 	android.widget.TextView;
import android.widget.Button;

import java.net.URI;
import java.net.URISyntaxException;

import tech.gusavila92.websocketclient.WebSocketClient;



public class MainActivity extends AppCompatActivity {
    static private final String websocketServerURL = "wss://ws.postman-echo.com/raw"; // ws://192.168.0.103:8000/";
    // wss://ws.postman-echo.com/raw
    // ws://192.168.0.103:8000/ - LAN Wi-Fi
    private WebSocketClient webSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createWebSocketClient();

        // register click event
        Button button = findViewById(R.id.sendMessageButton);
        button.setOnClickListener(v -> {
            TextView messageTextView = findViewById(R.id.messageEditText);
            String message = messageTextView.getText().toString();

            webSocketClient.send(message);
        });
    }

    private void createWebSocketClient() {
        URI uri;

        try {
            // Connect to local host
            uri = new URI(websocketServerURL);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.i("WebSocket", "Connection opened");
            }

            @Override
            public void onTextReceived(String message) {
                Log.i("WebSocket", "Received message: '" + message + "'");
                runOnUiThread(() -> {
                    try {
                        TextView textView = findViewById(R.id.serverMessageTextView);
                        textView.setText(message);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onBinaryReceived(byte[] data) {}

            @Override
            public void onPingReceived(byte[] data) {}

            @Override
            public void onPongReceived(byte[] data) {}

            @Override
            public void onException(Exception err) {
                System.out.println(err.getMessage());
            }

            @Override
            public void onCloseReceived() {
                Log.i("WebSocket", "Connection closed");
            }
        };

        webSocketClient.setConnectTimeout(10_000);
        webSocketClient.setReadTimeout(60_000);
        webSocketClient.enableAutomaticReconnection(2000);
        webSocketClient.connect();
    }
}
