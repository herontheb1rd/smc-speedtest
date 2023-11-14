package com.herontheb1rd.smcspeedtest;

public class Place {
    private String placeName;
    private double latitude;
    private double longitude;

    public String getPlaceName(){
        return placeName;
    }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public Place(){

    }

    public Place(String placeName, double[] latlng) {
        this.placeName = placeName;
        this.latitude = latlng[0];
        this.longitude = latlng[1];
    }
}
