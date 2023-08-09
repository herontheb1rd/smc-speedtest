package com.herontheb1rd.smcspeedtest;

//object for firebase
public class Results {
    double dlspeed;
    double ulspeed;
    long latency;
    int rssi;
    long date_time;
    String location;
    String network_provider;
    String phone_brand;

    public Results(){

    }

    public Results(double dlspeed, double ulspeed, long latency, int rssi, long date_time, String location, String network_provider, String phone_brand){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.rssi = rssi;
        this.date_time = date_time;
        this.location = location;
        this.network_provider = network_provider;
        this.phone_brand = phone_brand;
    }
}
