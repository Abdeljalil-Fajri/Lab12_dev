package com.example.geotracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOC = 100;
    private static final String INSERT_URL =
            "http://10.0.2.2/localisation/createPosition.php";

    private TextView tvLat, tvLon, tvAlt, tvAcc, tvStatus, tvDeviceId, tvLog;
    private RequestQueue requestQueue;
    private LocationManager locationManager;
    private StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLat      = findViewById(R.id.tvLat);
        tvLon      = findViewById(R.id.tvLon);
        tvAlt      = findViewById(R.id.tvAlt);
        tvAcc      = findViewById(R.id.tvAcc);
        tvStatus   = findViewById(R.id.tvStatus);
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvLog      = findViewById(R.id.tvLog);

        Button btnMap = findViewById(R.id.btnMap);

        requestQueue   = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        tvDeviceId.setText("Device ID: " + getDeviceIdentifier());

        btnMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapsActivity.class)));

        askPermissionAndStart();
    }

    private void askPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, REQ_LOC);
        } else {
            startGpsUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGpsUpdates();
        } else {
            tvStatus.setText("Permission denied");
            appendLog("Location permission denied by user");
        }
    }

    @SuppressLint("MissingPermission")
    private void startGpsUpdates() {
        tvStatus.setText("Searching for GPS signal...");
        appendLog("GPS listener started");

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000,
                150,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        double alt = location.getAltitude();
                        float  acc = location.getAccuracy();

                        tvLat.setText(String.format("%.6f", lat));
                        tvLon.setText(String.format("%.6f", lon));
                        tvAlt.setText(String.format("%.1f m", alt));
                        tvAcc.setText(String.format("%.1f m", acc));
                        tvStatus.setText("Location acquired — sending...");

                        appendLog("New position: " + lat + ", " + lon);

                        String msg = String.format(
                                getString(R.string.new_location),
                                lat, lon, alt, acc);
                        Toast.makeText(getApplicationContext(),
                                msg, Toast.LENGTH_LONG).show();

                        sendPosition(lat, lon);
                    }

                    @Override
                    public void onStatusChanged(String provider,
                                                int status, Bundle extras) {
                        String label;
                        switch (status) {
                            case LocationProvider.OUT_OF_SERVICE:
                                label = "OUT_OF_SERVICE"; break;
                            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                label = "TEMPORARILY_UNAVAILABLE"; break;
                            default:
                                label = "AVAILABLE";
                        }
                        appendLog("Provider " + provider + ": " + label);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        Toast.makeText(getApplicationContext(),
                                String.format(getString(
                                        R.string.provider_enabled), provider),
                                Toast.LENGTH_SHORT).show();
                        appendLog("Provider enabled: " + provider);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        Toast.makeText(getApplicationContext(),
                                String.format(getString(
                                        R.string.provider_disabled), provider),
                                Toast.LENGTH_SHORT).show();
                        appendLog("Provider disabled: " + provider);
                    }
                });
    }

    private void sendPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                INSERT_URL,
                response -> {
                    tvStatus.setText("Saved to database");
                    appendLog("Server: " + response.trim());
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.location_saved),
                            Toast.LENGTH_SHORT).show();
                },
                error -> {
                    tvStatus.setText("Network error");
                    appendLog("Error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Map<String, String> params = new HashMap<>();
                params.put("latitude",  String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date",      sdf.format(new Date()));
                params.put("imei",      getDeviceIdentifier());
                return params;
            }
        };

        requestQueue.add(request);
    }

    private String getDeviceIdentifier() {
        String androidId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null && !androidId.trim().isEmpty()) return androidId;
        return "UNKNOWN_DEVICE";
    }

    private void appendLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date());
        logBuilder.insert(0, "[" + timestamp + "] " + message + "\n");
        tvLog.setText(logBuilder.toString());
    }
}