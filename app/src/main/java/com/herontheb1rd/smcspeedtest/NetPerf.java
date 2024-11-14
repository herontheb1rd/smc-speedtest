//NetPerf.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

public class NetPerf {
    private double dlspeed;
    private double ulspeed;
    private int latency;
    public double getDlspeed() {
        return dlspeed;
    }
    public double getUlspeed() {
        return ulspeed;
    }
    public int getLatency() {
        return latency;
    }

    public void setDlspeed(double dlspeed){ this.dlspeed = dlspeed;}
    public void setUlspeed(double ulspeed){ this.ulspeed = ulspeed;}
    public void setLatency(int latency){this.latency = latency;}
    public NetPerf(){

    }

    public NetPerf(double dlspeed, double ulspeed, int latency){
        this.dlspeed = dlspeed;
        this.ulspeed = ulspeed;
        this.latency = latency;
    }
}
