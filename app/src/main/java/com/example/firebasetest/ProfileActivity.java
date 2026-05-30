package com.example.firebasetest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {
    private ImageView avatarImage;
    private TextView profileNameText, profileEmailText;
    private Button changeNameBtn, galleryAvatarBtn, cameraAvatarBtn, logoutBtn, backBtn;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LogInActivity.class));
            finish();
            return;
        }

        avatarImage = findViewById(R.id.avatarImage);
        profileNameText = findViewById(R.id.profileNameText);
        profileEmailText = findViewById(R.id.profileEmailText);
        changeNameBtn = findViewById(R.id.changeNameBtn);
        galleryAvatarBtn = findViewById(R.id.galleryAvatarBtn);
        cameraAvatarBtn = findViewById(R.id.cameraAvatarBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        backBtn = findViewById(R.id.backBtn);

        registerImageLaunchers();
        loadProfile();

        changeNameBtn.setOnClickListener(v -> showChangeNameDialog());
        galleryAvatarBtn.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        cameraAvatarBtn.setOnClickListener(v -> openCameraWithPermissionCheck());
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            finish();
        });
        backBtn.setOnClickListener(v -> finish());
    }

    private void registerImageLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                saveAvatarFromUri(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                saveAvatarBitmap(bitmap);
            }
        });

        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                cameraLauncher.launch(null);
            } else {
                Toast.makeText(this, "Camera permission is needed to take a photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
        profileEmailText.setText(currentUser.getEmail());

        firebaseDatabase
                .getReference("users")
                .child(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.name != null && !user.name.trim().isEmpty()) {
                        profileNameText.setText(user.name);
                    } else {
                        profileNameText.setText("Profile");
                    }

                    if (user != null && user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                        showAvatar(user.avatarBase64);
                    } else {
                        avatarImage.setImageResource(R.drawable.ic_profile);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChangeNameDialog() {
        EditText nameInput = new EditText(this);
        nameInput.setHint("New name");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(this)
                .setTitle("Change name")
                .setView(nameInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(ProfileActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateUserName(newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUserName(String newName) {
        firebaseDatabase
                .getReference("users")
                .child(currentUser.getUid())
                .child("name")
                .setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    profileNameText.setText(newName);
                    Toast.makeText(ProfileActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openCameraWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void saveAvatarFromUri(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            saveAvatarBitmap(bitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAvatarBitmap(Bitmap bitmap) {
        String avatarBase64 = bitmapToBase64(bitmap);
        DatabaseReference avatarRef = firebaseDatabase
                .getReference("users")
                .child(currentUser.getUid())
                .child("avatarBase64");

        avatarRef.setValue(avatarBase64)
                .addOnSuccessListener(aVoid -> {
                    showAvatar(avatarBase64);
                    Toast.makeText(ProfileActivity.this, "Avatar updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to update avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String bitmapToBase64(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    private void showAvatar(String avatarBase64) {
        byte[] imageBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        avatarImage.setPadding(0, 0, 0, 0);
        avatarImage.setImageBitmap(bitmap);
    }
}
