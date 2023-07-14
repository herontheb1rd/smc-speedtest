package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

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

        //display results from speedtest
        //the important ones, anyway
        //can be expanded further to fit more
        TextView downloadResultTV = (TextView) view.findViewById(R.id.downloadResultTV);
        downloadResultTV.setText(mDlspeedStr);

        TextView uploadResultTV = (TextView) view.findViewById(R.id.uploadResultTV);
        downloadResultTV.setText(mUlspeedStr);

        TextView latencyResultTV = (TextView) view.findViewById(R.id.latencyResultTV);
        downloadResultTV.setText(mLatencyStr);

        return view;
    }
}