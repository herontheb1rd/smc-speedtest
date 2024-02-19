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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ResultsFragment extends Fragment {

    // Used to load the 'smcspeedtest' library
    static {
        System.loadLibrary("smcspeedtest");
    }

    private final Map<String, double[]> qrLocations = new HashMap<String, double[]>() {{
        put("Library", new double[]{10.0, 10.0});
        put("Canteen", new double[]{10.0, 10.0});
        put("Kiosk", new double[]{10.0, 10.0});
        put("Airport", new double[]{10.0, 10.0});
        put("ABD", new double[]{10.0, 10.0});
        put("Garden", new double[]{10.0, 10.0});
    }};

    //codes from https://mcc-mnc.com/
    private final Map<String, String> simOperators = new HashMap<String, String>() {{
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
                String placeName = bundle.getString("bundleKey");

                Executor listeningExecutor = Executors.newSingleThreadExecutor();
                ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

                ListenableFuture<Long> serverInfoFuture = pool.submit(() -> getServerInfo());
                AsyncFunction<Long, NetPerf> asyncNetPerf = serverPtr -> {
                    ListenableFuture<Double> dlspeedFuture = pool.submit(() -> {
                        double dlspeed = computeDlspeed(serverPtr);
                        updateProgress("Download speed computed", 30);
                        displayResult(R.id.downloadResultTV, String.format("%.1f", dlspeed));
                        return dlspeed;
                    });

                    ListenableFuture<Double> ulspeedFuture = pool.submit(() -> {
                        double ulspeed = computeUlspeed(serverPtr);
                        updateProgress("Upload speed computed", 30);
                        displayResult(R.id.uploadResultTV, String.format("%.1f", ulspeed));
                        return ulspeed;
                    });

                    ListenableFuture<Integer> latencyFuture = pool.submit(() -> {
                        int latency = computeLatency(serverPtr);
                        updateProgress("Latency computed", 20);
                        displayResult(R.id.latencyResultTV, Integer.toString(latency));
                        return latency;
                    });

                    ListenableFuture<NetPerf> computeNetPerf = Futures.whenAllSucceed(dlspeedFuture, ulspeedFuture, latencyFuture)
                            .call(() -> {
                                NetPerf netPerf = new NetPerf(Futures.getDone(dlspeedFuture), Futures.getDone(ulspeedFuture),
                                    Futures.getDone(latencyFuture));
                                freeServerPtr(serverPtr);
                                return netPerf;
                            }, listeningExecutor);
                    return computeNetPerf;
                };

                ListenableFuture<NetPerf> netPerfFuture = Futures.transformAsync(serverInfoFuture, asyncNetPerf, listeningExecutor);

                Futures.addCallback(netPerfFuture, new FutureCallback<NetPerf>() {
                        @Override
                        public void onSuccess(NetPerf netPerf) {
                            long time = Calendar.getInstance().getTime().getTime();
                            String phoneBrand = Build.MANUFACTURER;
                            String networkProvider = getNetworkProvider();
                            Place place = getPlace(placeName);
                            SignalPerf signalPerf = computeSignalPerf();

                            updateProgress("Test complete", 10);

                            Results results = new Results(time, phoneBrand, networkProvider, place, netPerf, signalPerf);
                            mDatabase.child("results").push().setValue(results);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Toast.makeText(getActivity(), "Test failed to run properly. Please try again",
                                    Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(getView()).navigate(R.id.action_resultsFragment_to_runTestFragment);
                        }
                }, listeningExecutor);


            }
        });

        return view;
    }

    private void updateProgress(String progressText, int progressIncrement) {
        TextView progressTV = (TextView) getView().findViewById(R.id.progressTV);
        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);

        progressTV.post(() -> progressTV.setText(progressText));
        progressBar.incrementProgressBy(progressIncrement);
    }

    private Place getPlace(String placeName) {
        double latitude = qrLocations.get(placeName)[0];
        double longitude = qrLocations.get(placeName)[1];

        return new Place(placeName, latitude, longitude);
    }

    private String getNetworkProvider() {
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
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);

                CellInfoLte cellinfolte = (CellInfoLte) dataTM.getAllCellInfo().get(0);
                CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();

                rssi = cellSignalStrengthLte.getRssi();
                displayResult(R.id.rssiResultTV, Integer.toString(rssi));

                rsrp = cellSignalStrengthLte.getRsrp();
                displayResult(R.id.rsrpResultTV, Integer.toString(rsrp));

                rsrq = cellSignalStrengthLte.getRsrq();
                displayResult(R.id.rsrqResultTV, Integer.toString(rsrq));
            }else{
                Toast.makeText(getActivity(), "Phone model too old to retrieve signal info",
                        Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(getActivity(), "Location access not granted. Skipping signal data collection",
                    Toast.LENGTH_SHORT).show();
        }
        return new SignalPerf(rssi, rsrq, rsrp);
    }

    private void displayResult(int id, String resultStr){
        ((TextView) getView().findViewById(id)).setText(resultStr);
    }

    private native long getServerInfo();
    private native double computeDlspeed(long serverPtr);
    private native double computeUlspeed(long serverPtr);
    private native int computeLatency(long serverPtr);
    private native void freeServerPtr(long serverPtr);
}