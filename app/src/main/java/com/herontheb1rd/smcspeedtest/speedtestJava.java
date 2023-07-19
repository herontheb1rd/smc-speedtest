package com.herontheb1rd.smcspeedtest;

import android.os.Build;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class speedtestJava {
    //information that will be added to the db but not shown to the user
    private long date_time;
    private String phone_brand;
    private String test_location;
    private String network_provider;
    private int rssi;

    //information that will be shown to the user
    //store them in runTestAddResults() and resultsFragment just uses an object to run function and access variables
    public double dlspeed;
    public double ulspeed;
    public long latency;

    private DatabaseReference mDatabase;

    public speedtestJava() {
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
                ).getReference();
    }

    public void runTestAddResults(String initRawLocation){
        //run speedtest from JNI first
        runSpeedtest();

        //store results from speedtest
        //lol this reads weird but the first instance of getTime() returns a Date and the second converts it to long
        date_time = Calendar.getInstance().getTime().getTime();
        phone_brand = Build.MANUFACTURER;
        test_location = initRawLocation;
        network_provider = getNetworkProvider();
        dlspeed = getDLSpeed();
        ulspeed = getULSpeed();
        latency = getLatency();
        rssi = getRssi();

        //firebase code
        Results results = new Results(date_time, phone_brand, test_location, network_provider, dlspeed, ulspeed, latency, rssi);
        mDatabase.child("results").push().setValue(results);
    }


    private int getRssi(){
        //TODO: write function that gets the RSSI
        int rssi = 10;

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
