package com.example.weather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MyLocationListener implements LocationListener {
    private static final int PERMISSION_REQUEST_CODE = 0;
    static Location imHere; // здесь будет всегда доступна самая последняя информация о местоположении пользователя.

    public static void SetUpLocationListener(Context context) // это нужно запустить в самом начале работы программы
    {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
            if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
                imHere = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else imHere = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ex) {
            ex.printStackTrace();
        }
    }

    private static void checkEnabled() {

    }

    @Override
    public void onLocationChanged(Location loc) {
        imHere = loc;
    }
    @Override
    public void onProviderDisabled(String provider) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
