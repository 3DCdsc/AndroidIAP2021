package com.example.virtualpets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyPetActivity extends AppCompatActivity {
    DatabaseReference mDatabase;
    DatabaseReference mUserRef;
    private final static int RC_PICK_IMAGE_1 = 1;
    private final static int RC_PICK_IMAGE_2 = 2;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pet);

        // Set references to widgets in layout file
        ImageView pet1Image = findViewById(R.id.pet1Image);
        TextView pet1HungerText = findViewById(R.id.pet1HungerText);
        Button pet1Button = findViewById(R.id.pet1Button);

        ImageView pet2Image = findViewById(R.id.pet2Image);
        TextView pet2HungerText = findViewById(R.id.pet2HungerText);
        Button pet2Button = findViewById(R.id.pet2Button);

        EditText emailText = findViewById(R.id.emailText);
        Button visitButton = findViewById(R.id.visitButton);
        Button signOutButton = findViewById(R.id.signOutButton);


        // Get copy of sharedpreferences
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // Get Pet1, default to cat
        String pet1 = sharedPref.getString("pet1", "cat");
        int pet1HungerValue = sharedPref.getInt("pet1hunger", 100);

        // Get Pet2, default to dog
        String pet2 = sharedPref.getString("pet2", "dog");
        int pet2HungerValue = sharedPref.getInt("pet2hunger", 100);

        // Get a reference to our Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get our user's email
        String userEmail = sharedPref.getString("email", ".").replace(".", ",");

        // Get reference to our user's pet data
        mUserRef = mDatabase.child("pets_database").child(userEmail);

        mUserRef.child("pet1").child("type").setValue(pet1);
        mUserRef.child("pet1").child("hunger").setValue(pet1HungerValue);
        mUserRef.child("pet2").child("type").setValue(pet2);
        mUserRef.child("pet2").child("hunger").setValue(pet2HungerValue);

        initialiseWidgetsForPet("pet1", pet1Image, RC_PICK_IMAGE_1, pet1HungerText, pet1Button, pet1, pet1HungerValue);
        initialiseWidgetsForPet("pet2", pet2Image, RC_PICK_IMAGE_2, pet2HungerText, pet2Button, pet2, pet2HungerValue);


        visitButton.setOnClickListener(view -> {
            sharedPref.edit().putString("email", emailText.getText().toString()).apply();
            Intent intent = new Intent(MyPetActivity.this, MyPetActivity.class);
            startActivity(intent);
            finish();
        });

        signOutButton.setOnClickListener(view -> {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        public void onComplete(@NonNull Task<Void> task) {
                            Intent intent = new Intent(MyPetActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
        });
    }

    private void initialiseWidgetsForPet(String petId, ImageView petImage,
                                         int imageRC, TextView hungerText,
                                         Button petButton, String pet, int petHungerValue) {
        // This function runs for each pet we have

        petImage.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), imageRC);
        });

        // Set the image based on which pet it is
        if (pet.equals("cat")) {
            petImage.setImageResource(R.drawable.cat);
        } else if (pet.equals("dog")) {
            petImage.setImageResource(R.drawable.dog);
        } else {    // Else it probably is a fish
            petImage.setImageResource(R.drawable.fish);
        }

        hungerText.setText(Integer.toString(petHungerValue));

        mUserRef.child(petId).child("hunger").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hungerText.setText(Integer.toString(snapshot.getValue(int.class)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mUserRef.child(petId).child("imageUrl").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imageUrlString = snapshot.getValue(String.class);
                    setBitmapFromURL(imageUrlString, petImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Set onclicklistener to increment hunger by 1 for each button click
        petButton.setOnClickListener(view -> {
            int oldHunger = Integer.parseInt(hungerText.getText().toString());
            hungerText.setText(Integer.toString(oldHunger + 1));
            mUserRef.child(petId).child("hunger").setValue(oldHunger + 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PICK_IMAGE_1 || requestCode == RC_PICK_IMAGE_2) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", ",");
                StorageReference userStorageRef = storageRef.child(userEmail).child(Integer.toString(requestCode));
                userStorageRef.putStream(inputStream)
                    .continueWithTask(taskSnapshot -> {
                        return userStorageRef.getDownloadUrl();
                    }).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String imageUrl = task.getResult().toString();
                            mUserRef.child("pet" + requestCode).child("imageUrl").setValue(imageUrl);
                        }
                    });
            } catch (FileNotFoundException ex) {
            }
        }
    }

    /**
     * Generic function to set an ImageView from a given url
     */
    private void setBitmapFromURL(String imageString, ImageView imageView) {
        new Thread(() -> {
            // We will need to download the image, on a separate thread/process to avoid hanging our UI
            try {
                // We use try/catch since our download may not succeed
                // First convert the url to a URL type object
                URL imageURL = new URL(imageString);
                // Set up connection to the URL
                HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
                // Used to indicate we are using the URL connection for input
                connection.setDoInput(true);
                // Attempt to initiate the connection
                connection.connect();
                // Get an inputstream that will receive our Image
                InputStream inputStream = connection.getInputStream();
                // Convert the bytes received from the stream into a Bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                // Now that we have our downloading Bitmap, we go back to the thread running the UI
                // so we can finally set the bitmap to the ImageView
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception ex) {
            }
        }).start();
    }
}