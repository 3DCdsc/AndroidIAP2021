package com.example.virtualpets;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

public class ChoosePetActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_pet2);

        // Set references to Images
        ImageView catImage = findViewById(R.id.catImage);
        ImageView dogImage = findViewById(R.id.dogImage);
        ImageView fishImage = findViewById(R.id.fishImage);

        // Get a reference to SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        catImage.setOnClickListener(view -> {
            sharedPref.edit().putString("pet2", "cat").apply();
            goNextActivity();
        });

        dogImage.setOnClickListener(view -> {
            sharedPref.edit().putString("pet2", "dog").apply();
            goNextActivity();
        });

        fishImage.setOnClickListener(view -> {
            sharedPref.edit().putString("pet2", "fish").apply();
            goNextActivity();
        });


    }

    private void goNextActivity() {
        Intent intent = new Intent(this, MyPetActivity.class);
        startActivity(intent);
    }
}