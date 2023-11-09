package com.herontheb1rd.smcspeedtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;


import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
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
                        //TODO: make this code neater and divided up more
                        //run the speedtest
                        String speedtestResults[] = runSpeedtest(preResultTV);

                        //if the test completed successfully, the returned array would be length 4 containing the measured values
                        //if it didn't, the returned array would be length 1 with a fail message
                        if (speedtestResults.length != 1) {
                            //store the resulting strings to the class variables
                            String dlspeedStr = speedtestResults[0];
                            String ulspeedStr = speedtestResults[1];
                            String latencyStr = speedtestResults[2];

                            //convert result strings to double/long
                            double dlspeed = Double.parseDouble(dlspeedStr);
                            double ulspeed = Double.parseDouble(ulspeedStr);
                            int latency = Integer.parseInt(latencyStr);

                            int rssi = 0;
                            int rsrp = 0;
                            int rsrq = 0;
                            TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                                //TODO: handle permissions
                            } else {
                                CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
                                CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    rssi = cellSignalStrengthLte.getRssi();
                                } else {
                                    //uses wifimanager to get rssi
                                    WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                    rssi = wifiManager.getConnectionInfo().getRssi();
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    rsrp = cellSignalStrengthLte.getRsrp();
                                    rsrq = cellSignalStrengthLte.getRsrq();
                                } else {
                                    rsrp = cellSignalStrengthLte.getDbm();
                                    //invalid number, as rsrq has to be negative
                                    //older phones dont have another function for this
                                    //we can just filter this value later
                                }
                            }

                            long time = Calendar.getInstance().getTime().getTime();
                            String locName = bundle.getString("bundleKey");
                            String networkProvider = speedtestResults[3];
                            String phoneBrand = Build.MANUFACTURER;

                            LocationManager lm = (LocationManager)getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                            Location latlng = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            double longitude = latlng.getLongitude();
                            double latitude = latlng.getLatitude();

                            Results results = new Results();
                            results.setTime(time);
                            results.setNetworkProvider(networkProvider);
                            results.setPhoneBrand(phoneBrand);

                            Results.Location location = new Results().new Location();
                            location.setLocName(locName);
                            location.setLatitude(latitude);
                            location.setLongitude(longitude);
                            results.setLocation(location);

                            Results.NetPerf netPerf = new Results().new NetPerf();
                            netPerf.setDlspeed(dlspeed);
                            netPerf.setUlspeed(ulspeed);
                            netPerf.setLatency(latency);
                            netPerf.setRssi(rssi);
                            netPerf.setRsrp(rsrp);
                            netPerf.setRsrq(rsrq);
                            results.setNetPerf(netPerf);

                            mDatabase.child("results").push().setValue(results);

                            //show results
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    preResultTV.setVisibility(View.INVISIBLE);
                                    //yes theres dlspeedStr and latencyStr, but this formats them to 1 decimal place
                                    downloadResultTV.setText(String.format("%.1f", dlspeed));
                                    uploadResultTV.setText(String.format("%.1f", ulspeed));
                                    latencyResultTV.setText(latencyStr);
                                }
                            });
                        } else {
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