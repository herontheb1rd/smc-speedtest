package com.herontheb1rd.smcspeedtest;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.Calendar;

public class ResultsFragment extends Fragment {

    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    private static final String ARG_DLSPEED = "DLSPEED";
    private static final String ARG_ULSPEED = "ULSPEED";
    private static final String ARG_LATENCY = "LATENCY";

    private String mDlspeedStr;
    private String mUlspeedStr;
    private String mLatencyStr;
    private DatabaseReference mDatabase;

    private boolean hasTestRan;
    public ResultsFragment() {
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();    }

    public static ResultsFragment newInstance(String mDlspeedStr, String mUlspeedStr, String mLatencyStr) {
        ResultsFragment fragment = new ResultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DLSPEED, mDlspeedStr);
        args.putString(ARG_ULSPEED, mUlspeedStr);
        args.putString(ARG_LATENCY, mLatencyStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDlspeedStr = getArguments().getString(ARG_DLSPEED);
            mUlspeedStr = getArguments().getString(ARG_ULSPEED);
            mLatencyStr = getArguments().getString(ARG_LATENCY);
            hasTestRan = true;
        }else{
            hasTestRan = false;
        }
    }

    //allows the results to persist when going between fragments
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString(ARG_DLSPEED, mDlspeedStr);
        outState.putString(ARG_ULSPEED, mUlspeedStr);
        outState.putString(ARG_LATENCY, mLatencyStr);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        TextView preTestTV = (TextView) view.findViewById(R.id.preTestTV);
        TextView preResultTV = (TextView) view.findViewById(R.id.preResultTV);

        if(hasTestRan){
            preTestTV.setVisibility(View.INVISIBLE);
            showResults(view);
        }else{
            preTestTV.setVisibility(View.VISIBLE);
        }

        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                //running this in a separate thread as to not block main thread
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                //make preResultTV visible
                                preResultTV.setVisibility(View.VISIBLE);

                                //run the speedtest
                                String speedtestResults[] = runSpeedtest();

                                //if the test completed successfully, the returned array would be length 4 containing the measured values
                                //if it didnt, the returned array would be length 1 with a fail message
                                //this prevents segmentation faults/crashes/errors
                                if(speedtestResults.length != 1){
                                    //store the resulting strings to the class variables
                                    mDlspeedStr = speedtestResults[0];
                                    mUlspeedStr = speedtestResults[1];
                                    mLatencyStr = speedtestResults[2];

                                    //show results
                                    showResults(view);

                                    //convert result strings to double/long
                                    double dlspeed = Double.parseDouble(mDlspeedStr);
                                    double ulspeed = Double.parseDouble(mUlspeedStr);
                                    long latency = Long.parseLong(mLatencyStr);

                                    //get the other information: rssi, date/time, location, network_provider, and the phone brand
                                    int rssi = getRssi();
                                    long date_time = Calendar.getInstance().getTime().getTime(); //yes this looks weird
                                    String location = bundle.getString("requestKey");
                                    String network_provider = speedtestResults[3];
                                    String phone_brand = Build.MANUFACTURER;

                                    //upload results to firebase
                                    Results results = new Results(dlspeed, ulspeed, latency, rssi, date_time, location, network_provider, phone_brand);
                                    mDatabase.child("results").push().setValue(results);
                                }else{
                                    //if failed, show toast and revert text to preTestTV
                                    preTestTV.setVisibility(View.VISIBLE);
                                    preResultTV.setVisibility(View.INVISIBLE);

                                    //message is embedded in the returned array, which makes this easier
                                    //also is helpful for debugging
                                    CharSequence message = speedtestResults[0];
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(getActivity(), message, duration);
                                    toast.show();
                                }
                            }
                        }
                ).start();
            }
        });

        return view;
    }


    //shows the results group, sets the text of the results, and hides the preResultText
    private void showResults(View view){
        TextView preResultTV = (TextView) view.findViewById(R.id.preResultTV);
        Group resultsGroup = (Group) view.findViewById(R.id.resultsGroup);
        TextView downloadResultTV = (TextView) view.findViewById(R.id.downloadResultTV);
        TextView uploadResultTV = (TextView) view.findViewById(R.id.uploadResultTV);
        TextView latencyResultTV = (TextView) view.findViewById(R.id.latencyResultTV);

        preResultTV.setVisibility(View.INVISIBLE);
        resultsGroup.setVisibility(View.VISIBLE);
        downloadResultTV.setText(mDlspeedStr);
        uploadResultTV.setText(mUlspeedStr);
        latencyResultTV.setText(mLatencyStr);
    }


    //gets the rssi
    private int getRssi(){
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();
        int rssi = info.getRssi();

        return rssi;
    }

    public native String[] runSpeedtest();
}