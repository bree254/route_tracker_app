package com.example.routetracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import org.json.*;

import java.io.IOException;
import java.util.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private LatLng pickupLocation;
    private LatLng dropoffLocation;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private Marker userMarker;

    private final OkHttpClient httpClient = new OkHttpClient();
    private LocationCallback locationCallback;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        enableUserLocation();
        myMap.setOnMarkerClickListener(this);

        myMap.setOnMapClickListener(latLng -> {
            if (pickupLocation == null) {
                pickupLocation = latLng;
                pickupMarker = myMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                if (pickupMarker != null) pickupMarker.setTag(0);
            } else if (dropoffLocation == null) {
                dropoffLocation = latLng;
                dropoffMarker = myMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Drop-off Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                if (dropoffMarker != null) dropoffMarker.setTag(0);

                getRoute(pickupLocation, dropoffLocation); // Draw route
            } else {
                // Reset
                myMap.clear();
                pickupLocation = latLng;
                dropoffLocation = null;

                pickupMarker = myMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Pickup Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                if (pickupMarker != null) pickupMarker.setTag(0);
            }
        });
    }

//    private void enableUserLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            myMap.setMyLocationEnabled(true);
//            fusedLocationProviderClient.getLastLocation()
//                    .addOnSuccessListener(this, location -> {
//                        if (location != null) {
//                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
//                            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
//                        }
//                    });
//            startLocationUpdates();
//        }
//    }

//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create()
//                .setInterval(5000)
//                .setFastestInterval(2000)
//                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                for (Location location : locationResult.getLocations()) {
//                    updateUserLocationMarker(location);
//                }
//            }
//        };
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            myMap.setMyLocationEnabled(true);

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        }
                    });

            startLocationUpdates(); // Ensures tracking always starts
        }
    }


    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000); // 3 seconds
        locationRequest.setFastestInterval(1500); // 1.5 seconds
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    updateUserLocationMarker(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    private void updateUserLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (userMarker == null) {
            userMarker = myMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            userMarker.setPosition(latLng);
        }
    }

    private void getRoute(LatLng origin, LatLng destination) {
        String apiKey = getString(R.string.google_maps_key);
        Log.d("MAPS_DEBUG", "API Key: " + (apiKey.isEmpty() ? "MISSING" : "PRESENT"));
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
//                "&key=" + apiKey;
                "&key=AIzaSyAFFMad-10qvSw8wZl7KgDp0jVafz4La6E";

        Log.d("ROUTE_URL", "Request URL: " + url);

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Route fetch failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    runOnUiThread(() -> drawRoute(responseData));
                }
            }
        });
    }

    private void drawRoute(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);
            JSONArray routes = json.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject overviewPolyline = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline");
                String encodedPoints = overviewPolyline.getString("points");

                List<LatLng> points = decodePolyline(encodedPoints);

                PolylineOptions polylineOptions = new PolylineOptions()
                        .addAll(points)
                        .color(Color.BLUE)
                        .width(10);

                myMap.addPolyline(polylineOptions);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Integer clickCount = (Integer) marker.getTag();
        if (clickCount != null) {
            clickCount++;
            marker.setTag(clickCount);
            Toast.makeText(this, marker.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }
}
