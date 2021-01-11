package com.example.virtualpets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());
    private static final int RC_SIGN_IN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("MainActivity", "Hello World");

        Button button = findViewById(R.id.startButton);

        button.setOnClickListener(view -> {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);


        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // Asserts that this result came from our FirebaseUI

            if (resultCode == RESULT_OK) {
                // Means the user successfully signed in
                // We can now get the details of the Firebase User
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // Get the name of the user
                String userName = user.getDisplayName();

                // Show a message welcoming the user
                Toast.makeText(this, "Welcome " + userName, Toast.LENGTH_SHORT).show();

                // Get Signed-in user's id
                String userId = user.getUid();
                // Get Reference to database
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                // Get Reference to where we expect user's data to be
                DatabaseReference mUser = mDatabase.child("pets_database").child(userId);
                // Use this function to attempt to read data from our reference
                mUser.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Check whether user has does indeed have existing pet data
                        if (snapshot.exists()) {
                            // If data exists, we want to extract the pet types and hunger values
                            // And store them into sharedPreferences

                            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                            sharedPref.edit()
                                    .putString("pet1", snapshot.child("pet1").child("type").getValue(String.class))
                                    .putInt("pet1hunger", snapshot.child("pet1").child("hunger").getValue(int.class))
                                    .putString("pet2", snapshot.child("pet2").child("type").getValue(String.class))
                                    .putInt("pet2hunger", snapshot.child("pet2").child("hunger").getValue(int.class))
                                    .apply();

                            // Go to MyPetActivity
                            Intent intent = new Intent(MainActivity.this, MyPetActivity.class);
                            startActivity(intent);
                        } else {
                            // Go to ChoosePetActivity
                            Intent intent = new Intent(MainActivity.this, ChoosePetActivity1.class);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            } else {
                Toast.makeText(this, "Sign in Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}