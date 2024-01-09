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

    private final Map<String, double[]> qrLocations = new HashMap<>();

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;


    public ResultsFragment() {
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
        mAuth = FirebaseAuth.getInstance();

        //TODO: put default locations
        qrLocations.put("Library", new double[]{10.0, 10.0});
    }

    @Override
    public void onStart() {
        super.onStart();

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
                            //mDatabase.child("results").push().setValue(results);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            //TODO: Write failure handling code
                        }
                }, listeningExecutor);
            }
        });

        return view;
    }

    public void updateProgress(String progressText, int progressIncrement){
        TextView progressTV = (TextView) getView().findViewById(R.id.progressTV);
        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.progressBar);

        progressTV.post(() -> progressTV.setText(progressText));
        progressBar.incrementProgressBy(progressIncrement);
    }

    public Place getPlace(String placeName){
        double latitude = qrLocations.get(placeName)[0];
        double longitude = qrLocations.get(placeName)[1];

        return new Place(placeName, latitude, longitude);
    }

    public String getNetworkProvider(){
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getNetworkOperatorName();
    }

    public SignalPerf computeSignalPerf(){
        int rssi = 1;
        int rsrp = 1;
        int rsrq = 1;
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rssi = cellSignalStrengthLte.getRssi();
                rsrp = cellSignalStrengthLte.getRsrp();
                rsrq = cellSignalStrengthLte.getRsrq();
            }
        }

        return new SignalPerf(rssi, rsrq, rsrp);
    }

    public void displayResult(int id, String resultStr){
        ((TextView) getView().findViewById(id)).setText(resultStr);
    }

    public native long getServerInfo();
    public native double computeDlspeed(long serverPtr);
    public native double computeUlspeed(long serverPtr);
    public native int computeLatency(long serverPtr);
    public native void freeServerPtr(long serverPtr);

}