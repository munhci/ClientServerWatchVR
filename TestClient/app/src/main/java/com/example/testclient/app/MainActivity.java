package com.example.testclient.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.testclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainActivity extends ComponentActivity {

    // put the expected Wi-Fi name here
    // **Right now, I'm using my hotspot
    // we will setup Wi-Fi in the lab soon
    private static final String EXPECTED_SSID = "WIFI NAME HERE";
    // put the hostname here
    // This is the IP address on the headset
    private static final String hostname = "IP ADDRESS HERE";
    // put port here
    // this is just a made up 5 digit number
    private static final int port = 56411;

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        setTheme(android.R.style.Theme_DeviceDefault);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status);
        statusText.setText((CharSequence) "Awaiting setup...");

        checkWifiAndConnect();

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWifiAndConnect();
    }

    // just asks for permission for location
    // not sure why this is needed but it is
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    statusText.setText(getCurrentSSID());
                } else {
                    Toast.makeText(this, "Location permission needed for Wi-Fi info", Toast.LENGTH_SHORT).show();
                }
            });

    // checks to see if we have all necessary permissions
    // if not, requests permission from user
    private void checkWifiAndConnect() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            // gets the ssid/wifi name
            String ssid = getCurrentSSID();
            statusText.setText((CharSequence) "Connected to: " + ssid);

            // checks to make sure it's the correct wifi
            if (EXPECTED_SSID.equals(ssid)) {
                connectToServerSocket(hostname, port);
            }
            else {
                // otherwise, opens wifi settings to connect to the correct wifi
                showToast("Please connect to " + EXPECTED_SSID + " in Wi-Fi settings.");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        }
    }

    // gets current ssid (wifi name) using wifi manager
    private String getCurrentSSID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID().replace("\"", "");
    }

    // connects to our server socket and receives messages
    private void connectToServerSocket(final String host, final int port) {
        new Thread(() -> {
            // creates socket to host via port number
            try (Socket socket = new Socket(host, port)) {
                Log.d("SOCKET", "Connected");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String incoming;
                while ((incoming = reader.readLine()) != null) {
                    Log.d("SOCKET", "Received: "+ incoming);
                    showToast("Server says: " + incoming);
                }
            } catch (IOException e) {
                Log.e("SOCKET", "Socket error", e);
                showToast("Socket error: " + e.getMessage());
            }
        }).start();
    }

    // show a toast (i.e. popup)
    private void showToast(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

}