package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class runTestFragment extends Fragment {
    public runTestFragment() {
        // Required empty public constructor
    }
    public static runTestFragment newInstance() {
        runTestFragment fragment = new runTestFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_run_test, container, false);

        //module install apis fix for google code scanner
        ModuleInstallClient moduleInstallClient = ModuleInstall.getClient(view.getContext());
        OptionalModuleApi optionalModuleApi = GmsBarcodeScanning.getClient(view.getContext());
        ModuleInstallRequest moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                        .addApi(optionalModuleApi)
                        .build();
        moduleInstallClient.installModules(moduleInstallRequest);

        //run QR code scanner on button press
        Button runTestB = (Button) view.findViewById(R.id.runTestB);
        runTestB.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //scan QR code with Google code scanner
                //lazy solution, but it works since I don't need something custom
                //heck Google recommends it, so sue me
                GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE)
                        .build();
                GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(view.getContext(), options);
                scanner
                        .startScan()
                        .addOnSuccessListener(
                                barcode -> {
                                    String rawValue = barcode.getRawValue();
                                    speedtestJava speedtest = new speedtestJava(rawValue);
                                    speedtest.storeResults();

                                    //store results in strings
                                    String dlspeedStr, ulspeedStr, latencyStr;
                                    dlspeedStr = Double.toString(speedtest.dlspeed);
                                    ulspeedStr = Double.toString(speedtest.ulspeed);
                                    latencyStr = Long.toString(speedtest.latency);
                                });
            }
        });


        return view;
    }
}

