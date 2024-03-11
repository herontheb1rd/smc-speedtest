package com.herontheb1rd.smcspeedtest;

import android.graphics.Color;
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
import com.google.android.gms.maps.model.Marker;
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
        put("Library", new LatLng[]{new LatLng(7.08438, 125.50793), new LatLng(7.08438, 125.50793), new LatLng(7.08438, 125.50793), new LatLng(7.08438, 125.50793)});
        put("Canteen", new LatLng[]{new LatLng(7.08328, 125.50782), new LatLng(7.08328, 125.50799), new LatLng(7.08300, 125.50799), new LatLng(7.08300, 125.50781)});
        put("Kiosk", new LatLng[]{new LatLng(7.08333, 125.50791), new LatLng(7.08371, 125.50793), new LatLng(7.08369, 125.50802), new LatLng(7.08332, 125.50802)});
        put("Airport", new LatLng[]{new LatLng(7.08426, 125.50839), new LatLng(7.08427, 125.50862), new LatLng(7.08456, 125.50839), new LatLng(7.08455, 125.50862)});
        put("ABD", new LatLng[]{new LatLng(7.083256, 125.508210), new LatLng(7.083074, 125.508213), new LatLng(7.083077, 125.508327), new LatLng(7.083108, 125.508328), new LatLng(7.083109, 125.508419), new LatLng(7.083230, 125.508416), new LatLng(7.083228, 125.508328), new LatLng(7.083259, 125.508326)});
        put("Garden", new LatLng[]{new LatLng(7.0846667, 125.508603), new LatLng(7.084671, 125.508401), new LatLng(7.084882, 125.508299), new LatLng(7.084811, 125.508374), new LatLng(7.085043, 125.508291), new LatLng(7.085232, 125.508494), new LatLng(7.085111, 125.508611)});
    }};

    private final Map<String, LatLng> qrDict = new HashMap<String, LatLng>(){{
        put("Library", new LatLng(7.08424, 125.50799));
        put("Canteen", new LatLng( 7.08314, 125.50790));
        put("Kiosk", new LatLng( 7.08350, 125.50799));
        put("Airport", new LatLng( 7.08440, 125.50852));
        put("ABD", new LatLng(7.08161, 125.508311));
        put("Garden", new LatLng(7.084942, 125.508449));
    }};

    //from here: https://gis.stackexchange.com/questions/246322/get-the-inverse-of-default-heat-map-gradient-in-google-maps-javascript-api
    //converted to rgba and then to hex
    private final int[] colorGradient = {0x0066ff000, 0xff66ff00, 0xff93ff00, 0xffc1ff00, 0xffeeff00, 0xfff4e300, 0xfff9c600, 0xffffaa00, 0xffff7100, 0xffff3900, 0xffff0000};
    private final LatLng pshsLatLng = new LatLng(7.082788894235911, 125.50813754841627);

    private Map<String, Polygon> mPolygonMap = new HashMap<>();
    private static String mNetworkProvider;

    public HeatMapFragment() {
        ResultsFragment r = new ResultsFragment();
        mNetworkProvider = r.getNetworkProvider();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance("https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heat_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //initialize spinner
        Spinner spinner = (Spinner)view.findViewById(R.id.metricSpinner);
        String[] metricOptions = {"Download Speed", "Upload Speed", "Latency"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                metricOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        return view;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        initPolygons(googleMap);
        initMarkers(googleMap);

        mPolygonMap.get("Canteen").setStrokeColor(Color.TRANSPARENT);
        mPolygonMap.get("Canteen").setFillColor(colorGradient[5]);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int metric, long id) {}
    public void onNothingSelected(AdapterView<?> parent){}

    private void getFirebaseResults(int metric){
        //get results from database
        Query resultsRef = mDatabase.child("results").orderByChild("networkProvider").equalTo(mNetworkProvider);
        resultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    mResults.add(singleSnapshot.getValue(Results.class));
                }
                updateHeatMap(metric);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

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

    private void initMarkers(GoogleMap map){
        for(String s: qrDict.keySet()){
            Marker m = map.addMarker(new MarkerOptions()
                    .position(qrDict.get(s))
                    .title(s));
            m.showInfoWindow();
        }
    }

    private void initPolygons(GoogleMap map){
        for(String s: locationDict.keySet()){
            Polygon polygon = map.addPolygon(new PolygonOptions()
                    .add(locationDict.get(s))
                    .fillColor(Color.TRANSPARENT)
                    .strokeColor(Color.GRAY)
                    .strokeWidth(3));
            mPolygonMap.put(s, polygon);
        }
        
    }

    private void updateHeatMap(int metric){
        Map<String, List<Double>> locationResultsMap = new HashMap<>();
        ArrayList<Double> resultsList = new ArrayList<>();

        //initialize hash map
        for(String placeName: locationDict.keySet()){
            locationResultsMap.put(placeName, new ArrayList<Double>());
        }


        //place values in hash map
        for (Results curResult : mResults) {
            double intensity = 0;
            String placeName = curResult.getPlace();
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

            locationResultsMap.get(placeName).add(intensity);
            resultsList.add(intensity);
        }

        double minResult = getMinResult(resultsList);
        double maxResult = getMaxResult(resultsList);
        //apply colors from values
        for(String placeName: locationDict.keySet()){
            List<Double> curLocationResults = locationResultsMap.get(placeName);
            if(curLocationResults.size() != 0){
                double meanResult = getMeanResult(locationResultsMap.get(placeName));
                //chooses color from meanResult
                int colorIndex = scaleResult(meanResult, minResult, maxResult, 0, 11);

                //changes color of polygon
                //mPolygonMap.get(placeName).setFillColor(colorGradient[colorIndex]);
                //mPolygonMap.get(placeName).setStrokeColor(colorGradient[colorIndex]);
            }else{
                Log.i("TEST", placeName+" is empty");

                //sets fill color to nothing and stroke color to black
                //mPolygonMap.get(placeName).setFillColor(0x00000000);
                //mPolygonMap.get(placeName).setStrokeColor(0xff000000);
            }
        }
    }

    private void resetHeatMap(){
        for(Polygon p: mPolygonMap.values()){
            //sets fill color to nothing and stroke color to black
        }
    }

}