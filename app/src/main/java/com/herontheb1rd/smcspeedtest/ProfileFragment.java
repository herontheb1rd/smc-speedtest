package com.herontheb1rd.smcspeedtest;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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


public class ProfileFragment extends Fragment {

    SharedPreferences prefs = null;

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
        prefs = getActivity().getSharedPreferences("com.herontheb1rd.smcspeedtest", MODE_PRIVATE);

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        displayLoading(view);
        getNameAndScore(view);
        getAndDisplayRank(view);

        Button changeNameButton = view.findViewById(R.id.changeNameB);
        changeNameButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.UsernameAlertStyle);
            builder.setTitle("Set Username");

            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
            builder.setMessage("\nMaximum of 20 characters");
            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String username = input.getText().toString();
                changeUsername(view, username);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void getNameAndScore(View view){
        if(mAuth.getCurrentUser() != null) {
            Query resultsRef = mDatabase.child("scoreboard");
            resultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String UID = getUID();

                    if(!snapshot.child(UID).exists()){
                        mDatabase.child("scoreboard").child(UID).child("username").setValue(prefs.getString("username", getUID()));
                        mDatabase.child("scoreboard").child(UID).child("score").setValue(0);
                    }else {
                        ((TextView) view.findViewById(R.id.usernameTV)).setText(prefs.getString("username", snapshot.child("username").getValue().toString()));
                        ((TextView) view.findViewById(R.id.scoreTV)).setText(snapshot.child("score").getValue().toString());
                    }
                    displayContent(view);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }
    private void getAndDisplayRank(View view){
        Query scoreboardQuery = mDatabase.child("scoreboard").orderByChild("score");
        scoreboardQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int rank = (int)dataSnapshot.getChildrenCount();
                for (DataSnapshot scoreboardSnapshot : dataSnapshot.getChildren()) {
                    if(Objects.equals(scoreboardSnapshot.getKey(), getUID()))
                        break;
                    rank--;
                }
                ((TextView) view.findViewById(R.id.rankTV)).setText("You are rank " + rank);

                displayContent(view);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void changeUsername(View view, String username){
        if(mAuth.getCurrentUser() != null){
            mDatabase.child("scoreboard").child(getUID()).child("username").setValue(username);
            prefs.edit().putString("username", username).apply();
            ((TextView) view.findViewById(R.id.usernameTV)).setText(username);
        }else{
            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Could not upload results to database",
                    Toast.LENGTH_SHORT).show());
        }
    }
    private String getUID() {
        return prefs.getString("UID", "");
    }

    private void displayLoading(View view){
        view.findViewById(R.id.loadingGroup).setVisibility(View.VISIBLE);
        view.findViewById(R.id.contentGroup).setVisibility(View.INVISIBLE);
    }

    private void displayContent(View view){
        view.findViewById(R.id.loadingGroup).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.contentGroup).setVisibility(View.VISIBLE);
    }
}