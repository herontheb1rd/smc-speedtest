package com.herontheb1rd.smcspeedtest;

import android.os.Build;

public class speedtestJava {
    //information that will be added to the db but not shown to the user
    private String phone_brand;
    private String test_location;
    private String network_provider;
    private int rssi;

    //information that will be shown to the user
    double dlspeed;
    double ulspeed;
    long latency;

    public speedtestJava(String rawLocationValue) {
        //test location is passed from qr code scan
        //TODO: write function that edits raw value
        //after doing fancy intent things with qr code
        //or not, if you change your mind
        test_location = rawLocationValue;
    }

    public void storeResults(){
        //run speedtest from JNI first
        runSpeedtest();

        //store results from speedtest
        dlspeed = getDLSpeed();
        ulspeed = getULSpeed();
        latency = getLatency();
        network_provider = getNetworkProvider();

        //no need for a function since its just one line
        phone_brand = Build.MANUFACTURER;

        //get RSSI from function
        rssi = getRssi();
    }


    public int getRssi(){
        int rssi;

        //TODO: write function that gets the RSSI
        rssi = 10;

        return rssi;
    }

    public native void runSpeedtest();
    //its easier to just have separate functions for each than to mess with an object array
    //and i can check beforehand if the values are empty
    //plus its neater to look at
    public native double getDLSpeed();
    public native double getULSpeed();
    public native long getLatency();
    public native String getNetworkProvider();
}
