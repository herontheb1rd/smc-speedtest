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
                        long time = Calendar.getInstance().getTime().getTime();
                        String networkProvider = "";
                        String phoneBrand = Build.MANUFACTURER;
                        Place place = new Place(bundle.getString("bundleKey") , computeLatLng());
                        NetPerf netPerf = runSpeedtest(preResultTV);
                        SignalPerf signalPerf = new SignalPerf(computeRssi(), computeRsrp(), computeRsrq());

                        Results results = new Results(time, networkProvider, phoneBrand, place, netPerf, signalPerf);
                        mDatabase.child("results").push().setValue(results);

                        //show results
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayResults(netPerf, signalPerf, view);
                            }
                        });
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
            }

        }else{

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

        downloadResultTV.setText(Double.toString(netPerf.getDlspeed()));
        uploadResultTV.setText(Double.toString(netPerf.getUlspeed()));
        latencyResultTV.setText(Integer.toString(netPerf.getLatency()));
        rssiResultTV.setText(Integer.toString(signalPerf.getRssi()));
        rsrpResultTV.setText(Integer.toString(signalPerf.getRsrp()));
        rsrqResultTV.setText(Integer.toString(signalPerf.getRsrq()));
    }


    public native NetPerf runSpeedtest(TextView preResultTV);
}