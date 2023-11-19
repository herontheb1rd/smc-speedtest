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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

//TODO: add Google Maps SDK
public class HeatMapFragment extends Fragment implements AdapterView.OnItemSelectedListener, OnMapReadyCallback {
    private DatabaseReference mDatabase;
    private ArrayList<Results> results = new ArrayList<Results>();

    public HeatMapFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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
                    results.add(singleSnapshot.getValue(Results.class));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Database error", "onCancelled", databaseError.toException());
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        final LatLng PSHS = new LatLng(7.082788894235911, 125.50813754841627);
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(PSHS));
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        updateHeatMap(pos);
    }

    public void onNothingSelected(AdapterView<?> parent){
        resetHeatMap();
    }

    public void updateHeatMap(int metric){
        List<WeightedLatLng> weightedLatLngs = new ArrayList<>();
        for(Results curResult : results){
            double intensity = 0;
            switch(metric){
                case 0:
                    intensity = curResult.getNetPerf().getDlspeed();
                    break;
                case 1:
                    intensity = curResult.getNetPerf().getUlspeed();
                    break;
                case 2:
                    intensity = curResult.getNetPerf().getLatency();
                    break;
                case 3:
                    intensity = curResult.getSignalPerf().getRssi();
                    //skips if stored value is invalid
                    if(intensity == 1){
                        continue;
                    }
                    break;
                case 4:
                    intensity = curResult.getSignalPerf().getRsrp();
                    if(intensity == 1){
                        continue;
                    }
                    break;
                case 5:
                    intensity = curResult.getSignalPerf().getRsrq();
                    if(intensity == 1){
                        continue;
                    }
                    break;
            }

            weightedLatLngs.add(new WeightedLatLng(new LatLng(curResult.getPlace().getLatitude(),
                    curResult.getPlace().getLongitude()),
                    intensity));
        }

        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .weightedData(weightedLatLngs)
                .build();

    }

    public void resetHeatMap(){

    }

}