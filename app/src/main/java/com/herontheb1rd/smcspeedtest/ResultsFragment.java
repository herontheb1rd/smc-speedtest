//ResultsFragment.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
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
import androidx.navigation.Navigation;


import android.provider.Telephony;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResultsFragment extends Fragment {

    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    private final List<String> qrLocations = Arrays.asList("Library", "Canteen", "Kiosk", "Airport", "ABD", "Garden");


    //codes from https://mcc-mnc.com/
    public final Map<String, String> simOperators = new HashMap<String, String>() {{
        put("51566", "DITO");
        put("51502", "Globe");
        put("51501", "Globe");
        put("51503", "Smart");
    }};

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;


    public ResultsFragment() {

    }

    @Override
    public void onStart() {
        super.onStart();

        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously()
                .addOnCompleteListener(getActivity(), task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Firebase authentication failed. Can't upload results",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String place = bundle.getString("bundleKey");

                Executor executor = Executors.newSingleThreadExecutor();
                ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

                ListenableFuture<Long> serverInfoFuture = pool.submit(() -> {
                    long serverInfo = 10;
                    //long serverInfo = getServerInfo();
                    updateProgress("Server info acquired", 10);
                    return serverInfo;
                });

                AsyncFunction<Long, NetPerf> asyncNetPerf = serverPtr -> {
                    ListenableFuture<Double> dlspeedFuture = pool.submit(() -> {
                        double dlspeed = 10;
                        //double dlspeed = computeDlspeed(serverPtr);
                        updateProgress("Download speed computed", 30);
                        displayResult(R.id.downloadResultTV, String.format("%.1f", dlspeed));
                        return dlspeed;
                    });

                    ListenableFuture<Double> ulspeedFuture = pool.submit(() -> {
                        double ulspeed = 10;
                        //double ulspeed = computeUlspeed(serverPtr);
                        updateProgress("Upload speed computed", 30);
                        displayResult(R.id.uploadResultTV, String.format("%.1f", ulspeed));
                        return ulspeed;
                    });

                    ListenableFuture<Integer> latencyFuture = pool.submit(() -> {
                        int latency = 70;
                        //int latency = computeLatency(serverPtr);
                        updateProgress("Latency computed", 20);
                        displayResult(R.id.latencyResultTV, Integer.toString(latency));
                        return latency;
                    });

                    ListenableFuture<NetPerf> computeNetPerf = Futures.whenAllSucceed(dlspeedFuture, ulspeedFuture, latencyFuture)
                            .call(() -> {
                                NetPerf netPerf = new NetPerf(Futures.getDone(dlspeedFuture), Futures.getDone(ulspeedFuture),
                                        Futures.getDone(latencyFuture));
                                //freeServerPtr(serverPtr);
                                return netPerf;
                            }, pool);
                    return computeNetPerf;
                };

                ListenableFuture<NetPerf> netPerfFuture = Futures.transformAsync(serverInfoFuture, asyncNetPerf, pool);

                Futures.addCallback(netPerfFuture, new FutureCallback<NetPerf>() {
                    @Override
                    public void onSuccess(NetPerf netPerf) {
                        long time = Calendar.getInstance().getTime().getTime();
                        String phoneBrand = Build.MANUFACTURER;
                        String networkProvider = "DITO";
                        //String networkProvider = getNetworkProvider();
                        SignalPerf signalPerf = new SignalPerf(10, 10, 10);
                        //SignalPerf signalPerf = computeSignalPerf();

                        updateProgress("Test complete", 10);

                        Results results = new Results(time, phoneBrand, networkProvider, place, netPerf, signalPerf);
                        mDatabase.child("results").child(networkProvider).push().setValue(results);

                        findBetterLocation(networkProvider, place, netPerf);
                        showResults();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Toast.makeText(getActivity(), "Test failed to run properly. Please try again",
                                Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigate(R.id.action_resultsFragment_to_runTestFragment);
                    }
                }, pool);
            }
        });

        return view;
    }

    private double getMeanPerformance(List<NetPerf> resultList){
        //"performance" is dlspeed + ulspeed - latency
        //there might be better methods but this is a lazy way of implementing it i suppose
        //because out goal is higher dlspeed and ulspeed but lower latency

        int resultsSize = resultList.size();

        if(resultsSize == 0){
            return 0.0;
        }

        double dlspeedSum = 0;
        double ulspeedSum = 0;
        double latencySum = 0;
        for(NetPerf n: resultList){
            dlspeedSum += n.getDlspeed();
            ulspeedSum += n.getUlspeed();
            latencySum += n.getLatency();
        }

        double meanPerformance = dlspeedSum/resultsSize * ulspeedSum/resultsSize / latencySum/resultsSize;
        return meanPerformance;
    }

    private String compareLocations(Map<String, Double> dict, NetPerf netPerf){
        String betterLocation = "Nowhere else!";

        double maxValue = netPerf.getDlspeed() * netPerf.getUlspeed() / netPerf.getLatency();

        for(String l: dict.keySet()){
            Log.i("test", l);
            Log.i("test", Double.toString(dict.get(l)));
            if(dict.get(l) > maxValue){
                betterLocation = l;
                maxValue = dict.get(l);
            }
        }

        return betterLocation;
    }
    private void findBetterLocation(String networkProvider, String currentLocation, NetPerf netPerf){
        Map<String, List<NetPerf>> locationResultsDict = new HashMap<>();
        Map<String, Double> locationPerformance = new HashMap<>();

        for(String l: qrLocations){
            locationResultsDict.put(l, new ArrayList<>());
            locationPerformance.put(l, new Double(0.0));
        }


        //get results from database
        long twoHoursAgo = Calendar.getInstance().getTime().getTime() - (2*3600*1000);
        Query resultsQuery = mDatabase.child("results").child(networkProvider).orderByChild("time").startAt(twoHoursAgo);
        resultsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot networkSnapshot : dataSnapshot.getChildren()) {
                    Results curResult = networkSnapshot.getValue(Results.class);

                    locationResultsDict.get(curResult.getPlace()).add(curResult.getNetPerf());
                }

                for (String l : locationResultsDict.keySet()) {
                    locationPerformance.put(l, getMeanPerformance(locationResultsDict.get(l)));
                }

                String betterLocation = compareLocations(locationPerformance, netPerf);
                if (betterLocation.equals(currentLocation)) {
                    betterLocation = "Nowhere else!";
                }

                TextView betterLocationTV = (TextView) getView().findViewById(R.id.betterLocationTV);
                betterLocationTV.setText(betterLocation);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void updateProgress(String progressText, int progressIncrement) {
        TextView progressTV = (TextView) getView().findViewById(R.id.progressTV);
        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);

        progressTV.post(() -> progressTV.setText(progressText));
        progressBar.incrementProgressBy(progressIncrement);
    }

    private void showResults(){
       Group progressGroup = (Group) getView().findViewById(R.id.progressGroup);
       Group resultsGroup = (Group) getView().findViewById(R.id.resultsGroup);
       getActivity().runOnUiThread(() -> {
           progressGroup.setVisibility(View.INVISIBLE);
           resultsGroup.setVisibility(View.VISIBLE);
       });
    }

    public String getNetworkProvider() {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);
            return simOperators.get(dataTM.getSimOperator());
        }else {
            return simOperators.get(tm.getSimOperator());
        }
    }

    private SignalPerf computeSignalPerf(){
        int rssi = 1;
        int rsrp = 1;
        int rsrq = 1;

        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), "Location access not granted. Skipping signal data collection",
                    Toast.LENGTH_SHORT).show();

            return new SignalPerf(rssi, rsrq, rsrp);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Toast.makeText(getActivity(), "Phone model too old to retrieve signal info",
                    Toast.LENGTH_SHORT).show();

            return new SignalPerf(rssi, rsrq, rsrp);
        }

        LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getActivity(), "Location turned off. Signal data cannot be retrieved",
                    Toast.LENGTH_SHORT).show();

            return new SignalPerf(rssi, rsrq, rsrp);
        }

        int dataSubId = SubscriptionManager.getActiveDataSubscriptionId();
        TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);

        CellInfoLte cellinfolte = (CellInfoLte) dataTM.getAllCellInfo().get(0);
        CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();

        rssi = cellSignalStrengthLte.getRssi();
        rsrp = cellSignalStrengthLte.getRsrp();
        rsrq = cellSignalStrengthLte.getRsrq();

        return new SignalPerf(rssi, rsrq, rsrp);
    }

    private void displayResult(int id, String resultStr){
        ((TextView) getView().findViewById(id)).setText(resultStr);
    }

    public native long getServerInfo();
    public native double computeDlspeed(long serverPtr);
    public native double computeUlspeed(long serverPtr);
    public native int computeLatency(long serverPtr);
    public native void freeServerPtr(long serverPtr);
}