//Results.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

public class Results {
    private long time;
    private String phoneBrand;
    private String networkProvider;
    private String place;
    private NetPerf netPerf;
    private SignalPerf signalPerf;

    public long getTime(){
        return time;
    }
    public String getPhoneBrand(){
        return phoneBrand;
    }
    public String getNetworkProvider() { return networkProvider; }
    public String getPlace(){
        return place;
    }
    public NetPerf getNetPerf(){
        return netPerf;
    }
    public SignalPerf getSignalPerf() { return signalPerf; }

    public Results(){

    }

    public Results(long time, String phoneBrand, String networkProvider, String place, NetPerf netPerf, SignalPerf signalPerf){
        this.time = time;
        this.phoneBrand = phoneBrand;
        this.networkProvider = networkProvider;
        this.place = place;
        this.netPerf = netPerf;
        this.signalPerf = signalPerf;
    }
}
