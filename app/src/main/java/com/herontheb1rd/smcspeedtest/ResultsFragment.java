//ResultsFragment.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.Navigation;


import android.provider.Settings;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ResultsFragment extends Fragment {

    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    SharedPreferences prefs = null;

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

    private boolean downloadFlag;
    private boolean uploadFlag;
    private boolean latencyFlag;
    private OnBackPressedCallback callback;

    public ResultsFragment(){

    }


    @Override
    public void onStart() {
        super.onStart();

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

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

        //check if user has data in scoreboard
        //if not, set default values
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String UID = getUID();
                if(!snapshot.child("scoreboard").child(UID).exists()){
                    mDatabase.child("scoreboard").child(UID).child("username").setValue(UID);
                    mDatabase.child("scoreboard").child(UID).child("score").setValue(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

         callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(getActivity(), "Test still running.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        downloadFlag = false;
        uploadFlag = false;
        latencyFlag = false;

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        ServerInfo serverInfo = getStoredServerInfo();

        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, bundle) -> {
            String place = bundle.getString("bundleKey");

            ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));

            ListenableFuture<Double> dlspeedFuture = pool.submit(() -> {
                double dlspeed = 20d;
                Thread.sleep(5000);
                //double dlspeed = computeDlspeed(serverInfo);
                displayResult(view, R.id.downloadSpeedTV, String.format("%.1f", dlspeed));
                view.findViewById(R.id.downloadSpeedTV).setVisibility(View.VISIBLE);
                view.findViewById(R.id.downloadPB).setVisibility(View.INVISIBLE);
                downloadFlag = true;
                return dlspeed;
            });

            ListenableFuture<Double> ulspeedFuture = pool.submit(() -> {
                double ulspeed = 20d;
                Thread.sleep(4000);
                //double ulspeed = computeUlspeed(serverInfo);
                displayResult(view, R.id.uploadSpeedTV, String.format("%.1f", ulspeed));
                view.findViewById(R.id.uploadSpeedTV).setVisibility(View.VISIBLE);
                view.findViewById(R.id.uploadPB).setVisibility(View.INVISIBLE);
                uploadFlag = true;
                return ulspeed;
            });

            ListenableFuture<Integer> latencyFuture = pool.submit(() -> {
                int latency = 20;
                Thread.sleep(3000);
                //int latency = computeLatency(serverInfo);
                displayResult(view, R.id.latencyTV, Integer.toString(latency));
                view.findViewById(R.id.latencyTV).setVisibility(View.VISIBLE);
                view.findViewById(R.id.latencyPB).setVisibility(View.INVISIBLE);
                latencyFlag = true;
                return latency;
            });


            //kind of redundant to add waitingDisplayFuture here, but it doesn't hurt
            ListenableFuture<NetPerf> computeNetPerf = Futures.whenAllSucceed(dlspeedFuture, ulspeedFuture, latencyFuture)
                    .call(() -> new NetPerf(Futures.getDone(dlspeedFuture), Futures.getDone(ulspeedFuture),
                            Futures.getDone(latencyFuture)), pool);

            Futures.addCallback(computeNetPerf, new FutureCallback<NetPerf>() {
                @Override
                public void onSuccess(NetPerf netPerf) {

                    long time = Calendar.getInstance().getTime().getTime();
                    String phoneBrand = Build.MANUFACTURER;
                    String networkProvider = getNetworkProvider();
                    String UID = getUID();
                    SignalPerf signalPerf = computeSignalPerf();


                    if(mAuth.getCurrentUser() != null){
                        Results results = new Results(time, phoneBrand, networkProvider, place, netPerf, signalPerf);
                        mDatabase.child("results").child(networkProvider).push().setValue(results);
                        mDatabase.child("scoreboard").child(UID).child("username").setValue(prefs.getString("username", getUID()));


                        mDatabase.child("scoreboard").child(UID).child("score").setValue(ServerValue.increment(1));
                        findBetterLocation(view, networkProvider, place, netPerf);
                    }else{
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Could not upload results to database",
                                Toast.LENGTH_SHORT).show());
                    }

                    callback.setEnabled(false);
                }

                @Override
                public void onFailure(Throwable t) {

                }
            }, pool);


        });

        return view;
    }

    private ServerInfo getStoredServerInfo(){
        return ((MainActivity) getContext()).getServerInfo();
    }

    private void setStoredServerInfo(ServerInfo serverInfo){
        ((MainActivity) getContext()).setServerInfo(serverInfo);
    }

    private String getUID() {
        //UID is the phone's Android ID
        //this removes the need for permissions for READ_PHONE_STATE
        //and is still unique to each phone
        return Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    private double getMeanPerformance(List<NetPerf> resultList){
        //"performance" is dlspeed * ulspeed / latency
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
        String betterLocation = "None";

        double maxValue = netPerf.getDlspeed() * netPerf.getUlspeed() / netPerf.getLatency();

        for(String l: dict.keySet()){
            if(dict.get(l) > maxValue){
                betterLocation = l;
                maxValue = dict.get(l);
            }
        }

        return betterLocation;
    }
    private void findBetterLocation(View view, String networkProvider, String currentLocation, NetPerf netPerf){
        Map<String, List<NetPerf>> locationResultsDict = new HashMap<>();
        Map<String, Double> locationPerformance = new HashMap<>();

        for(String l: qrLocations){
            locationResultsDict.put(l, new ArrayList<>());
            locationPerformance.put(l, 0.0);
        }

        //get results from database
        int hours = 24;
        long timeLimit = Calendar.getInstance().getTime().getTime() - (hours*3600*1000);
        Query resultsQuery = mDatabase.child("results").child(networkProvider).orderByChild("time").startAt(timeLimit);
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
                if (betterLocation.equals("None")) {
                    betterLocation = "This location has (probably) the best connection available";
                }else{
                    betterLocation = "The " + betterLocation + " (probably) has a better connection";
                }

                displayResult(view, R.id.suggestionTV, betterLocation);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
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

            return new SignalPerf(rssi, rsrq, rsrp);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return new SignalPerf(rssi, rsrq, rsrp);
        }
        LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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

    private void displayResult(View view, int id, String resultStr){
        getActivity().runOnUiThread(() -> {
            ((TextView) view.findViewById(id)).setText(resultStr);
        });
    }

    public void cppLogger(String debugString){
        Log.i("test", debugString);
    }

    public native double computeDlspeed(ServerInfo serverInfo);
    public native double computeUlspeed(ServerInfo serverInfo);
    public native int computeLatency(ServerInfo serverInfo);
}