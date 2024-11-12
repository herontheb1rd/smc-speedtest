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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;


import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.Executors;

public class ResultsFragment extends Fragment {

    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    SharedPreferences prefs = null;

    //private final List<String> qrLocations = Arrays.asList("Library", "Canteen", "Kiosk", "Airport", "ABD", "Garden");

    //codes from https://mcc-mnc.com/
    public final Map<String, String> simOperators = new HashMap<String, String>() {{
        put("51566", "DITO");
        put("51502", "Globe");
        put("51501", "Globe");
        put("51503", "Smart");
    }};

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
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
                    mDatabase.child("scoreboard").child(UID).child("username").setValue(prefs.getString("username", getUID()));
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

    public void displayDownloadResult(double dlspeed){
        View view = getView();

        if(view == null)
            return;


        displayResult(view, R.id.downloadSpeedTV, dlspeed != -1d ? String.format("%.1f", dlspeed) : "N/A");
        view.findViewById(R.id.downloadSpeedTV).setVisibility(View.VISIBLE);
        view.findViewById(R.id.downloadPB).setVisibility(View.INVISIBLE);
    }

    public void displayUploadResult(double ulspeed){
        View view = getView();

        if(view == null)
            return;

        displayResult(view, R.id.uploadSpeedTV, ulspeed != -1d ? String.format("%.1f", ulspeed) : "N/A");
        view.findViewById(R.id.uploadSpeedTV).setVisibility(View.VISIBLE);
        view.findViewById(R.id.uploadPB).setVisibility(View.INVISIBLE);
    }

    public void displayLatencyResult(int latency){
        View view = getView();

        if(view == null)
            return;

        displayResult(view, R.id.latencyTV, latency != -1 ? Integer.toString(latency) : "N/A");
        view.findViewById(R.id.latencyTV).setVisibility(View.VISIBLE);
        view.findViewById(R.id.latencyPB).setVisibility(View.INVISIBLE);
    }

    public void displayProgress(String progress){
        View view = getView();

        if(view == null)
            return;

        displayResult(view, R.id.progressTV, progress);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, bundle) -> {
            String place = bundle.getString("bundleKey");

            ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(8));
            ListenableFuture<NetPerf> netPerfFuture = pool.submit(() -> {
                NetPerf netPerf;

                int MAX_TRIES = 3;
                for(int i = 0; i < MAX_TRIES; i++) {
                    Log.i("test", "attempt " + MAX_TRIES);
                    netPerf = computeNetPerf();

                    if(netPerf != null)
                        return netPerf;
                }

                throw new Exception("Speed test failed");
            });

            Futures.addCallback(netPerfFuture, new FutureCallback<NetPerf>() {
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

                        //findBetterLocation(view, networkProvider, place, netPerf);
                    }else{
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Could not upload results to database",
                                Toast.LENGTH_SHORT).show());
                    }

                    callback.setEnabled(false);
                }

                @Override
                public void onFailure(Throwable t) {
                    displayDownloadResult(-1d);
                    displayUploadResult(-1d);
                    displayLatencyResult(-1);
                    displayProgress("Speed test failed. Please retry");
                }
            }, pool);
        });

        return view;
    }

    private String getUID() {
        return prefs.getString("UID", "");
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
        getActivity().runOnUiThread(() -> ((TextView) view.findViewById(id)).setText(resultStr));
    }

    public void cppLogger(String debugString){
        Log.i("test", debugString);
    }

    public native NetPerf computeNetPerf();

    /*
    * Might reimplement this with more users, but for now it doesn't really do much
    * It might actually hurt getting results, and
    * we don't have enough data for it to be very useful
    * If we had more users, then it might have a usecase
    * Keeping it here anyway
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
                if (betterLocation.equals("None") || betterLocation.contains(currentLocation)) {
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
    }*/
}