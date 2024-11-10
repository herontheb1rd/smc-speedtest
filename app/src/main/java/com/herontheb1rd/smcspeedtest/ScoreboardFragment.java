package com.herontheb1rd.smcspeedtest;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ScoreboardFragment extends Fragment {
    
    private TextView[] nameTVS = new TextView[7];
    private TextView[] scoreTVS = new TextView[7];
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scoreboard, container, false);

        displayLoading(view);
        initTVArrays(view);

        if(mAuth.getCurrentUser() != null) {
            Query resultsRef = mDatabase.child("scoreboard").orderByChild("score").limitToLast(7);
            resultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int curRank = 0;
                    for (DataSnapshot scoreboardSnapshot : dataSnapshot.getChildren()) {
                        Player curPlayer = scoreboardSnapshot.getValue(Player.class);

                        nameTVS[curRank].setText(curPlayer.username);
                        scoreTVS[curRank].setText(Integer.toString(curPlayer.score));

                        curRank++;
                    }
                    displayContent(view);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        getAndDisplayRank(view);

        return view;
    }

    private void initTVArrays(View view){
        nameTVS[0] = view.findViewById(R.id.firstNameTV);
        nameTVS[1] = view.findViewById(R.id.secondNameTV);
        nameTVS[2] = view.findViewById(R.id.thirdNameTV);
        nameTVS[3] = view.findViewById(R.id.fourthNameTV);
        nameTVS[4] = view.findViewById(R.id.fifthNameTV);
        nameTVS[5] = view.findViewById(R.id.sixthNameTV);
        nameTVS[6] = view.findViewById(R.id.seventhNameTV);

        scoreTVS[0] = view.findViewById(R.id.firstScoreTV);
        scoreTVS[1] = view.findViewById(R.id.secondScoreTV);
        scoreTVS[2] = view.findViewById(R.id.thirdScoreTV);
        scoreTVS[3] = view.findViewById(R.id.fourthScoreTV);
        scoreTVS[4] = view.findViewById(R.id.fifthScoreTV);
        scoreTVS[5] = view.findViewById(R.id.sixthScoreTV);
        scoreTVS[6] = view.findViewById(R.id.seventhScoreTV);
    }

    private void displayLoading(View view){
        view.findViewById(R.id.loadingGroup).setVisibility(View.VISIBLE);
        view.findViewById(R.id.contentGroup).setVisibility(View.INVISIBLE);
    }

    private void displayContent(View view){
        view.findViewById(R.id.loadingGroup).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.contentGroup).setVisibility(View.VISIBLE);
    }

    private void getAndDisplayRank(View view){
        Query scoreboardQuery = mDatabase.child("scoreboard").orderByChild("score");
        scoreboardQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int rank = (int)dataSnapshot.getChildrenCount();
                for (DataSnapshot scoreboardSnapshot : dataSnapshot.getChildren()) {
                    if(scoreboardSnapshot.getKey().equals(getUID()))
                        break;
                    rank--;
                }
                ((TextView) view.findViewById(R.id.yourRankTV)).setText(Integer.toString(rank));

                displayContent(view);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getUID() {
        //UID is the phone's Android ID
        //this removes the need for permissions for READ_PHONE_STATE
        //and is still unique to each phone
        if(getContext() != null) {
            return Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return "";
    }

}