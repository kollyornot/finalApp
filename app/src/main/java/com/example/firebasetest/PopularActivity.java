package com.example.firebasetest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class PopularActivity extends AppCompatActivity {

    private Button goToMain;
    private TextView tvStatus;
    private ListView tvUsersList;
    private ArrayList<Car> cars;
    private CarAdapter adapter;
    private DatabaseReference publicCarsRef;
    private ValueEventListener publicCarsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular);

        goToMain = findViewById(R.id.goToMain);
        tvStatus = findViewById(R.id.tvStatus);
        tvUsersList = findViewById(R.id.tvUsersList);

        cars = new ArrayList<>();
        adapter = new CarAdapter(this, cars);
        adapter.setShowLikeButton(true);
        adapter.setLikeListener(this::toggleLike);
        tvUsersList.setAdapter(adapter);

        goToMain.setOnClickListener(v -> {
            Intent intent = new Intent(PopularActivity.this, MainActivity.class);
            startActivity(intent);
        });
        loadPopularCars();
    }

    private void loadPopularCars() {
        setStatus("Loading popular cars");
        publicCarsRef = FirebaseDatabase.getInstance().getReference("publicCars");
        publicCarsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cars.clear();
                for (DataSnapshot carSnapshot : snapshot.getChildren()) {
                    Car car = carSnapshot.getValue(Car.class);
                    if (car != null && car.published) {
                        if (car.carid == null || car.carid.isEmpty()) {
                            car.carid = carSnapshot.getKey();
                        }
                        cars.add(car);
                    }
                }

                Collections.sort(cars, (first, second) -> second.likesCount - first.likesCount);

                if (cars.isEmpty()) {
                    setStatus("No published cars yet");
                } else {
                    setStatus(null);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setStatus("Error loading popular cars");
                Toast.makeText(PopularActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        publicCarsRef.addValueEventListener(publicCarsListener);
    }

    private void toggleLike(Car car) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PopularActivity.this, LogInActivity.class));
            return;
        }

        if (car.carid == null || car.carid.isEmpty()) {
            Toast.makeText(this, "Cannot like this car", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference carRef = FirebaseDatabase.getInstance()
                .getReference("publicCars")
                .child(car.carid);

        carRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    return Transaction.success(currentData);
                }

                String uid = currentUser.getUid();
                Long likes = currentData.child("likesCount").getValue(Long.class);
                int likesCount = likes == null ? 0 : likes.intValue();
                Boolean alreadyLiked = currentData.child("likedBy").child(uid).getValue(Boolean.class);

                if (Boolean.TRUE.equals(alreadyLiked)) {
                    currentData.child("likedBy").child(uid).setValue(null);
                    currentData.child("likesCount").setValue(Math.max(0, likesCount - 1));
                } else {
                    currentData.child("likedBy").child(uid).setValue(true);
                    currentData.child("likesCount").setValue(likesCount + 1);
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(PopularActivity.this, "Like failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!committed) {
                    return;
                }

                Car updatedCar = currentData.getValue(Car.class);
                if (updatedCar == null || updatedCar.ownerId == null || updatedCar.carid == null) {
                    return;
                }

                FirebaseDatabase.getInstance()
                        .getReference("cars")
                        .child(updatedCar.ownerId)
                        .child(updatedCar.carid)
                        .child("likesCount")
                        .setValue(updatedCar.likesCount);
            }
        });
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
        if (publicCarsRef != null && publicCarsListener != null) {
            publicCarsRef.removeEventListener(publicCarsListener);
        }
    }
}
