package com.example.geotracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapsActivity extends AppCompatActivity {

    private static final String SHOW_URL =
            "http://10.0.2.2/localisation/showPositions.php";

    private MapView mapView;
    private TextView tvMarkerCount;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_maps);

        tvMarkerCount = findViewById(R.id.tvMarkerCount);
        mapView       = findViewById(R.id.mapView);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(new GeoPoint(33.5731, -7.5898));

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        loadPositions();
    }

    private void loadPositions() {
        tvMarkerCount.setText("Fetching positions from server...");

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                SHOW_URL,
                null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");
                        int count = positions.length();

                        for (int i = 0; i < count; i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            double lat  = pos.getDouble("latitude");
                            double lon  = pos.getDouble("longitude");
                            String date = pos.getString("date");
                            String imei = pos.getString("imei");

                            GeoPoint point  = new GeoPoint(lat, lon);
                            Marker   marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle("Position " + (i + 1));
                            marker.setSnippet("Date: " + date
                                    + "\nDevice: " + imei);
                            marker.setAnchor(Marker.ANCHOR_CENTER,
                                    Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(marker);
                        }

                        if (count > 0) {
                            JSONObject first = positions.getJSONObject(0);
                            mapView.getController().animateTo(
                                    new GeoPoint(
                                            first.getDouble("latitude"),
                                            first.getDouble("longitude")));
                        }

                        mapView.invalidate();
                        tvMarkerCount.setText(count + " position"
                                + (count != 1 ? "s" : "") + " loaded");

                    } catch (JSONException e) {
                        tvMarkerCount.setText("Error parsing response");
                        Toast.makeText(getApplicationContext(),
                                "Parse error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    tvMarkerCount.setText("Could not reach server");
                    Toast.makeText(getApplicationContext(),
                            "Network error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}