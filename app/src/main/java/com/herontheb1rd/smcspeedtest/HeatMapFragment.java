//HeatMapFragment.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HeatMapFragment extends Fragment implements AdapterView.OnItemSelectedListener, OnMapReadyCallback {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private ArrayList<Results> mResults = new ArrayList<>();
    private final Map<String, LatLng[]> locationDict = new HashMap<String, LatLng[]>() {{
        put("Library", new LatLng[]{new LatLng(7.08438, 125.50793), new LatLng(7.08438, 125.50808), new LatLng(7.08411, 125.50808), new LatLng(7.08411, 125.50793)});
        put("Canteen", new LatLng[]{new LatLng(7.08328, 125.50782), new LatLng(7.08328, 125.50799), new LatLng(7.08300, 125.50799), new LatLng(7.08300, 125.50781)});
        put("Kiosk", new LatLng[]{new LatLng(7.08333, 125.50791), new LatLng(7.08371, 125.50791), new LatLng(7.08371, 125.50802), new LatLng(7.08333, 125.50802)});
        put("Airport", new LatLng[]{new LatLng(7.08426, 125.50839), new LatLng(7.08456, 125.50839), new LatLng(7.08455, 125.50862), new LatLng(7.08427, 125.50862)});
        put("ABD", new LatLng[]{new LatLng(7.083256, 125.508210), new LatLng(7.083074, 125.508213), new LatLng(7.083077, 125.508327), new LatLng(7.083108, 125.508328), new LatLng(7.083109, 125.508419), new LatLng(7.083230, 125.508416), new LatLng(7.083228, 125.508328), new LatLng(7.083259, 125.508326)});
        put("Garden", new LatLng[]{new LatLng(7.08512, 125.50841), new LatLng(7.08511, 125.50860), new LatLng(7.08467, 125.50860), new LatLng(7.08468, 125.50840)});
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
    private final int[] colorGradient = {0xff66ff00, 0xff93ff00, 0xffc1ff00, 0xffeeff00, 0xfff4e300, 0xfff9c600, 0xffffaa00, 0xffff7100, 0xffff3900, 0xffff0000};

    private Map<String, Polygon> mPolygonDict = new HashMap<>();
    private Map<String, MarkerOptions> mMarkerDict = new HashMap<>();

    private static String mNetworkProvider;

    public final Map<String, String> simOperators = new HashMap<String, String>() {{
        put("51566", "DITO");
        put("51502", "Globe");
        put("51501", "Globe");
        put("51503", "Smart");
    }};

    public String getNetworkProvider() {
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);
            return simOperators.get(dataTM.getSimOperator());
        }else {
            return simOperators.get(tm.getSimOperator());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance("https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously()
                .addOnCompleteListener(getActivity(), task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Firebase authentication failed. Map will not load.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heat_map, container, false);

        mNetworkProvider = getNetworkProvider();

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
    }

    public void onItemSelected(AdapterView<?> parent, View view, int metric, long id) {getFirebaseResults(metric);}
    public void onNothingSelected(AdapterView<?> parent){resetHeatMap();}

    public BitmapDescriptor addTextOnMap(final LatLng location, final String text) {
        final int fontSize = 12;
        final int padding = 8;

        final TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(fontSize);

        final Paint paintText = textView.getPaint();

        final Rect boundsText = new Rect();
        paintText.getTextBounds(text, 0, textView.length(), boundsText);
        paintText.setTextAlign(Paint.Align.CENTER);

        final Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        final Bitmap bmpText = Bitmap.createBitmap(boundsText.width() + 2
                * 8, boundsText.height() + 2 * padding, conf);

        final Canvas canvasText = new Canvas(bmpText);
        paintText.setColor(Color.BLACK);

        canvasText.drawText(text, canvasText.getWidth() / 2,
                canvasText.getHeight() - padding - boundsText.bottom, paintText);

        return BitmapDescriptorFactory.fromBitmap(bmpText);
    }

    private void getFirebaseResults(int metric){
        if(mAuth.getCurrentUser() != null){
            //get results from database
            long twoHoursAgo = Calendar.getInstance().getTime().getTime() - (2*3600*1000);
            Query resultsRef = mDatabase.child("results").child(getNetworkProvider()).orderByChild("time").startAt(twoHoursAgo);
            resultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot networkSnapshot : dataSnapshot.getChildren()){
                        mResults.add(networkSnapshot.getValue(Results.class));
                    }
                    updateHeatMap(metric);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
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


    private void initPolygons(GoogleMap map){
        for(String s: locationDict.keySet()){
            Polygon p= map.addPolygon(new PolygonOptions()
                    .add(locationDict.get(s)));
            p.setFillColor(Color.LTGRAY);
            p.setStrokeColor(Color.LTGRAY);
            mPolygonDict.put(s, p);
        }
    }

    private void initMarkers(GoogleMap map) {
        for (String s : qrDict.keySet()) {
            MarkerOptions m = new MarkerOptions().position(qrDict.get(s));
            mMarkerDict.put(s, m);
        }
    }

    private void displayMeanResults(Map<String, Double> locationMeanMap){
        for(String l: qrDict.keySet()){
            addTextOnMap(qrDict.get(l), Double.toString(locationMeanMap.get(l)));
        }
    }

    private void updateHeatMap(int metric){
        Map<String, List<Double>> locationResultsMap = new HashMap<>();
        Map<String, Double> locationMeanMap = new HashMap<>();

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
        }

        double minResult = Double.MAX_VALUE;
        double maxResult = 0.0;

        //calculate means for each location
        //also get min and max mean
        for(String placeName: locationDict.keySet()){
            List<Double> curLocationResults = locationResultsMap.get(placeName);
            if(curLocationResults.size() != 0){
                double meanResult = getMeanResult(locationResultsMap.get(placeName));
                locationMeanMap.put(placeName, meanResult);

                if(meanResult > maxResult) maxResult = meanResult;
                if(meanResult < minResult) minResult = meanResult;
            }else{
                locationMeanMap.put(placeName, 0.0);
            }
        }

        //apply colors
        for(String placeName: locationDict.keySet()){
            double curMean = locationMeanMap.get(placeName);
            if(curMean == 0.0){
                //sets fill color to nothing and stroke color to gray
                mPolygonDict.get(placeName).setFillColor(Color.LTGRAY);
                mPolygonDict.get(placeName).setStrokeColor(Color.LTGRAY);
            }else{
                int colorIndex;
                if(metric == 2){
                    //for latency we want it to scale backwards, meaning lower is better
                    //so this reverses it linearly
                    colorIndex = scaleResult(maxResult-curMean+minResult, minResult, maxResult, 0, 9);
                }else{
                    colorIndex = scaleResult(curMean, minResult, maxResult, 0, 9);
                }
                //changes color of polygon
                mPolygonDict.get(placeName).setFillColor(colorGradient[colorIndex]);
                mPolygonDict.get(placeName).setStrokeColor(colorGradient[colorIndex]);
            }
        }

        //display mean results
        displayMeanResults(locationMeanMap);
    }

    private void resetHeatMap(){
        for(Polygon p: mPolygonDict.values()){
            //sets fill color to nothing and stroke color to transparent
            p.setFillColor(Color.TRANSPARENT);
            p.setStrokeColor(Color.TRANSPARENT);
        }
    }

}