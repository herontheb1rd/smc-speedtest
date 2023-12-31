package com.herontheb1rd.smcspeedtest;

public class NetPerf {
    private double dlspeed;
    private double ulspeed;
    private int latency;
    private int rssi;
    public double getDlspeed() {
        return dlspeed;
    }
    public double getUlspeed() {
        return ulspeed;
    }
    public int getLatency() {
        return latency;
    }
    public int getRssi(){ return rssi; }
    public NetPerf(){

    }

    public NetPerf(double dlspeed, double ulspeed, int latency, int rssi){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.rssi = rssi;
    }
}
