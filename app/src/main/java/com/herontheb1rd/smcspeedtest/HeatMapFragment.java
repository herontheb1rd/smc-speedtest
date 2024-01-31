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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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
    private ArrayList<Results> mResults = new ArrayList<>();
    private final Map<String, LatLng[]> locationDict = new HashMap<String, LatLng[]>() {{
        put("Library", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Canteen", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Kiosk", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Airport", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("ABD", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
        put("Garden", new LatLng[]{new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0), new LatLng(0, 0)});
    }};

    //from here: https://gis.stackexchange.com/questions/246322/get-the-inverse-of-default-heat-map-gradient-in-google-maps-javascript-api
    //converted to rgba and then to hex
    private final int[] colorGradient = {0x0066ff000, 0xff66ff00, 0xff93ff00, 0xffc1ff00, 0xffeeff00, 0xfff4e30, 0xfff9c600, 0xffffaa00, 0xffff7100, 0xffff3900, 0xffff0000};
    private final LatLng pshsLatLng = new LatLng(7.082788894235911, 125.50813754841627);
    private final LatLng[] qrLatLng = {new LatLng(0, 0),
            new LatLng(0,0),
            new LatLng(0, 0),
            new LatLng(0, 0),
            new LatLng(0, 0),
            new LatLng(0, 0)};

    private Map<String, Polygon> mPolygonMap;

    public HeatMapFragment() {
        // Required empty public constructor
        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();

        mPolygonMap = new HashMap<>();
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                metricOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        getFirebaseResults();

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(pshsLatLng));

        for(LatLng ll: qrLatLng){
            googleMap.addMarker(new MarkerOptions().position(ll));
        }

        for(String s: locationDict.keySet()){
            Polygon polygon = googleMap.addPolygon(new PolygonOptions()
                    .add(locationDict.get(s)));
            mPolygonMap.put(s, polygon);
        }

    }

    public void onItemSelected(AdapterView<?> parent, View view, int metric, long id) { updateHeatMap(metric); }

    public void onNothingSelected(AdapterView<?> parent){
        resetHeatMap();
    }

    public void getFirebaseResults(){
        //get results from database
        long before24hours = new Date().getTime() - (24 * 3600 * 1000);
        Query timeQuery = mDatabase.child("results").orderByChild("date_time")
                .startAt(before24hours);
        timeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    mResults.add(singleSnapshot.getValue(Results.class));
                }
                TextView dataTV = (TextView) getActivity().findViewById(R.id.dataTV);
                dataTV.setText(Long.toString(mResults.get(0).getTime()));
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Database error", "onCancelled", databaseError.toException());
            }
        });
    }
    //arduino's map function: https://www.arduino.cc/reference/en/language/functions/math/map/
    //used to scale the results to the color values on a heat map
    private int scaleResult(double x, double in_min, double in_max, double out_min, double out_max) {
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

    private void updateHeatMap(int metric){
        Map<String, List<Double>> resultsMap = new HashMap<>();
        //initialize hash map
        for(String placeName: locationDict.keySet()){
            resultsMap.put(placeName, new ArrayList<Double>());
        }

        //place values in hash map
        for (Results curResult : mResults) {
            double intensity = 0;
            String placeName = curResult.getPlace().getPlaceName();
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
            }
            //skip if values are invalid
            if (intensity == -1) continue;

            resultsMap.get(placeName).add(intensity);
        }

        //apply colors from values
        for(String placeName: locationDict.keySet()){
            double meanResult = getMeanResult(resultsMap.get(placeName));
            double minResult = getMinResult(resultsMap.get(placeName));
            double maxResult = getMaxResult(resultsMap.get(placeName));
            //chooses color from meanResult
            int colorIndex = scaleResult(meanResult, minResult, maxResult, 0, 11);

            //changes color of polygon
            mPolygonMap.get(placeName).setFillColor(colorGradient[colorIndex]);
            mPolygonMap.get(placeName).setStrokeColor(colorGradient[colorIndex]);
        }
    }

    public void resetHeatMap(){
        for(Polygon p: mPolygonMap.values()){
            //sets fill color to nothing and stroke color to black
            p.setFillColor(0x00000000);
            p.setStrokeColor(0xff000000);
        }
    }

}