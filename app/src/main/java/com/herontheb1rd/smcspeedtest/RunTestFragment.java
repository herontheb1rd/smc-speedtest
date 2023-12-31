package com.herontheb1rd.smcspeedtest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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

public class RunTestFragment extends Fragment {
    private final String[] allowedLocations = {"Library", "Canteen", "Kiosk", "Airport", "ABD", "Garden"};
    private static boolean justScannedQR;

    public RunTestFragment(){
        justScannedQR = false;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_run_test, container, false);

        //module install apis fix for google code scanner
        ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(view.getContext());
        OptionalModuleApi optionalModuleApi = GmsBarcodeScanning.getClient(view.getContext());
        ModuleInstallRequest moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                        .addApi(optionalModuleApi)
                        .build();
        moduleInstallClient.installModules(moduleInstallRequest);

        Button runTestB = (Button) view.findViewById(R.id.runTestB);
        runTestB.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                new AlertDialog.Builder(getActivity())
                        .setTitle("User Agreement")
                        .setMessage("This application will record your phone brand, and the location you scanned your QR code in (but not your phone's longitude and latitude). We will not release this data publicly, but we will use it for our study. \n\nBy pressing Yes you agree to this data being collected. ")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                scanQRCode();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });

        return view;
    }

    private boolean isConnected(){
        boolean isConnected = false;

        //getActiveNetworkInfo is deprecated after version 29
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(ConnectivityManager.class);
            Network currentNetwork = connectivityManager.getActiveNetwork();
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);

            if(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)){
                isConnected = true;
            }
        }else{
            ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

            if(connectivityManager.getActiveNetworkInfo() != null){
                isConnected = true;
            }
        }

        return isConnected;
    }

    private void scanQRCode(){
        if(isConnected()){
            GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_QR_CODE)
                    .build();

            GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(getView().getContext(), options);
            scanner.startScan().addOnSuccessListener(
                    barcode -> {
                        String rawValue = barcode.getRawValue();

                        boolean isValidLocation = false;
                        for(int i = 0; i < allowedLocations.length; i++){
                            if(rawValue.equals(allowedLocations[i])){
                                isValidLocation = true;
                                break;
                            }
                        }
                        if(isValidLocation){
                            Bundle result = new Bundle();
                            result.putString("bundleKey", rawValue);
                            getParentFragmentManager().setFragmentResult("requestKey", result);
                            justScannedQR = true;
                        }else{
                            Toast.makeText(getActivity(), "Invalid QR code.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }else{
            Toast.makeText(getActivity(), "You must have an internet connection to run the test.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        if(justScannedQR){
            Navigation.findNavController(getView()).navigate(R.id.action_runTestFragment_to_resultsFragment);
            justScannedQR = false;
        }
    }
}

