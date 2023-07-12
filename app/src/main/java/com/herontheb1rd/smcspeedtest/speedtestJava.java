package com.herontheb1rd.smcspeedtest;

public class speedtestJava {
    //information that will be added to the db but not shown to the user
    private String phone_model;
    private String test_location;
    private String network_provider;
    private int rssi;

    //information that will be shown to the user
    double dlspeed;
    double ulspeed;
    long latency;


    public speedtestJava(String rawLocationValue) {
        //test location is passed from qr code scan
        //edit after doing fancy intent things with qr code
        //or not, if you change your mind
        test_location = rawLocationValue;
    }

    /**
     * Runs taganaka's SpeedTest code through JNI
     */
    public void runSpeedtest(){

    }

    /**
     * Gets phone brand
     */
    public void getPhoneInfo(){

    }

    /**
     * Get current RSSI (received signal strength indicator) in dbm
     */
    public void getRSSI(){

    }

    /**
     * Add results to MySQL db
     * Uses Volley
     */
    public void addResults(){

    }
}
