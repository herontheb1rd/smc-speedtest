package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
        View v = inflater.inflate(R.layout.fragment_run_test, container, false);

        Button runTestB = (Button) v.findViewById(R.id.runTestButton);

        return v;
    }
}