package com.herontheb1rd.smcspeedtest;


public class Results {
    long time;
    String networkProvider;
    String phoneBrand;
    Place place;
    NetPerf netPerf;
    SignalPerf signalPerf;

    public long getTime(){
        return time;
    }

    public void setTime(long time){
        this.time = time;
    }

    public String getNetworkProvider(){
        return networkProvider;
    }

    public void setNetworkProvider(String networkProvider){ this.networkProvider = networkProvider;}

    public String getPhoneBrand(){
        return phoneBrand;
    }

    public void setPhoneBrand(String phoneBrand){
        this.phoneBrand = phoneBrand;
    }

    public Place getPlace(){
        return place;
    }

    public void setPlace(Place place){
        this.place = place;
    }

    public NetPerf getNetPerf(){
        return netPerf;
    }

    public void setNetPerf(NetPerf netPerf) {
        this.netPerf = netPerf;
    }

    public SignalPerf getSignalPerf() { return signalPerf; }
    public void setSignalPerf(SignalPerf signalPerf){ this.signalPerf = signalPerf; }

    public Results(){

    }
}
