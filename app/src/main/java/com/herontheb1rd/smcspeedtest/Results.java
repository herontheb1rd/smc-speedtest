package com.herontheb1rd.smcspeedtest;

//object for firebase
public class Results {
    public double dlspeed;
    public double ulspeed;
    public long latency;
    public long date_time;
    public String location;
    public String network_provider;
    public String phone_brand;

    public Results(){

    }

    public Results(double dlspeed, double ulspeed, long latency, long date_time, String location, String network_provider, String phone_brand){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.date_time = date_time;
        this.location = location;
        this.network_provider = network_provider;
        this.phone_brand = phone_brand;
    }
}
