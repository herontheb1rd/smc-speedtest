package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

//TODO: add Google Maps SDK
public class HeatMapFragment extends Fragment implements AdapterView.OnItemSelectedListener{
    private DatabaseReference mDatabase;
    private ArrayList<Results> curResults = new ArrayList<Results>();

    public HeatMapFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heat_map, container, false);

        //initialize spinner
        Spinner spinner = (Spinner)view.findViewById(R.id.metric_spinner);
        String[] metricOptions = {"Download Speed", "Upload Speed", "Latency"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                metricOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //get results from database
        long before24hours = new Date().getTime() - (24 * 3600 * 1000);
        Query timeQuery = mDatabase.child("results").orderByChild("date_time")
                .startAt(before24hours);
        timeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    curResults.add(singleSnapshot.getValue(Results.class));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Database error", "onCancelled", databaseError.toException());
            }
        });

        return view;
    }


    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        updateHeatMap(pos);
    }

    public void onNothingSelected(AdapterView<?> parent){
        resetHeatMap();
    }

    public void updateHeatMap(int metric){

    }

    public void resetHeatMap(){

    }

}