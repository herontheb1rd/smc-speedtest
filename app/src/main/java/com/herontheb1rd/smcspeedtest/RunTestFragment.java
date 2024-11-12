package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.collect.Sets;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunTestFragment extends Fragment {
    SharedPreferences prefs = null;
    private final Set<String> allowedLocations = Sets.newHashSet("Library", "Kiosks", "Canteen", "ABD", "Garden", "Airport");
    private DecoratedBarcodeView barcodeScannerView;
    BroadcastReceiver dataLocationReceiver;
    IntentFilter dataFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    IntentFilter locationFilter = new IntentFilter("android.location.PROVIDERS_CHANGED");


    ActivityResultLauncher<String[]> permissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                View view = getView();

                if(view == null)
                    return;

                if(checkPermissions()){
                    updateEverything(view);
                    displayPermissionsAccepted(view);
                }else{
                    displayPermissionsUnaccepted(view);
                }
            });


    private final BarcodeCallback callback = result -> {
        String scanResult = result.getText();
        if(scanResult == null) {
            return;
        }

        if(!scanResult.isEmpty() && allowedLocations.contains(scanResult)){
            successScan();
            goToResultsFragment(scanResult);
        }else{
            invalidScan();
        }
    };

    @Override
    public void onStart(){
        super.onStart();

        getActivity().registerReceiver(dataLocationReceiver, dataFilter);
        getActivity().registerReceiver(dataLocationReceiver, locationFilter);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

        setUID(getActivity().getApplicationContext());
        dataLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                View view = getView();

                updateEverything(view);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_test, container, false);

        //zxing barcode initialization
        barcodeScannerView = view.findViewById(R.id.barcodeScanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeScannerView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));

        stopScan();
        setUsernameOnFirstLaunch();
        askPermissions();
        updateEverything(view);

        barcodeScannerView.setOnClickListener(view1 -> {
            if(!checkPermissions())
                askPermissions();
        });

        return view;
    }

    public void updateEverything(View view){
        ExecutorService executor = Executors.newFixedThreadPool(8);
        executor.submit(() -> updateLocationStatus(view));
        executor.submit(() -> updateDataStatus(view));
        executor.submit(() -> updateQRStatus(view));
        executor.submit(() -> tryStartQRScan(view));
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(dataLocationReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeScannerView.pause();
    }

    private void tryStartQRScan(View view){
        if(view == null){
            return;
        }

        boolean permitted = checkPermissions();

        if(!permitted){
            getActivity().runOnUiThread(() -> displayPermissionsUnaccepted(view));
        }else{
            getActivity().runOnUiThread(() -> displayPermissionsAccepted(view));
        }

        if(isConnected() && isLocationOn()){
            if(permitted){
                getActivity().runOnUiThread(this::startScan);
            }
        }else{
            stopScan();
        }
    }

    private void askPermissions(){
        askUserPermission();
        askLocationPermission();
        askCameraPermission();
    }

    private boolean checkPermissions(){
        return checkUserPermission() && checkLocationPermission() && checkCameraPermission();
    }
    private void askUserPermission(){
        if (!prefs.getBoolean("agreed", false)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("User Agreement")
                    .setMessage("This application will record your phone brand, and the location you scanned your QR code in. We will not release this data publicly, but we will use it in our study.\n\nBy pressing Yes you agree to this data being collected. ")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        prefs.edit().putBoolean("agreed", true).apply();
                        if(checkPermissions()){
                            View view = getView();
                            if(view != null)
                                updateEverything(view);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private boolean checkUserPermission(){
        return prefs.getBoolean("agreed", false);
    }


    private void setUsernameOnFirstLaunch(){
        if(!prefs.getBoolean("setUsername", false)){
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext(), R.style.UsernameAlertStyle);
            builder.setTitle("Set Username");
            builder.setMessage("\nType in a username. This will be used for the scoreboard. This can be changed later in your Profile.\n\nMaximum of 20 characters");
            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String username = input.getText().toString();
                prefs.edit().putBoolean("setUsername", true).apply();
                prefs.edit().putString("username", username).apply();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        }
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            Network currentNetwork = cm.getActiveNetwork();
            if(currentNetwork == null) {
                return false;
            }
            NetworkCapabilities caps = cm.getNetworkCapabilities(currentNetwork);
            //return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        }else{
            if(cm.getActiveNetworkInfo() != null){
                //return cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
                return cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE || cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
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

    private void askLocationPermission(){
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(getActivity())
                    .setTitle("Location Permission")
                    .setMessage("This application requires location permissions to get signal data.")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> permissionRequest.launch(new String[] {
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    }))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private boolean checkLocationPermission(){
        return (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void askCameraPermission(){
        if(ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            new AlertDialog.Builder(getActivity())
                    .setTitle("Camera Permission")
                    .setMessage("This application requires camera permissions to scan the QR codes.")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> permissionRequest.launch(new String[] {
                            android.Manifest.permission.CAMERA
                    }))
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private boolean checkCameraPermission(){
        return ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
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

    public void displayPermissionsAccepted(View view){
        view.findViewById(R.id.permissionsTV).setAlpha(1f);
        TextView permissionsTV = view.findViewById(R.id.permissionsTV);
        permissionsTV.setText("Permissions accepted");
    }

    public void displayPermissionsUnaccepted(View view){
        view.findViewById(R.id.permissionsTV).setAlpha(0.2f);
        TextView permissionsTV = view.findViewById(R.id.permissionsTV);
        permissionsTV.setText("Accept permissions to start");
    }

    private void updateLocationStatus(View view){
        if(view == null) {
            return;
        }

        if(isLocationOn()) {
            getActivity().runOnUiThread(() -> displayLocationOn(view));
        }
        else{
            getActivity().runOnUiThread(() -> displayLocationOff(view));
            getActivity().runOnUiThread(this::stopScan);
        }
    }

    private void updateDataStatus(View view){
        if(view == null) {
            return;
        }
        if(isConnected()) {
            getActivity().runOnUiThread(() -> displayDataOn(view));
        }
        else{
            getActivity().runOnUiThread(() -> displayDataOff(view));
            getActivity().runOnUiThread(this::stopScan);
        }
    }

    private void updateQRStatus(View view){
        if(view == null) {
            return;
        }

        boolean locationOn = isLocationOn();
        boolean dataOn = isConnected();
        if(!locationOn || !dataOn){
            getActivity().runOnUiThread(this::displayLocDataOff);
        }
    }


    
    public void startScan() {
        barcodeScannerView.setStatusText("Scan QR code to begin the test");
        barcodeScannerView.resume();
        barcodeScannerView.getViewFinder().setVisibility(View.VISIBLE);
        barcodeScannerView.decodeContinuous(callback);
    }

    public void stopScan() {
        if(checkPermissions()){
            barcodeScannerView.setStatusText("Test will not run unless location and mobile data are on");
        }else{
            barcodeScannerView.setStatusText("Permissions needed for application to function properly. Tap here to request permissions again.");
        }
        barcodeScannerView.pause();
        barcodeScannerView.getViewFinder().setVisibility(View.INVISIBLE);
    }

    public void invalidScan(){
        barcodeScannerView.setStatusText("Scan failed, invalid QR code. Try to scan again.");
    }

    public void displayLocDataOff(){
        barcodeScannerView.setStatusText("Test will not run unless location and mobile data are on");
    }

    public void successScan(){
        barcodeScannerView.setStatusText("Scan successful");
        barcodeScannerView.pause();
        barcodeScannerView.getViewFinder().setVisibility(View.INVISIBLE);
    }

    public void goToResultsFragment(String scanResult){
        Bundle qrResult = new Bundle();
        qrResult.putString("bundleKey", scanResult);
        getParentFragmentManager().setFragmentResult("requestKey", qrResult);
        Navigation.findNavController(getView()).navigate(R.id.action_runTestFragment_to_resultsFragment);
    }

    private void setUID(Context context){
        //this should run at least once
        //after we dont have to get the UID programmatically ever again
        //which avoids getContext() errors when it returns null
        if(prefs.getString("UID", "").isEmpty()){
            prefs.edit().putString("UID", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).apply();
        }
    }


}

