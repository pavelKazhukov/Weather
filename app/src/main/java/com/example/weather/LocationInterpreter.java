package com.example.weather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationInterpreter {
    public String getCity(double lon, double lat, Context currentContext) {
        Geocoder geocoder = new Geocoder(currentContext, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                return returnedAddress.getLocality();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
