package com.example.testclient.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.VibrationEffect;
import android.os.VibratorManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.testclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainActivity extends ComponentActivity {

    private TextView statusText;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startSocketService();
                } else {
                    Toast.makeText(this, "Location permission needed for Wi-Fi info", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status);
        statusText.setText("Awaiting setup…");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Register to receive status updates from the service
        registerReceiver(socketStatusReceiver, new IntentFilter("SOCKET_STATUS"));

        // Check & request permission before starting service
        checkWifiPermissionAndStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(socketStatusReceiver);
    }

    private void checkWifiPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startSocketService();
        }
    }

    private void startSocketService() {
        Intent serviceIntent = new Intent(this, ServerSocketService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private final BroadcastReceiver socketStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if (status != null) {
                statusText.setText(status);
            }
        }
    };
}
//public class MainActivity extends ComponentActivity {
//
//    // put the expected Wi-Fi name here
//    // **Right now, I'm using my hotspot
//    // we will setup Wi-Fi in the lab soon
//    private static final String EXPECTED_SSID = "XCI-a30c";
//    // put the hostname here
//    // This is the IP address on the headset
//    private static final String hostname = "192.168.229.3";
//    // put port here
//    // this is just a made up 5 digit number
//    private static final int port = 56411;
//
//    // Vibrator manager so we can provide
//    // vibrotactile feedback
//    private VibratorManager vibratorManager;
//    private CombinedVibration clickVibration;
//
//    private TextView statusText;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        SplashScreen.installSplashScreen(this);
//        super.onCreate(savedInstanceState);
//
//        setTheme(android.R.style.Theme_DeviceDefault);
//        setContentView(R.layout.activity_main);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        statusText = findViewById(R.id.status);
//        statusText.setText((CharSequence) "Awaiting setup...");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
//            if (vibratorManager.getDefaultVibrator().areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
//                clickVibration = CombinedVibration.createParallel(
//                        VibrationEffect.startComposition()
//                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD)
//                                .compose()
//                );
//            }
//        }
//
//        checkWifiAndConnect();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        checkWifiAndConnect();
//    }
//
//    // just asks for permission for location
//    // not sure why this is needed but it is
//    private final ActivityResultLauncher<String> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                if (isGranted) {
//                    statusText.setText(getCurrentSSID());
//                } else {
//                    Toast.makeText(this, "Location permission needed for Wi-Fi info", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//    // checks to see if we have all necessary permissions
//    // if not, requests permission from user
//    private void checkWifiAndConnect() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
//        } else {
//            // gets the ssid/wifi name
//            String ssid = getCurrentSSID();
//            statusText.setText((CharSequence) "Connected to: " + ssid);
//
//            // checks to make sure it's the correct wifi
//            if (EXPECTED_SSID.equals(ssid)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    connectToServerSocket(hostname, port);
//                }
//            }
//            else {
//                // otherwise, opens wifi settings to connect to the correct wifi
//                statusText.setText((CharSequence) "Please connect to " + EXPECTED_SSID + " in Wi-Fi settings.");
//                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
//            }
//        }
//    }
//
//    // gets current ssid (wifi name) using wifi manager
//    private String getCurrentSSID() {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        return wifiInfo.getSSID().replace("\"", "");
//    }
//
//    // connects to our server socket and receives messages
//    @RequiresApi(api = Build.VERSION_CODES.S)
//    private void connectToServerSocket(final String host, final int port) {
//        new Thread(() -> {
//            while (true) {
//                try (Socket socket = new Socket(host, port)) {
//                    Log.d("SOCKET", "Connected");
//                    statusText.setText((CharSequence) "Connected to server");
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                    String incoming;
//                    while ((incoming = reader.readLine()) != null) {
//                        Log.d("SOCKET", "Received: " + incoming);
//                        if (vibratorManager != null && clickVibration != null) {
//                            vibratorManager.vibrate(clickVibration);
//                        }
//                        statusText.setText((CharSequence) "Target ID: " + incoming);
//                    }
//
//                    // If server closes connection gracefully
//                    Log.d("SOCKET", "Disconnected by server");
//
//                } catch (IOException e) {
//                    Log.e("SOCKET", "Connection failed, retrying...", e);
//                    statusText.setText((CharSequence) "Connection failed, retrying...");
//                }
//
//                // Wait before retrying
//                try {
//                    Thread.sleep(3000); // retry every 3 seconds
//                } catch (InterruptedException ie) {
//                    Log.e("SOCKET", "Retry loop interrupted", ie);
//                    break; // break the loop if interrupted
//                }
//            }
//        }).start();
//    }
//
//
//
//}