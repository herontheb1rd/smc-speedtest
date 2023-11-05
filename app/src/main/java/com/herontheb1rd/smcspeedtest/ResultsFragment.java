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


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.Calendar;

//runs the test, and uploads the results
public class ResultsFragment extends Fragment {


    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    private DatabaseReference mDatabase;
    public ResultsFragment() {
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        TextView preResultTV = (TextView) view.findViewById(R.id.preResultTV);

        Group resultsGroup = (Group) view.findViewById(R.id.resultsGroup);
        TextView downloadResultTV = (TextView) view.findViewById(R.id.downloadResultTV);
        TextView uploadResultTV = (TextView) view.findViewById(R.id.uploadResultTV);
        TextView latencyResultTV = (TextView) view.findViewById(R.id.latencyResultTV);

        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                //running this in a separate thread because this will block main thread and cause app to crash
                new Thread(new Runnable() {
                @Override
                public void run() {
                    //run the speedtest
                    String speedtestResults[] = runSpeedtest(preResultTV);

                    //if the test completed successfully, the returned array would be length 4 containing the measured values
                    //if it didn't, the returned array would be length 1 with a fail message
                    if(speedtestResults.length != 1){
                        //store the resulting strings to the class variables
                        String dlspeedStr = speedtestResults[0];
                        String ulspeedStr = speedtestResults[1];
                        String latencyStr = speedtestResults[2];

                        //convert result strings to double/long
                        double dlspeed = Double.parseDouble(dlspeedStr);
                        double ulspeed = Double.parseDouble(ulspeedStr);
                        long latency = Long.parseLong(latencyStr);

                        //get the other information: rssi, time, location, network_provider, and the phone brand
                        long time = Calendar.getInstance().getTime().getTime();
                        String location = bundle.getString("bundleKey");
                        String network_provider = speedtestResults[3];
                        String phone_brand = Build.MANUFACTURER;

                        //upload results to firebase
                        //Results results = new Results(dlspeed, ulspeed, latency, time, location, network_provider, phone_brand);
                        //mDatabase.child("results").push().setValue(results);

                        //show results
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                preResultTV.setVisibility(View.INVISIBLE);
                                resultsGroup.setVisibility(View.VISIBLE);
                                //yes theres dlspeedStr and latencyStr, but this formats them to 1 decimal place
                                downloadResultTV.setText(String.format("%.1f", dlspeed));
                                uploadResultTV.setText(String.format("%.1f", ulspeed));
                                latencyResultTV.setText(latencyStr);
                            }
                        });
                    }else{
                        preResultTV.setText(speedtestResults[0] + ", please try again");

                        //message is embedded in the returned array, which makes this easier
                        //also is helpful for debugging
                        Toast toast = Toast.makeText(getActivity(), speedtestResults[0], Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                }).start();
            }
        });

        return view;
    }


    public native String[] runSpeedtest(TextView preResultTV);
}