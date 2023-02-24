package com.example.simple_server_app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import enchantedtowers.sample.Sample;

import io.grpc.Grpc;
import io.grpc.StatusRuntimeException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;

        Sample.print();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
