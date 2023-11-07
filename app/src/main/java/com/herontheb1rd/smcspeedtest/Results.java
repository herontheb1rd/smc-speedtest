package com.herontheb1rd.smcspeedtest;


public class Results {
    long time;
    String networkProvider;
    String phoneBrand;
    Location location;
    NetPerf netPerf;

    public long getTime(){
        return time;
    }

    public void setTime(long time){
        this.time = time;
    }

    public String getNetworkProvider(){
        return networkProvider;
    }

    public void setNetworkProvider(String networkProvider){
        this.networkProvider = networkProvider;
    }

    public String getPhoneBrand(){
        return phoneBrand;
    }

    public void setPhoneBrand(String phoneBrand){
        this.phoneBrand = phoneBrand;
    }

    public Location getLocation(){
        return location;
    }

    public void setLocation(Location location){
        this.location = location;
    }

    public NetPerf getNetPerf(){
        return netPerf;
    }

    public void setNetPerf(NetPerf netPerf) {
        this.netPerf = netPerf;
    }

    public class Location{
        String locName;
        double latitude;
        double longitude;

        public String getLocName(){
            return locName;
        }

        public void setLocName(String locName){
            this.locName = locName;
        }

        public double getLatitude(){
            return latitude;
        }

        public void setLatitude(double latitude){
            this.latitude = latitude;
        }

        public double getLongitude(){
            return longitude;
        }

        public void setLongitude(double longitude){
            this.longitude = longitude;
        }

        public Location() {

        }
    }

    public class NetPerf{
        public double dlspeed;
        public double ulspeed;
        public int latency;
        public int rssi;
        public int rsrp;
        public int rsrq;

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

        public int getRssi() {
            return rssi;
        }

        public void setRssi(int rssi) {
            this.rssi = rssi;
        }

        public int getRsrp() {
            return rsrp;
        }

        public void setRsrp(int rsrp) {
            this.rsrp = rsrp;
        }

        public int getRsrq() {
            return rsrq;
        }

        public void setRsrq(int rsrq) {
            this.rsrq = rsrq;
        }

        public NetPerf() {

        }
    }

    public Results(){

    }
}
