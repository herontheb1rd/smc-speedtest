package com.herontheb1rd.smcspeedtest;

public class Results {
    public long date_time;
    public String phone_brand;
    public String test_location;
    public String network_provider;
    public double dlspeed;
    public double ulspeed;
    public long latency;
    public int rssi;

    public Results(){

    }

    public Results(long date_time, String phone_brand, String test_location, String network_provider, double dlspeed, double ulspeed, long latency, int rssi){
        this.date_time = date_time;
        this.phone_brand = phone_brand;
        this.test_location = test_location;
        this.network_provider = network_provider;
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.rssi = rssi;
    }
}
