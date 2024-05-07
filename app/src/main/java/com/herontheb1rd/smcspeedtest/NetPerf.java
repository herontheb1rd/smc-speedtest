//NetPerf.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

public class NetPerf {
    private double dlspeed;
    private double ulspeed;
    private int latency;
    private String url;
    public double getDlspeed() {
        return dlspeed;
    }
    public double getUlspeed() {
        return ulspeed;
    }
    public int getLatency() {
        return latency;
    }
    public String getUrl() {return url; }

    public NetPerf(){

    }

    public NetPerf(double dlspeed, double ulspeed, int latency, String url){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
        this.url = url;
    }
}
