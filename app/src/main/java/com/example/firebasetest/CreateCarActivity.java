package com.example.firebasetest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

public class CreateCarActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Button goToMain, createBtn;
    private Spinner teamSpinner, engineSpinner;
    private ImageView imageView;
    private EditText nameEdtTxt;
    private DatabaseReference databaseCars;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private boolean canCreateCars = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_car);

        goToMain = findViewById(R.id.goToMain);
        nameEdtTxt = findViewById(R.id.nameEdtTxt);
        engineSpinner = findViewById(R.id.engineSpinner);
        teamSpinner = findViewById(R.id.teamSpinner);
        createBtn = findViewById(R.id.createBtn);
        imageView = findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.redbull_car);
        mAuth = FirebaseAuth.getInstance();
        databaseCars = FirebaseDatabase.getInstance().getReference("cars");
        usersRef = FirebaseDatabase.getInstance().getReference("users");


        ArrayAdapter<CharSequence> engAdapter = ArrayAdapter.createFromResource(this, R.array.engines_array, android.R.layout.simple_spinner_dropdown_item);
        engAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> teamAdapter = ArrayAdapter.createFromResource(this, R.array.teams_array, android.R.layout.simple_spinner_dropdown_item);
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        engineSpinner.setAdapter(engAdapter);
        teamSpinner.setAdapter(teamAdapter);
        engineSpinner.setOnItemSelectedListener(this);
        teamSpinner.setOnItemSelectedListener(this);

        goToMain.setOnClickListener(v -> {
            Intent intent = new Intent(CreateCarActivity.this, MainActivity.class);
            startActivity(intent);
        });

        createBtn.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(CreateCarActivity.this, "Please log in first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(CreateCarActivity.this, LogInActivity.class));
                finish();
                return;
            }

            if (!canCreateCars) {
                Toast.makeText(CreateCarActivity.this, "You are not authorized to create cars", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = nameEdtTxt.getText().toString().trim();
            String team = teamSpinner.getSelectedItem().toString();
            String engine = engineSpinner.getSelectedItem().toString();
            int imageResId = CarImageHelper.getImageResIdForTeam(team);

            if (name.isEmpty()) {
                Toast.makeText(CreateCarActivity.this, "Please enter a car name", Toast.LENGTH_SHORT).show();
                return;
            }

            String carId = databaseCars.push().getKey();
            String username = currentUser.getUid();

            Car newCar = new Car(carId, username, team, name, engine, imageResId, false, 0);

            databaseCars.child(username).child(carId)
                    .setValue(newCar)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateCarActivity.this, "Car saved successfully!", Toast.LENGTH_SHORT).show();
                        nameEdtTxt.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CreateCarActivity.this, "Failed to save car: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        loadAuthorizationState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAuthorizationState();
    }

    private void loadAuthorizationState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            canCreateCars = false;
            createBtn.setEnabled(false);
            Toast.makeText(this, "Please log in to create cars", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CreateCarActivity.this, LogInActivity.class));
            finish();
            return;
        }
        canCreateCars = true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (parent.getId() == R.id.teamSpinner) {

            String selectedTeam = parent.getItemAtPosition(position).toString();
            imageView.setImageResource(CarImageHelper.getImageResIdForTeam(selectedTeam));
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
