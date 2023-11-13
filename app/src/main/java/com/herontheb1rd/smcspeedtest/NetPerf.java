package com.herontheb1rd.smcspeedtest;

public class NetPerf {
    public double dlspeed;
    public double ulspeed;
    public int latency;

    public double getDlspeed() {
        return dlspeed;
    }

    public void setDlspeed(double dlspeed) {
        this.dlspeed = dlspeed;
    }

    public double getUlspeed() {
        return ulspeed;
    }

    public void setUlspeed(double ulspeed) {
        this.ulspeed = ulspeed;
    }

    public int getLatency() {
        return latency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }

}
