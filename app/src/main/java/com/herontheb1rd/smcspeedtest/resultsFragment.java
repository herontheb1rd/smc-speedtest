package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class resultsFragment extends Fragment {

    private static final String ARG_DLSPEED = "param1";
    private static final String ARG_ULSPEED = "param2";
    private static final String ARG_LATENCY = "param3";

    private String mDlspeedStr;
    private String mUlspeedStr;
    private String mLatencyStr;

    public resultsFragment() {
        // Required empty public constructor
    }

    public static resultsFragment newInstance(String mDlspeedStr, String mUlspeedStr, String mLatencyStr) {
        resultsFragment fragment = new resultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DLSPEED, mDlspeedStr);
        args.putString(ARG_ULSPEED, mUlspeedStr);
        args.putString(ARG_LATENCY, mLatencyStr);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDlspeedStr = getArguments().getString(ARG_DLSPEED);
            mUlspeedStr = getArguments().getString(ARG_ULSPEED);
            mLatencyStr = getArguments().getString(ARG_LATENCY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        TextView downloadResultTV = (TextView) view.findViewById(R.id.downloadResultTV);
        TextView uploadResultTV = (TextView) view.findViewById(R.id.uploadResultTV);
        TextView latencyResultTV = (TextView) view.findViewById(R.id.latencyResultTV);

        //this is probably not intended but whatever
        //when it receives the results from runTestFragment, display values
        //calling setText() within onFragmentResult() makes it so values arent empty
        //weird, but it doesnt automatically get the values when the screen does the griddy
        getParentFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String rawLocation = bundle.getString("resultKey");

                speedtestJava speedtest = new speedtestJava(rawLocation);
                speedtest.getResults();

                //we can access the dlspeed, ulspeed, and latency through the speedtest class and convert them to string
                //also stores it so we can do the goofy saved instance state thing
                mDlspeedStr = Double.toString(speedtest.dlspeed);
                mUlspeedStr = Double.toString(speedtest.ulspeed);
                mLatencyStr = Long.toString(speedtest.latency);

                //and we display then here
                downloadResultTV.setText(mDlspeedStr);
                uploadResultTV.setText(mUlspeedStr);
                latencyResultTV.setText(mLatencyStr);
            }
        });

        return view;
    }


}