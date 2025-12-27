package com.example.hidcontroller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectButton = findViewById(R.id.btnConnectDevice);

        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Connect button clicked");
            Intent intent = new Intent(MainActivity.this, BluetoothConnectionActivity.class);
            startActivity(intent);
        });

        Log.d(TAG, "MainActivity created");
    }
}