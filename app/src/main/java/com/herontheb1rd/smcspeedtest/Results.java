package com.herontheb1rd.smcspeedtest;


public class Results {
    private long time;
    private String networkProvider;
    private String phoneBrand;
    private Place place;
    private NetPerf netPerf;
    private SignalPerf signalPerf;

    public long getTime(){
        return time;
    }
    public String getNetworkProvider(){
        return networkProvider;
    }
    public String getPhoneBrand(){
        return phoneBrand;
    }
    public Place getPlace(){
        return place;
    }
    public NetPerf getNetPerf(){
        return netPerf;
    }
    public SignalPerf getSignalPerf() { return signalPerf; }

    public Results(){

    }

    public Results(long time, String networkProvider, String phoneBrand, Place place, NetPerf netPerf, SignalPerf signalPerf){
        this.time = time;
        this.networkProvider = networkProvider;
        this.phoneBrand = phoneBrand;
        this.place = place;
        this.netPerf = netPerf;
        this.signalPerf = signalPerf;
    }
}
