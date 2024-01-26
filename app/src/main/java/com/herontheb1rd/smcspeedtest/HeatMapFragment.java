package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeatMapFragment extends Fragment implements AdapterView.OnItemSelectedListener, OnMapReadyCallback {
    private DatabaseReference mDatabase;
    private ArrayList<Results> results = new ArrayList<Results>();
    private final Map<String, LatLng[]> locationDict = new HashMap<String, LatLng[]>() {{
        put("Library", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Canteen", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Kiosk", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Airport", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("ABD", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Garden", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
    }};

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
        //SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager()
        //       .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);
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
                TextView dataTV = (TextView) getActivity().findViewById(R.id.dataTV);
                dataTV.setText(Long.toString(results.get(0).getTime()));
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

        for(LatLng[] ll: locationDict.values()){
            Polygon polygon = googleMap.addPolygon(new PolygonOptions()
                    .add(ll));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int metric, long id) {
        updateHeatMap(metric);
    }

    public void onNothingSelected(AdapterView<?> parent){
        resetHeatMap();
    }

    //arduino's map function: https://www.arduino.cc/reference/en/language/functions/math/map/
    //used to scale the results to the color values on a heat map
    private int map(double x, double in_min, double in_max, double out_min, double out_max) {
        return (int) ((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    private double getMeanResult(List<Double> doubleList){
        if(!doubleList.isEmpty()){
            double sum = 0.0;
            for (Double d : doubleList) {
                sum += d;
            }
            return sum / doubleList.size();
        }
        return 0.0;
    }

    private double getMinResult(List<Double> doubleList){
        return doubleList.indexOf(Collections.min(doubleList));
    }

    private double getMaxResult(List<Double> doubleList){
        return doubleList.indexOf(Collections.max(doubleList));
    }

    public List<Integer> updateHeatMap(int metric){
        List<Integer> colorList = new ArrayList<>();
        for(String location: locationDict.keySet()) {
            List<Double> resultsList = new ArrayList<>();
            for (Results curResult : results) {
                double intensity = 0;
                switch (metric) {
                    case 0:
                        intensity = curResult.getNetPerf().getDlspeed();
                        break;
                    case 1:
                        intensity = curResult.getNetPerf().getUlspeed();
                        break;
                    case 2:
                        intensity = Double.valueOf(curResult.getNetPerf().getLatency());
                        break;
                    case 3:
                        intensity = Double.valueOf(curResult.getSignalPerf().getRssi());
                        break;
                    case 4:
                        intensity = Double.valueOf(curResult.getSignalPerf().getRsrp());
                        break;
                    case 5:
                        intensity = Double.valueOf(curResult.getSignalPerf().getRsrq());
                        break;
                }
                //skip if values are invalid
                if ((metric > 0 && metric <= 3) && intensity == -1) continue;
                if ((metric > 3 && metric <= 6) && intensity == 1) continue;

                resultsList.add(intensity);
            }

            double meanResult = getMeanResult(resultsList);
            //chooses color from meanResult
            int color = map(meanResult, getMinResult(resultsList), getMaxResult(resultsList), 0, 11);
            colorList.add(color);
        }
        return colorList;
    }

    public void resetHeatMap(){

    }

}