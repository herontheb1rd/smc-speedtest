//ResultsFragment.java
//Code by Heron Nalasa

package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;


import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.ComputationMethod;

public class ResultsFragment extends Fragment {

    SharedPreferences prefs = null;

    //private final List<String> qrLocations = Arrays.asList("Library", "Canteen", "Kiosk", "Airport", "ABD", "Garden");

    //codes from https://mcc-mnc.com/
    public final Map<String, String> simOperators = new HashMap<String, String>() {{
        put("51566", "DITO");
        put("51502", "Globe");
        put("51501", "Globe");
        put("51503", "Smart");
    }};

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private OnBackPressedCallback callback;
    private String mPlace;

    final private HashMap<ServerInfo, Long> serverLatencyMap = new HashMap<>();
    final CountDownLatch findServerLatch = new CountDownLatch(10);
    public Context mContext;


    @Override
    public void onStart() {
        super.onStart();

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

        mDatabase = FirebaseDatabase.getInstance(
                "https://smc-speedtest-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference();
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously()
                .addOnCompleteListener(getActivity(), task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Firebase authentication failed. Can't upload results",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        //check if user has data in scoreboard
        //if not, set default values
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String UID = getUID();
                if(!snapshot.child("scoreboard").child(UID).exists()){
                    mDatabase.child("scoreboard").child(UID).child("username").setValue(prefs.getString("username", getUID()));
                    mDatabase.child("scoreboard").child(UID).child("score").setValue(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

         callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(getActivity(), "Test still running",
                        Toast.LENGTH_SHORT).show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(getContext());
        mContext = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        displayProgress(view, "Finding best server");

        getParentFragmentManager().setFragmentResultListener("requestKey", this, (requestKey, bundle) -> {
             mPlace = bundle.getString("bundleKey");

            ExecutorService executor = Executors.newFixedThreadPool(8);
            executor.submit(() -> {
                try {
                    startSpeedTest(view, getContext(), getServerList());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        return view;
    }


    public ServerInfo[] getServerList(){
        ServerInfo[] serverList = new ServerInfo[10];

        URL url = null;
        try {
            url = new URL("https://c.speedtest.net/speedtest-servers-static.php");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;

        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(url.openStream());
            doc.getDocumentElement().normalize();

            NodeList xmlServerList = doc.getElementsByTagName("server");

            for(int i = 0; i < xmlServerList.getLength(); i++){
                Node xmlServer = xmlServerList.item(i);
                if(xmlServer.getNodeType() == Node.ELEMENT_NODE){
                    Element serverElement = (Element) xmlServer;

                    String rawUrl = serverElement.getAttribute("url");
                    String rawHost = serverElement.getAttribute("host");
                    String name = serverElement.getAttribute("name");

                    Log.i("test", rawUrl);

                    ServerInfo serverInfo = new ServerInfo(rawUrl, rawHost, name);
                    serverList[i] = serverInfo;
                }
            }
        } catch (SAXException | IOException |
                 ParserConfigurationException e) {
            throw new RuntimeException(e);

        }

        return serverList;
    }

    public static Network getNetwork(final Context context, final int transport) {
        final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        for (Network network : connManager.getAllNetworks()) {
            NetworkCapabilities networkCapabilities = connManager.getNetworkCapabilities(network);
            if (networkCapabilities != null &&
                    networkCapabilities.hasTransport(transport) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return network;
            }
        }
        return null;
    }

    public static InetAddress getInetAddress(final String host, Class<? extends InetAddress> inetClass) throws UnknownHostException {
        final InetAddress[] inetAddresses = InetAddress.getAllByName(host);
        InetAddress dest = null;
        for (final InetAddress inetAddress : inetAddresses) {
            if (inetClass.equals(inetAddress.getClass())) {
                return inetAddress;
            }
        }
        throw new UnknownHostException("Could not find IP address of type " + inetClass.getSimpleName());
    }

    public void getQuickLatency(Context context, ServerInfo server){
        Log.i("test", "getting latency of " + server.latencyUrl);
        final Class<? extends InetAddress> inetClass = Inet4Address.class;
        final InetAddress dest;
        try {
            dest = getInetAddress(server.latencyUrl, inetClass);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        Network network = getNetwork(context,  NetworkCapabilities.TRANSPORT_CELLULAR);
        //Network network = getNetwork(context, NetworkCapabilities.TRANSPORT_WIFI);
        if (network == null)
            try {
                throw new UnknownHostException("Failed to establish network connection");
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

        Ping ping = new Ping(dest, new Ping.PingListener() {
            @Override
            public void onPing(long timeMs, int index) {
                Log.i("test", server.latencyUrl + " is done with latency" + timeMs);
                serverLatencyMap.put(server, timeMs);
                findServerLatch.countDown();
            }

            @Override
            public void onPingException(Exception e, int count) {

            }
        });

        ping.setNetwork(network);
        ping.setCount(1);
        ping.run();
    }

    public ServerInfo findBestServer(Context context, ServerInfo[] serverList) throws InterruptedException {
        Log.i("test", "findgin best server");
        for(int i = 0; i < 10; i++){
            Log.i("test", "i is " + i);
            final int curIndex = i;
            Thread t = new Thread(){
                public void run(){
                    getQuickLatency(context, serverList[curIndex]);
                }
            };
            t.start();

        }

        findServerLatch.await();

        Log.i("test", "all done");

        ServerInfo bestServer = (ServerInfo) serverLatencyMap.keySet().toArray()[0];
        long minLatency = (long) serverLatencyMap.values().toArray()[0];
        boolean hasDavaoServer = false;
        for(ServerInfo s: serverLatencyMap.keySet()){
            long curLatency = serverLatencyMap.get(s);
            String curName = s.name;

            if (curLatency == -1)
                    continue;

            //prioritize servers in Davao City
            //since their latency doesn't differ as much
            //and we're likely to get better results.
            //It also helps that servers within the same city are grouped together in the PHP file
            if(curName.equals("Davao City") && !hasDavaoServer){
                //if we're looking at a server within Davao and the previous wasn't, immediately use this one
                minLatency = curLatency;
                bestServer = s;
                hasDavaoServer = true;
            }else if(!curName.equals("Davao City") && hasDavaoServer){
                //if we're looking at a server outside Davao and the previous was, skip
                continue;
            }else if(minLatency == -1 || curLatency < minLatency){
                minLatency = curLatency;
                bestServer = s;
            }
        }

        Log.i("test", "best server is" + bestServer.latencyUrl);
        return bestServer;
    }

    public long getMedianLatency(ArrayList<Long> latencyResults){
        Collections.sort(latencyResults);
        int arrLen = latencyResults.size();
        if(arrLen % 2 == 0)
            return (latencyResults.get(arrLen / 2 - 1) + latencyResults.get(arrLen / 2))/2;
        else
            return latencyResults.get(arrLen / 2);
    }


    public void startSpeedTest(View view, final Context context, ServerInfo[] serverList) throws InterruptedException {
        final ServerInfo server = findBestServer(context, serverList);
        displayProgress(view, "Connected to ".concat(server.latencyUrl) + ". Computing latency");
        getLatency(view, context, server);
    }

    public void getLatency(View view, final Context context, ServerInfo server){
        int MAX_COUNT = 8;

        final Class<? extends InetAddress> inetClass = Inet4Address.class;
        final InetAddress dest;
        try {
            dest = getInetAddress(server.latencyUrl, inetClass);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        Network network = getNetwork(context,  NetworkCapabilities.TRANSPORT_CELLULAR);
        //Network network = getNetwork(context, NetworkCapabilities.TRANSPORT_WIFI);
        if (network == null)
            try {
                throw new UnknownHostException("Failed to establish network connection");
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

        ArrayList<Long> latencyResults = new ArrayList<>();
        Ping ping = new Ping(dest, new Ping.PingListener() {
            @Override
            public void onPing(long timeMs, int index) {
                int MAX_THRESH = 200;
                if(timeMs < MAX_THRESH)
                    latencyResults.add(timeMs);

                displayLatencyResult(view, (int) timeMs, false);
                Log.i("test", Long.toString(timeMs));
                Log.i("test", Integer.toString(index));
                if(index == MAX_COUNT-1){
                    int medianLatency = (int) getMedianLatency(latencyResults);
                    NetPerf netPerf = new NetPerf();
                    netPerf.setLatency(medianLatency);
                    displayLatencyResult(view, medianLatency, true);

                    getDownloadSpeed(view, server, netPerf);
                }
            }

            @Override
            public void onPingException(Exception e, int count) {
                int medianLatency = -1;
                NetPerf netPerf = new NetPerf();
                netPerf.setLatency(medianLatency);
                displayLatencyResult(view, medianLatency, true);

                getDownloadSpeed(view, server, netPerf);
            }
        });
        ping.setDelayMs(50);
        ping.setCount(MAX_COUNT);
        ping.setNetwork(network);
        ping.run();
    }

    public void getDownloadSpeed(View view, final ServerInfo server, NetPerf netPerf){
        displayProgress(view, "Computing download speed");

        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                // called when download/upload is complete
                double dlspeed = report.getTransferRateBit().doubleValue()/1e6;
                displayDownloadResult(view, dlspeed, true);
                netPerf.setDlspeed(dlspeed);
                Log.i("test", "[COMPLETED] rate in Mbit/s   : " + dlspeed);

                getUploadSpeed(view, server, netPerf);
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download/upload error occur
                Log.i("test - ste", speedTestError.name());
                Log.i("test - em", errorMessage);

                double dlspeed = -1;
                displayDownloadResult(view, dlspeed, false);
                netPerf.setDlspeed(dlspeed);
                getUploadSpeed(view, server, netPerf);
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                double dlspeed = report.getTransferRateBit().doubleValue()/1e6;
                displayDownloadResult(view, dlspeed, false);

                String percentStr = String.format("%.0f", percent);
                displayProgress("Computing download speed - " + percentStr + "%");


                Log.i("test", "[PROGRESS] rate in Mbit/s   : " + dlspeed);
                Log.i("test", "progress " + percent);

            }
        });
        speedTestSocket.startFixedDownload(server.downloadUrl, 10000);
    }

    public Double getMedianUlspeed(ArrayList<Double> uploadSpeedList){
        Double medianUlspeed;

        int listSize = uploadSpeedList.size();

        if(listSize % 2 == 0){
            medianUlspeed = (uploadSpeedList.get(listSize/2-1) + uploadSpeedList.get(listSize/2))/2;
        }else{
            medianUlspeed = uploadSpeedList.get(listSize/2);
        }

        return medianUlspeed;
    }

    public void getUploadSpeed(View view, final ServerInfo server, NetPerf netPerf){
        displayProgress(view, "Computing upload speed");

        SpeedTestSocket speedTestSocket = new SpeedTestSocket();
        ArrayList<Double> tempData = new ArrayList<>();

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                /*
                // called when download/upload is complete
                double ulspeed = report.getTransferRateBit().doubleValue()/1e6;
                displayUploadResult(view, ulspeed, true);
                netPerf.setUlspeed(ulspeed);

                uploadResults(view, netPerf);

                Log.i("test", "[COMPLETED] rate in Mbit/s   : " + ulspeed);

                 */
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download/upload error occur
                Log.i("test - ste", speedTestError.name());
                Log.i("test - em", errorMessage);

                double ulspeed = getMedianUlspeed(tempData);
                Log.i("test", "temp " + ulspeed);
                displayUploadResult(view, ulspeed, true);
                netPerf.setUlspeed(ulspeed);

                uploadResults(view, netPerf);
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                double ulspeed = report.getTransferRateBit().doubleValue()/1e6;
                tempData.add(ulspeed);
                String percentStr = String.format("%.0f", percent);
                displayUploadResult(view, ulspeed,false);
                displayProgress("Computing upload speed - " + percentStr + "%");
                Log.i("test", "[PROGRESS] rate in Mbit/s   : " + ulspeed);
                Log.i("test", "progress " + percent);

                if(percent == 100f){
                    double medianUlspeed = getMedianUlspeed(tempData);
                    displayUploadResult(view, medianUlspeed, true);
                    netPerf.setUlspeed(medianUlspeed);
                    uploadResults(view, netPerf);
                    Log.i("test", "[CFAKE OMPLETED] rate in Mbit/s   : " + medianUlspeed);
                }
            }
        });

        Log.i("test", server.uploadUrl);
        speedTestSocket.setSocketTimeout(5000);
        speedTestSocket.setUploadChunkSize(1024);
        speedTestSocket.startFixedUpload(server.uploadUrl, 250000, 10000);
    }

    public void uploadResults(View view, NetPerf netPerf){
        displayProgress(view, "Uploading results");

        long time = Calendar.getInstance().getTime().getTime();
        String phoneBrand = Build.MANUFACTURER;
        String networkProvider = getNetworkProvider();
        String UID = getUID();
        SignalPerf signalPerf = computeSignalPerf();


        if(mAuth.getCurrentUser() != null){
            Results results = new Results(time, phoneBrand, networkProvider, mPlace, netPerf, signalPerf, UID);
            mDatabase.child("results").child(networkProvider).push().setValue(results);

            mDatabase.child("scoreboard").child(UID).child("username").setValue(prefs.getString("username", getUID()));
            mDatabase.child("scoreboard").child(UID).child("score").setValue(ServerValue.increment(1));

            //findBetterLocation(view, networkProvider, place, netPerf);
        }

        callback.setEnabled(false);

        displayProgress("Test complete");
    }

    private String getUID() {
        return prefs.getString("UID", "");
    }

    public String getNetworkProvider() {

        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);
            return simOperators.getOrDefault(dataTM.getSimOperator(), "Other");
        }else {
            String simOperator = tm.getSimOperator();
            if(!simOperators.containsKey(simOperator)){
                return "Other";
            }else{
                return simOperators.get(simOperator);

            }
        }
    }

    private SignalPerf computeSignalPerf(){
        int rssi = 1;
        int rsrp = 1;
        int rsrq = 1;

        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return new SignalPerf(rssi, rsrq, rsrp);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return new SignalPerf(rssi, rsrq, rsrp);
        }
        LocationManager lm = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return new SignalPerf(rssi, rsrq, rsrp);
        }

        int dataSubId = SubscriptionManager.getActiveDataSubscriptionId();
        TelephonyManager dataTM = tm.createForSubscriptionId(dataSubId);

        CellInfoLte cellinfolte = (CellInfoLte) dataTM.getAllCellInfo().get(0);
        CellSignalStrengthLte cellSignalStrengthLte = cellinfolte.getCellSignalStrength();

        rssi = cellSignalStrengthLte.getRssi();
        rsrp = cellSignalStrengthLte.getRsrp();
        rsrq = cellSignalStrengthLte.getRsrq();

        return new SignalPerf(rssi, rsrq, rsrp);
    }

    public void displayDownloadResult(View view, double dlspeed, boolean isFinal){
        if(view == null)
            return;

        TextView downloadTV = view.findViewById(R.id.downloadSpeedTV);

        String displayStr;
        if(isFinal) {
            downloadTV.setAlpha(1f);
            displayStr = String.format("%.1f", dlspeed).concat(" Mbps");
        }
        else {
            displayStr = String.format("%.3f", dlspeed);
            downloadTV.setAlpha(0.4f);
        }

        displayResult(view, R.id.downloadSpeedTV, (dlspeed != -1d && dlspeed != 0d) ? displayStr: "N/A");

        downloadTV.setVisibility(View.VISIBLE);
        view.findViewById(R.id.downloadPB).setVisibility(View.INVISIBLE);
    }

    public void displayUploadResult(View view, double ulspeed, boolean isFinal){
        if(view == null)
            return;

        TextView uploadTV = view.findViewById(R.id.uploadSpeedTV);

        String displayStr;
        if(isFinal) {
            uploadTV.setAlpha(1f);
            displayStr = String.format("%.1f", ulspeed).concat(" Mbps");
        }
        else {
            displayStr = String.format("%.3f", ulspeed);
            uploadTV.setAlpha(0.4f);
        }
        displayResult(view, R.id.uploadSpeedTV, (ulspeed != -1d && ulspeed != 0d) ? displayStr : "N/A");
        uploadTV.setVisibility(View.VISIBLE);
        view.findViewById(R.id.uploadPB).setVisibility(View.INVISIBLE);
    }

    public void displayLatencyResult(View view, int latency, boolean isFinal){
        if(view == null)
            return;

        TextView latencyTV = view.findViewById(R.id.latencyTV);

        String displayStr = Integer.toString(latency);
        if(isFinal) {
            latencyTV.setAlpha(1f);
            displayStr = displayStr.concat(" ms");
        }
        else {
            latencyTV.setAlpha(0.4f);
        }

        displayResult(view, R.id.latencyTV, (latency != -1 && latency != 0) ? displayStr : "N/A");
        latencyTV.setVisibility(View.VISIBLE);
        view.findViewById(R.id.latencyPB).setVisibility(View.INVISIBLE);
    }

    public void displayProgress(View view, String progress) {
        if (view == null)
            return;

        Log.i("test", progress);
        displayResult(view, R.id.progressTV, progress);
    }

    public void displayProgress(String progress) {
        View view = getView();

        if (view == null)
            return;

        Log.i("test", progress);
        displayResult(view, R.id.progressTV, progress);
    }

    public void displayResult(View view, int id, String resultStr){
        getActivity().runOnUiThread(() -> ((TextView) view.findViewById(id)).setText(resultStr));
    }

    /*
    * Might reimplement this with more users, but for now it doesn't really do much
    * It might actually hurt getting results, and
    * we don't have enough data for it to be very useful
    * If we had more users, then it might have a usecase
    * Keeping it here anyway
    private double getMeanPerformance(List<NetPerf> resultList){
        //"performance" is dlspeed * ulspeed / latency
        //there might be better methods but this is a lazy way of implementing it i suppose
        //because out goal is higher dlspeed and ulspeed but lower latency

        int resultsSize = resultList.size();

        if(resultsSize == 0){
            return 0.0;
        }

        double dlspeedSum = 0;
        double ulspeedSum = 0;
        double latencySum = 0;
        for(NetPerf n: resultList){
            dlspeedSum += n.getDlspeed();
            ulspeedSum += n.getUlspeed();
            latencySum += n.getLatency();
        }

        double meanPerformance = dlspeedSum/resultsSize * ulspeedSum/resultsSize / latencySum/resultsSize;
        return meanPerformance;
    }

    private String compareLocations(Map<String, Double> dict, NetPerf netPerf){
        String betterLocation = "None";

        double maxValue = netPerf.getDlspeed() * netPerf.getUlspeed() / netPerf.getLatency();

        for(String l: dict.keySet()){
            if(dict.get(l) > maxValue){
                betterLocation = l;
                maxValue = dict.get(l);
            }
        }

        return betterLocation;
    }
    private void findBetterLocation(View view, String networkProvider, String currentLocation, NetPerf netPerf){
        Map<String, List<NetPerf>> locationResultsDict = new HashMap<>();
        Map<String, Double> locationPerformance = new HashMap<>();

        for(String l: qrLocations){
            locationResultsDict.put(l, new ArrayList<>());
            locationPerformance.put(l, 0.0);
        }

        //get results from database
        int hours = 24;
        long timeLimit = Calendar.getInstance().getTime().getTime() - (hours*3600*1000);
        Query resultsQuery = mDatabase.child("results").child(networkProvider).orderByChild("time").startAt(timeLimit);
        resultsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot networkSnapshot : dataSnapshot.getChildren()) {
                    Results curResult = networkSnapshot.getValue(Results.class);

                    locationResultsDict.get(curResult.getPlace()).add(curResult.getNetPerf());
                }

                for (String l : locationResultsDict.keySet()) {
                    locationPerformance.put(l, getMeanPerformance(locationResultsDict.get(l)));
                }

                String betterLocation = compareLocations(locationPerformance, netPerf);
                if (betterLocation.equals("None") || betterLocation.contains(currentLocation)) {
                    betterLocation = "This location has (probably) the best connection available";
                }else{
                    betterLocation = "The " + betterLocation + " (probably) has a better connection";
                }

                displayResult(view, R.id.suggestionTV, betterLocation);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/
}