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

                            Results results = new Results();

                            long time = Calendar.getInstance().getTime().getTime();
                            String networkProvider = speedtestResults[3];
                            String phoneBrand = Build.MANUFACTURER;

                            Place place = new Place();
                            place.setLatitude(computeLatLng()[0]);
                            place.setLongitude(computeLatLng()[1]);
                            place.setPlaceName(speedtestResults[3]);

                            NetPerf netPerf = new NetPerf();
                            netPerf.setDlspeed(Double.parseDouble(speedtestResults[0]));
                            netPerf.setUlspeed(Double.parseDouble(speedtestResults[1]));
                            netPerf.setLatency(Integer.parseInt(speedtestResults[2]));

                            SignalPerf signalPerf = new SignalPerf();
                            signalPerf.setRssi(computeRssi());
                            signalPerf.setRsrp(computeRsrp());
                            signalPerf.setRsrq(computeRsrq());

                            results.setTime(time);
                            results.setNetworkProvider(networkProvider);
                            results.setPhoneBrand(phoneBrand);
                            results.setPlace(place);
                            results.setNetPerf(netPerf);
                            results.setSignalPerf(signalPerf);

                            mDatabase.child("results").push().setValue(results);

                            //show results
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    displayResults(netPerf, signalPerf, view);
                                }
                            });
                        } else {
                            Toast toast = Toast.makeText(getActivity(), speedtestResults[0], Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }).start();
            }
        });

        return view;
    }

    public double[] computeLatLng() {
        double[] latlng;
        double latitude = 0;
        double longitude = 0;

        LocationManager lm = (LocationManager) getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            latitude =  loc.getLatitude();
            longitude = loc.getLongitude();
        }

        latlng = new double[]{latitude, longitude};
        return latlng;
    }

    public int computeRssi() {
        int rssi = 0;

        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rssi = cellSignalStrengthLte.getRssi();
            } else {
                //uses wifimanager to get rssi
                WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                rssi = wifiManager.getConnectionInfo().getRssi();
            }
        }else{
            //TODO: Handle permissions
        }

        return rssi;
    }

    public int computeRsrp(){
        int rsrp = 0;
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                rsrp = cellSignalStrengthLte.getRsrp();
            } else {
                rsrp = cellSignalStrengthLte.getDbm();
            }

        }else{
            //TODO: Handle permissions
        }

        return rsrp;
    }

    public int computeRsrq(){
        int rsrq = 0;
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                rsrq = cellSignalStrengthLte.getRsrp();
            }else{
                //TODO:
            }

        }else{
            //TODO: Handle permissions
        }

        return rsrq;
    }

    public void displayResults(NetPerf netPerf, SignalPerf signalPerf, View view){
        TextView downloadResultTV = (TextView) view.findViewById(R.id.downloadResultTV);
        TextView uploadResultTV = (TextView) view.findViewById(R.id.uploadResultTV);
        TextView latencyResultTV = (TextView) view.findViewById(R.id.latencyResultTV);
        TextView rssiResultTV = (TextView) view.findViewById(R.id.rssiResultTV);
        TextView rsrpResultTV = (TextView) view.findViewById(R.id.rssiResultTV);
        TextView rsrqResultTV = (TextView) view.findViewById(R.id.rssiResultTV);
    }


    public native String[] runSpeedtest(TextView preResultTV);
}