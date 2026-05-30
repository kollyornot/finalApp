package com.example.firebasetest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GarageActivity extends AppCompatActivity {
    private TextView tvStatus;
    private ListView tvUsersList;
    private ArrayList<Car> cars;
    private CarAdapter adapter;
    private Button goToMain;
    private DatabaseReference carsRef;
    private DatabaseReference publicCarsRef;
    private ValueEventListener carsListener;
    private ValueEventListener publicCarsListener;
    private DataSnapshot latestGarageSnapshot;
    private DataSnapshot latestPublicCarsSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage);

        tvStatus = findViewById(R.id.tvStatus);
        tvUsersList = findViewById(R.id.tvUsersList);
        goToMain = findViewById(R.id.goToMain);

        cars = new ArrayList<>();
        adapter = new CarAdapter(this, cars);
        adapter.setShowPublishButton(true);
        adapter.setShowDeleteButton(true);
        adapter.setPublishListener(this::publishCar);
        adapter.setDeleteListener(this::confirmDeleteCar);
        tvUsersList.setAdapter(adapter);

        goToMain.setOnClickListener(v -> {
            Intent intent = new Intent(GarageActivity.this, MainActivity.class);
            startActivity(intent);
        });


        loadGarage();
    }

    private void publishCar(Car car) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (car.carid == null || car.carid.isEmpty()) {
            Toast.makeText(this, "Cannot publish this car", Toast.LENGTH_SHORT).show();
            return;
        }

        car.ownerId = currentUser.getUid();
        car.published = true;

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/cars/" + currentUser.getUid() + "/" + car.carid + "/published", true);
        updates.put("/cars/" + currentUser.getUid() + "/" + car.carid + "/ownerId", currentUser.getUid());
        updates.put("/publicCars/" + car.carid, car);

        rootRef.updateChildren(updates)
                .addOnFailureListener(e -> Toast.makeText(GarageActivity.this, "Failed to publish: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteCar(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Delete car")
                .setMessage("Delete this car from your garage and popular list?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCar(car))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCar(Car car) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (car.carid == null || car.carid.isEmpty()) {
            Toast.makeText(this, "Cannot delete this car", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/cars/" + currentUser.getUid() + "/" + car.carid, null);
        updates.put("/publicCars/" + car.carid, null);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(GarageActivity.this, "Car deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(GarageActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadGarage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            setStatus("Please log in to view your garage");
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        setStatus("Loading");
        carsRef = FirebaseDatabase.getInstance()
                .getReference("cars")
                .child(currentUser.getUid());

        carsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                latestGarageSnapshot = snapshot;
                rebuildGarageCars();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setStatus("Error loading garage");
                Toast.makeText(GarageActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        publicCarsRef = FirebaseDatabase.getInstance().getReference("publicCars");
        publicCarsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                latestPublicCarsSnapshot = snapshot;
                rebuildGarageCars();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GarageActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        carsRef.addValueEventListener(carsListener);
        publicCarsRef.addValueEventListener(publicCarsListener);
    }

    private void rebuildGarageCars() {
        if (latestGarageSnapshot == null) {
            return;
        }

        cars.clear();
        for (DataSnapshot carSnapshot : latestGarageSnapshot.getChildren()) {
            Car car = carSnapshot.getValue(Car.class);
            if (car != null) {
                String carId = car.carid == null || car.carid.isEmpty() ? carSnapshot.getKey() : car.carid;
                car.carid = carId;

                if (latestPublicCarsSnapshot != null && latestPublicCarsSnapshot.hasChild(carId)) {
                    Long publicLikes = latestPublicCarsSnapshot.child(carId).child("likesCount").getValue(Long.class);
                    if (publicLikes != null) {
                        car.likesCount = publicLikes.intValue();
                    }
                }

                cars.add(car);
            }
        }

        if (cars.isEmpty()) {
            setStatus("No cars in your garage yet");
        } else {
            setStatus(null);
        }
        adapter.notifyDataSetChanged();
    }

    private void setStatus(String status) {
        if (status == null) {
            tvStatus.setVisibility(View.GONE);
            tvUsersList.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(status);
            tvUsersList.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (carsRef != null && carsListener != null) {
            carsRef.removeEventListener(carsListener);
        }
        if (publicCarsRef != null && publicCarsListener != null) {
            publicCarsRef.removeEventListener(publicCarsListener);
        }
    }
}
