package com.herontheb1rd.smcspeedtest;

import android.os.Build;
import android.util.Log;

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

    public speedtestJava(String initRawLocation) {
        //test location is passed from qr code scan
        //TODO: write function that edits raw value
        //after doing fancy intent things with qr code
        //or not, if you change your mind
        test_location = initRawLocation;
    }

    public void getResults(){
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


    private int getRssi(){
        int rssi;

        //TODO: write function that gets the RSSI
        rssi = 10;

        return rssi;
    }

    private native void runSpeedtest();
    //its easier to just have separate functions for each than to mess with an object array
    //and i can check beforehand if the values are empty
    //plus its neater to look at
    private native double getDLSpeed();
    private native double getULSpeed();
    private native long getLatency();
    private native String getNetworkProvider();
}
