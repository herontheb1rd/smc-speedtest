package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RunTestFragment extends Fragment {
    SharedPreferences prefs = null;
    private final Set<String> allowedLocations = Sets.newHashSet("Library", "Kiosks", "Canteen", "ABD", "Garden", "Airport");
    private DecoratedBarcodeView barcodeScannerView;
    private String mScanResult = "";
    private final int MAX_TRIES = 3;
    ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    private final BarcodeCallback callback = result -> {
        if(result.getText() == null) {
            return;
        }
        mScanResult = result.getText();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_test, container, false);

        //zxing barcode initialization
        barcodeScannerView = view.findViewById(R.id.barcodeScanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeScannerView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));

        //used for checking whether we've connected to a server
        AtomicBoolean isServerConnected = new AtomicBoolean(false);
        //used to store the connected server info
        //we could remove this altogether and just store it in the main activity
        //but i'd like to be redundant
        AtomicReference<ServerInfo> serverInfo = new AtomicReference<>(null);

        //on start:
        //  1) send user agreement
        //  2) ask for location permissions
        //  3) stop the scan (in case we havent)
        checkIfAgreed();
        askLocationPermission();
        stopScan();

        //tries to connect to a server for internet testing
        //if we have a connected server (i.e. we just connected to a server), then we can skip everything
        //if we haven't, run the following loop:
        //  1) wait until the user's internet is on
        //  2) if it is, try to connect to the server
        //  3) try thrice until successful
        ListeningExecutorService serverListeningExecService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        ListenableFuture<Integer> serverInfoFuture = serverListeningExecService.submit(() -> {
            if(getStoredServerInfo() != null){
                getActivity().runOnUiThread(() -> displayServerInfoSuccess(view));
                isServerConnected.set(true);
                serverInfo.set(getStoredServerInfo());
                return 0;
            }
            
            while (true) {
                if (isConnected()) {
                    ServerInfo result;

                    for (int i = 0; i < MAX_TRIES; i++) {
                        result = getServerInfo();
                        if (result.name.equals("ipInfo")) {
                            getActivity().runOnUiThread(() -> displayIpInfoFailed(view));
                        } else if (result.name.equals("serverList")) {
                            getActivity().runOnUiThread(() -> displayServerListFailed(view));
                        } else {
                            getActivity().runOnUiThread(() -> displayServerInfoSuccess(view));
                            isServerConnected.set(true);
                            serverInfo.set(result);
                            setStoredServerInfo(result);
                            return 0;
                        }
                    }

                    displayServerConnectionFailed(view);

                    return 0;
                }
            }
        });

        //thread for starting the qr code
        //checks the following:
        //  1) checks if the location is on
        //  2) checks if the data is on
        //  3) checks if we have connected to a server
        //if 1, 2, and 3 are good, then we start the QR code
        //it also updates the display for these three
        //after the qr code, it transitions to the next screen

        ListeningExecutorService qrListeningExecService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        ListenableFuture<Integer> qrFuture = qrListeningExecService.submit(() -> {
            //because we update the display when the location is on/off, its better to only call the display update once
            boolean readyToRun = false;
            boolean hadLocOn = false;
            boolean hadDataOn = false;

            //these are so we only have to run the functions once per loop
            boolean locationOn, dataOn;

            while(true){
                locationOn = isLocationOn();
                dataOn = isConnected();

                //check if location is on/off, then update display
                if(locationOn){
                    if(!hadLocOn){
                        getActivity().runOnUiThread(() -> displayLocationOn(view));
                        hadLocOn = true;
                    }
                }else{
                    if(hadLocOn){
                        getActivity().runOnUiThread(() -> displayLocationOff(view));
                        hadLocOn = false;
                    }
                }

                //check if mobile data is on/off, then update display
                if(dataOn){
                    if(!hadDataOn){
                        getActivity().runOnUiThread(() -> displayDataOn(view));
                        getActivity().runOnUiThread(() ->displayCantConnectServer(view));
                        hadDataOn = true;
                    }
                }else{
                    if(hadDataOn){
                        getActivity().runOnUiThread(() -> displayDataOff(view));
                        hadDataOn = false;
                    }
                }


                //checks if location, mobile data are on, AND if we connected to a server
                if(locationOn && dataOn && isServerConnected.get()){
                    if(!readyToRun) {
                        readyToRun = true;

                        boolean userAgreed = checkIfAgreed();
                        boolean locationAgreed = askLocationPermission();
                        if(userAgreed && locationAgreed) {
                            getActivity().runOnUiThread(this::startScan);
                        }else{
                            if(!userAgreed){
                                Toast.makeText(getActivity(), "Test won't run unless you agree to user agreement", Toast.LENGTH_LONG).show();
                            }

                            if(!locationAgreed){
                                Toast.makeText(getActivity(), "Test won't run unless you allow location permissions", Toast.LENGTH_LONG).show();
                            }

                            //looks stupid, but its for the user to read
                            Thread.sleep(1000);
                        }
                    }
                }else{
                    if(readyToRun){
                        readyToRun = false;
                        getActivity().runOnUiThread(this::stopScan);
                    }
                }

                if(!mScanResult.equals("")){
                    if(allowedLocations.contains(mScanResult)){
                        getActivity().runOnUiThread(this::successScan);
                        Navigation.findNavController(view).navigate(R.id.action_runTestFragment_to_resultsFragment);
                        return 0;
                    }

                    getActivity().runOnUiThread(this::invalidScan);

                    //looks stupid, but its for the user to read
                    Thread.sleep(1000);
                    readyToRun = false;
                    hadLocOn = false;
                    hadDataOn = false;

                }
            }
        });


        return view;
    }


    private ServerInfo getStoredServerInfo(){
        return ((MainActivity) getContext()).getServerInfo();
    }

    private void setStoredServerInfo(ServerInfo serverInfo){
        ((MainActivity) getContext()).setServerInfo(serverInfo);
    }
    private boolean checkIfAgreed(){
        if (!prefs.getBoolean("agreed", true)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("User Agreement")
                    .setMessage("This application will record your phone brand, and the location you scanned your QR code in. We will not release this data publicly, but we will use it in our study.\n\nBy pressing Yes you agree to this data being collected. ")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> prefs.edit().putBoolean("agreed", true).apply())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
        return prefs.getBoolean("agreed", true);
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            Network currentNetwork = cm.getActiveNetwork();
            if(currentNetwork == null)
                return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(currentNetwork);
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }else{
            if(cm.getActiveNetworkInfo() != null){
                return cm.getActiveNetworkInfo().isConnectedOrConnecting();
            }else{
                return false;
            }
        }
    }

    private boolean isLocationOn(){
        askLocationPermission();
        LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean askLocationPermission(){
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(getActivity())
                    .setTitle("User Agreement")
                    .setMessage("This application requires location permissions to get signal data.")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> locationPermissionRequest.launch(new String[] {
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    }))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        //check again, and return true if user allowed it
        return (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        //barcodeScannerView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }


    public void displayCantConnectServer(View view){
        view.findViewById(R.id.connectedTV).setAlpha(0.2f);
        TextView connectedTV = view.findViewById(R.id.connectedTV);
        connectedTV.setText("Can't connect to server");
    }

    public void displayLocationOn(View view){
        view.findViewById(R.id.locationTV).setAlpha(1);
        TextView locationTv = view.findViewById(R.id.locationTV);
        locationTv.setText("Location is on");
    }

    public void displayLocationOff(View view){
        view.findViewById(R.id.locationTV).setAlpha(0.2f);
        TextView locationTv = view.findViewById(R.id.locationTV);
        locationTv.setText("Turn on your location");
    }

    public void displayDataOn(View view){
        view.findViewById(R.id.dataTV).setAlpha(1);
        TextView dataTV = view.findViewById(R.id.dataTV);
        dataTV.setText("Mobile data is on");
    }

    public void displayDataOff(View view){
        view.findViewById(R.id.dataTV).setAlpha(0.2f);
        TextView dataTV = view.findViewById(R.id.dataTV);
        dataTV.setText("Turn on your mobile data");
    }

    public void displayIpInfoFailed(View view){
        view.findViewById(R.id.connectedTV).setAlpha(0.2f);
        TextView connectedTV = view.findViewById(R.id.connectedTV);
        connectedTV.setText("Failed to get IP info, retrying...");
    }

    public void displayServerListFailed(View view){
        view.findViewById(R.id.connectedTV).setAlpha(0.2f);
        TextView connectedTV = view.findViewById(R.id.connectedTV);
        connectedTV.setText("Failed to get server list, retrying...");
    }

    public void displayServerInfoSuccess(View view){
        view.findViewById(R.id.connectedTV).setAlpha(1);
        TextView connectedTV = view.findViewById(R.id.connectedTV);
        connectedTV.setText("Connected to server");
    }

    public void displayServerConnectionFailed(View view){
        view.findViewById(R.id.connectedTV).setAlpha(0.2f);
        TextView connectedTV = view.findViewById(R.id.connectedTV);
        connectedTV.setText("Failed to connect to server, try restarting application");
    }


    public void stopScan() {
        barcodeScannerView.setStatusText("Test will not run unless location and mobile data are on");
        barcodeScannerView.pause();
        //barcodeScannerView.getViewFinder().setVisibility(View.INVISIBLE);
    }

    public void displayServerConnecting(){
        barcodeScannerView.setStatusText("Still connecting to server...");
        barcodeScannerView.pause();
    }


    public void updateProgress(String progress){
        getActivity().runOnUiThread(() -> {
            Log.i("test", progress);
            TextView connectedTV = getView().findViewById(R.id.connectedTV);
            connectedTV.setAlpha(0.2f);
            connectedTV.setText(progress);
        });
    }
    
    public void startScan() {
        barcodeScannerView.setStatusText("Scan QR code to begin the test");
        barcodeScannerView.resume();
        barcodeScannerView.getViewFinder().setVisibility(View.VISIBLE);
        barcodeScannerView.decodeSingle(callback);
    }

    public void successScan(){
        barcodeScannerView.setStatusText("Scan successful");
        barcodeScannerView.pause();
        barcodeScannerView.getViewFinder().setVisibility(View.INVISIBLE);
    }

    public void invalidScan(){
        mScanResult = "";
        barcodeScannerView.setStatusText("Scan failed, invalid QR code. Retrying...");
        barcodeScannerView.pause();
        //barcodeScannerView.getViewFinder().setVisibility(View.INVISIBLE);
    }

    public native ServerInfo getServerInfo();
}

