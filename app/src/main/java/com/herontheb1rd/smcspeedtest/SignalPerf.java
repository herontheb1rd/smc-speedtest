package com.herontheb1rd.smcspeedtest;

public class SignalPerf {
    private int rssi;
    private int rsrp;
    private int rsrq;

    public int getRssi(){ return rssi; }
    public int getRsrp(){ return rsrp; }
    public int getRsrq(){ return rsrq; }

    public SignalPerf(int rssi, int rsrp, int rsrq){
        this.rssi = rssi;
        this.rsrp = rsrp;
        this.rsrq = rsrq;
    }
}
