package com.example.testclient.app;

import android.app.*;
import android.content.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.testclient.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerSocketService extends Service {

    public static final String CHANNEL_ID = "socket_service_channel";
    private static final int NOTIFICATION_ID = 42;

    // This is the wifi ssid of the router in the lab
    private static final String EXPECTED_SSID = "XCI-a30c";
    // This is the host address of the Quest headset
    // The host is the ONLY thing you may have to change
    // I noticed once it swapped from .2 to .3, keep an eye out for that
    private static final String HOST = "192.168.229.3";

    // random number
    private static final int PORT = 56411;

    private VibratorManager vibratorManager;
    private CombinedVibration clickVibration;

    private boolean running = true;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();

        // Setup vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager.getDefaultVibrator().areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_THUD)) {
                clickVibration = CombinedVibration.createParallel(
                        VibrationEffect.startComposition()
                                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD)
                                .compose()
                );
            }
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Start background loop
        new Thread(this::mainLoop).start();
    }

    private void mainLoop() {
        while (running) {
            String ssid = getCurrentSSID();
            if (ssid != null && ssid.equals(EXPECTED_SSID)) {
                broadcastStatus("Connected to: " + ssid);
                // blocks until disconnect
                connectToServerLoop();
            } else {
                broadcastStatus("Please connect to " + EXPECTED_SSID);
                Log.d("SOCKET", "Expected " + EXPECTED_SSID + " but found " + ssid);
                try {
                    // Retry SSID check every 5 s
                    Thread.sleep(5000); 
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void connectToServerLoop() {
        while (running) {
            try {
                socket = new Socket(HOST, PORT);
                Log.d("SOCKET", "Connected");
                broadcastStatus("Connected to server");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                String incoming;
                while ((incoming = reader.readLine()) != null) {
                    Log.d("SOCKET", "Received: " + incoming);

                    // Vibrate on incoming
                    if (vibratorManager != null && clickVibration != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            vibratorManager.vibrate(clickVibration);
                        }
                    }

                    // Send message to activity
                    broadcastStatus("Target ID: " + incoming);
                }

                Log.d("SOCKET", "Disconnected by server");
                broadcastStatus("Disconnected by server");

            } catch (IOException e) {
                Log.e("SOCKET", "Connection failed, retrying...", e);
                broadcastStatus("Connection failed, retrying...");
            }

            // Retry after delay
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                Log.e("SOCKET", "Retry loop interrupted", ie);
                break;
            }
        }
    }

    private String getCurrentSSID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return wifiInfo.getSSID().replace("\"", "");
        }
        return null;
    }

    private void broadcastStatus(String status) {
        Intent intent = new Intent("SOCKET_STATUS");
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not binding
    }

    private Notification createNotification() {
        // Clicking notification will bring MainActivity to foreground
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Server Connection Active")
                .setContentText("Monitoring Wi-Fi & server connection…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // your icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // FULLY visible
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Socket Background Service",
                // High importance for Wear OS, hopefully keeps things from deactivating
                NotificationManager.IMPORTANCE_HIGH 
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

    }
}
