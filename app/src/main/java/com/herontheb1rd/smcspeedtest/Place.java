package com.herontheb1rd.smcspeedtest;

public class Place {
    String placeName;
    double latitude;
    double longitude;

    public String getPlaceName(){
        return placeName;
    }

    public void setPlaceName(String locName){
        this.placeName = locName;
    }

    public double getLatitude(){
        return latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public Place() {

    }
}
