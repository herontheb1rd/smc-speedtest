package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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

import java.util.Objects;

public class ScoreboardFragment extends Fragment {

    SharedPreferences prefs = null;
    private final TextView[] nameTVS = new TextView[7];
    private final TextView[] scoreTVS = new TextView[7];
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

        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

        displayLoading(view);
        initTVArrays(view);

        if(mAuth.getCurrentUser() != null) {
            Query resultsRef = mDatabase.child("scoreboard").orderByChild("score").limitToLast(7);
            resultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int curRank = (int) (dataSnapshot.getChildrenCount() - 1);

                    for (DataSnapshot scoreboardSnapshot : dataSnapshot.getChildren()) {
                        Player curPlayer = scoreboardSnapshot.getValue(Player.class);

                        assert curPlayer != null;
                        nameTVS[curRank].setText(curPlayer.username);
                        scoreTVS[curRank].setText(Integer.toString(curPlayer.score));

                        curRank--;
                    }
                    displayContent(view);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

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
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int rank = (int)dataSnapshot.getChildrenCount();

                for (DataSnapshot scoreboardSnapshot : dataSnapshot.getChildren()) {
                    if(Objects.equals(scoreboardSnapshot.getKey(), getUID()))
                        break;
                    rank--;
                }

                ((TextView) view.findViewById(R.id.yourRankTV)).setText(Integer.toString(rank));
                displayContent(view);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getUID() {
        return prefs.getString("UID", "");
    }
}