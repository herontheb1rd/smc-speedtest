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
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            Toast.makeText(getActivity(), "Firebase authentication failed. Can't upload results",
                                    Toast.LENGTH_SHORT).show();
                        }
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
                    ListenableFuture<Double> dlspeedFuture = pool.submit(() -> computeDlspeed(serverPtr));
                    ListenableFuture<Double> ulspeedFuture = pool.submit(() -> computeUlspeed(serverPtr));
                    ListenableFuture<Integer> latencyFuture = pool.submit(() -> computeLatency(serverPtr));

                    ListenableFuture<NetPerf> computeNetPerf = Futures.whenAllSucceed(dlspeedFuture, ulspeedFuture, latencyFuture)
                            .call(() -> new NetPerf(Futures.getDone(dlspeedFuture), Futures.getDone(ulspeedFuture),
                                    Futures.getDone(latencyFuture)), listeningExecutor);

                    return computeNetPerf;
                };

                ListenableFuture<NetPerf> netPerfFuture = Futures.transformAsync(serverInfoFuture, asyncNetPerf, listeningExecutor);

                Futures.addCallback(netPerfFuture, new FutureCallback<NetPerf>() {
                        @Override
                        public void onSuccess(NetPerf netPerf) {
                            Place place = computePlace(placeName);
                            SignalPerf signalPerf = computeSignalPerf();

                            long time = Calendar.getInstance().getTime().getTime();
                            String networkProvider = getNetworkProvider();
                            String phoneBrand = Build.MANUFACTURER;
                            updateProgress("Test finished", 10);

                            Results results = new Results(time, networkProvider, phoneBrand, place, netPerf, signalPerf);
                            displayResults(results.getNetPerf(), results.getSignalPerf());
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

        progressTV.post(new Runnable(){
            @Override
            public void run(){
                progressTV.setText(progressText);
            }
        });

        progressBar.incrementProgressBy(progressIncrement);
    }

    public String getNetworkProvider(){
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        return telephonyManager.getNetworkOperatorName();
    }

    public Place computePlace(String placeName){
        double latitude = qrLocations.get(placeName)[0];
        double longitude = qrLocations.get(placeName)[1];

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            try {
                Location location = Tasks.await(fusedLocationClient.getLastLocation());
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                updateProgress("Location acquired", 20);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }else{
            updateProgress("Permissions not given for getting location. Skipping", 20);
        }

        return new Place(placeName, latitude, longitude);
    }

    public SignalPerf computeSignalPerf(){
        //default values
        //can be filtered out later
        int rssi = 1;
        int rsrp = 1;
        int rsrq = 1;

        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            CellInfoLte cellinfolte = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rssi = cellSignalStrengthLte.getRssi();
                updateProgress("RSSI computed", 10);
                rsrp = cellSignalStrengthLte.getRsrp();
                updateProgress("RSRP computed", 10);
                rsrq = cellSignalStrengthLte.getRsrq();
                updateProgress("RSRQ computed", 10);
            }else{
                updateProgress("Phone cannot compute signal performance. Skipping", 30);
            }
        }else{
            updateProgress("Permissions were not given to compute signal performance. Skipping", 30);
        }
        return new SignalPerf(rssi, rsrq, rsrp);
    }


    public void displayResults(NetPerf netPerf, SignalPerf signalPerf){
        TextView downloadResultTV = (TextView) getView().findViewById(R.id.downloadResultTV);
        TextView uploadResultTV = (TextView) getView().findViewById(R.id.uploadResultTV);
        TextView latencyResultTV = (TextView) getView().findViewById(R.id.latencyResultTV);
        TextView rssiResultTV = (TextView) getView().findViewById(R.id.rssiResultTV);
        TextView rsrpResultTV = (TextView) getView().findViewById(R.id.rssiResultTV);
        TextView rsrqResultTV = (TextView) getView().findViewById(R.id.rssiResultTV);


        if(netPerf.getDlspeed() != -1) downloadResultTV.setText(String.format("%.1f", netPerf.getDlspeed()));
        else downloadResultTV.setText("N/A");

        if(netPerf.getUlspeed() != -1) uploadResultTV.setText(String.format("%.1f", netPerf.getUlspeed()));
        else uploadResultTV.setText("N/A");

        if(netPerf.getLatency() != -1) latencyResultTV.setText(Integer.toString(netPerf.getLatency()));
        else latencyResultTV.setText("N/A");

        if(signalPerf.getRssi() != 1) rssiResultTV.setText(Integer.toString(signalPerf.getRssi()));
        else rssiResultTV.setText("N/A");

        if(signalPerf.getRsrp() != 1) rsrpResultTV.setText(Integer.toString(signalPerf.getRsrp()));
        else rsrpResultTV.setText("N/A");

        if(signalPerf.getRsrq() != 1) rsrqResultTV.setText(Integer.toString(signalPerf.getRsrq()));
        else rsrqResultTV.setText("N/A");
    }

    public native NetPerf runSpeedtest() throws Exception;
    public native long getServerInfo();
    public native double computeDlspeed(long serverPtr);
    public native double computeUlspeed(long serverPtr);
    public native int computeLatency(long serverPtr);


}