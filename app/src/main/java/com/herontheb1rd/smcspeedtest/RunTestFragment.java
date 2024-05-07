//RunTestFragment.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.Arrays;

public class RunTestFragment extends Fragment {
    SharedPreferences prefs = null;
    private final String[] allowedLocations = {"Library", "Canteen", "Kiosk", "Airport", "ABD", "Garden"};
    private static boolean mQRValid = false;
    private static boolean mRanOnce;
    

    ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            fineLocationGranted = result.getOrDefault(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                        }
                        Boolean coarseLocationGranted = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            coarseLocationGranted = result.getOrDefault(
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,false);
                        }
                        if (fineLocationGranted != null && fineLocationGranted) {
                            Toast.makeText(getActivity(), "Location access granted.",
                                    Toast.LENGTH_SHORT).show();
                        } else if (coarseLocationGranted != null && coarseLocationGranted) {
                            Toast.makeText(getActivity(), "Approximate location will be used. Accuracy may be affected.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // No location access granted.
                            Toast.makeText(getActivity(), "Location access not granted. Accuracy may be affected.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_test, container, false);

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)){
            new AlertDialog.Builder(getActivity())
                    .setTitle("User Agreement")
                    .setMessage("This application requires location permissions to get signal data. This does NOT get your longitude and latitude.")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> askLocationPermission())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        //on a separate thread, check if the user's internet/location is on
        //if it is, start the qr test
        new Thread(() -> {
            while(!mRanOnce){
                if(isConnected() && isLocationOn()){
                    checkIfAgreed(view);
                    break;
                }
            }
        }).start();

        //module install apis fix for google code scanner
        ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(view.getContext());
        OptionalModuleApi optionalModuleApi = GmsBarcodeScanning.getClient(view.getContext());
        ModuleInstallRequest moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                        .addApi(optionalModuleApi)
                        .build();
        moduleInstallClient.installModules(moduleInstallRequest);

        Button runTestB = view.findViewById(R.id.runTestB);
        runTestB.setOnClickListener(v -> checkIfAgreed(view));

        return view;
    }

    private boolean preQRCode(){
        boolean canUserRun = true;
        if(!isConnected()) {
            Toast.makeText(getActivity(), "Turn on your mobile data to continue",
                    Toast.LENGTH_SHORT).show();
            canUserRun = false;
        }
        if(!isLocationOn()){
            Toast.makeText(getActivity(), "Turn on your location to continue",
                    Toast.LENGTH_SHORT).show();
            canUserRun = false;
        }

        return canUserRun;
    }

    private void checkIfAgreed(View view){
        if (prefs.getBoolean("agreed", false)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("User Agreement")
                    .setMessage("This application will record your phone brand, and the location you scanned your QR code in. We will not release this data publicly, but we will use it for our study.\n\nBy pressing Yes you agree to this data being collected. ")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        prefs.edit().putBoolean("agreed", true).apply();

                        if(preQRCode())
                            scanQRCode(view);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }else{
            if(preQRCode())
                scanQRCode(view);
        }
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        //getActiveNetworkInfo is deprecated after version 29
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
        LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void askLocationPermission(){
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            locationPermissionRequest.launch(new String[] {
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }


    private void scanQRCode(View view){

        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE)
        .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(view.getContext(), options);
        scanner.startScan().addOnSuccessListener(
                barcode -> {
                    mQRValid = true;

                    if(!mRanOnce) {
                        mRanOnce = true;
                    }

                    String rawValue = barcode.getRawValue();
                    boolean isValidLocation = Arrays.asList(allowedLocations).contains(rawValue);
                    if(!isValidLocation){
                        Toast.makeText(getActivity(), "Invalid QR code",
                                Toast.LENGTH_SHORT).show();
                        mQRValid = false;
                    }

                    if(mQRValid){
                        Bundle result = new Bundle();
                        result.putString("bundleKey", rawValue);
                        getParentFragmentManager().setFragmentResult("requestKey", result);
                    }
                }).addOnCanceledListener(()-> mRanOnce = true);
    }

    @Override
    public void onResume(){
        super.onResume();

        //if the user has interacted *at least once* with anything else (e.g. nav drawer or failed test)
        //then dont auto start it because it gets really annoying
        mRanOnce = true;

        if(mQRValid){
            Navigation.findNavController(getView()).navigate(R.id.action_runTestFragment_to_resultsFragment);
            mQRValid = false;
        }
    }
}

