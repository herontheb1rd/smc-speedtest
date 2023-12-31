package com.herontheb1rd.smcspeedtest;


public class Results {
    private long time;
    private String phoneBrand;
    private Place place;
    private NetPerf netPerf;

    public long getTime(){
        return time;
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
    public Results(){

    }

    public Results(long time, String phoneBrand, Place place, NetPerf netPerf){
        this.time = time;
        this.phoneBrand = phoneBrand;
        this.place = place;
        this.netPerf = netPerf;
    }
}
