package com.herontheb1rd.smcspeedtest;


public class Results {
    public double dlspeed;
    public double ulspeed;
    public long latency;
    public int rssi;
    public int rsrp;
    public int rsrq;

    public long time;
    public double latitude;
    public double longitude;
    public String location;
    public String network_provider;
    public String phone_brand;

    public Results(){

    }

    public Results(double dlspeed, double ulspeed, long latency, int rssi, int rsrp, int rsrq, long time, String location, String network_provider, String phone_brand){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.rssi = rssi;
        this.rsrp = rsrp;
        this.rsrq = rsrq;
        this.time = time;
        this.location = location;
        this.network_provider = network_provider;
        this.phone_brand = phone_brand;
    }
}
