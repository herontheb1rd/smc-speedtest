package com.herontheb1rd.smcspeedtest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
    static boolean justScannedQR;

    public RunTestFragment() {
        // Required empty public constructor
        justScannedQR = false;
    }
    public static RunTestFragment newInstance() {
        RunTestFragment fragment = new RunTestFragment();
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
                //check if user is using mobile data first
                //if not, show toast
                ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if(connectivityManager.getActiveNetworkInfo() != null){
                    //scan QR code with Google Code Scanner
                    GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE)
                            .build();
                    GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(view.getContext(), options);
                    scanner
                            .startScan()
                            //if successful, pass value of qr code to results panel
                            .addOnSuccessListener(
                                    barcode -> {
                                        String rawValue = barcode.getRawValue();
                                        Bundle result = new Bundle();
                                        result.putString("bundleKey", "result");
                                        getParentFragmentManager().setFragmentResult("requestKey", result);

                                        justScannedQR = true;
                                    });
                }else{
                    CharSequence message = "You must have an internet connection to run the test";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(getActivity(), message, duration);
                    toast.show();
                }
            }
        });

        return view;
    }

    //waits until successful resume from qr code before going to results panel
    @Override
    public void onResume(){
        super.onResume();

        if(justScannedQR){
            Navigation.findNavController(getView()).navigate(R.id.action_runTestFragment_to_resultsFragment);
            justScannedQR = false;
        }
    }
}

