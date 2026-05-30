package com.example.firebasetest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private TextView txtGreeting;
    private EditText edtPassword, edtEmail, edtName;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private Button createAccBtn, goToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        txtGreeting = findViewById(R.id.txtGreeting);
        edtPassword = findViewById(R.id.edtpassword);
        edtEmail = findViewById(R.id.edtEmail);
        createAccBtn = findViewById(R.id.createAccBtn);
        goToMain = findViewById(R.id.goToMain);
        edtName = findViewById(R.id.edtName);

        goToMain.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        createAccBtn.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String name = edtName.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "email or password or name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d("FireBaseRegistration", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI();
                        } else {
                            Log.w("FireBaseRegistration", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "registration failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });


        });
    }

    private void updateUI() {
            addUserDetails();
            Intent intent = new Intent(SignUpActivity.this, LogInActivity.class);
            startActivity(intent);
            finish();

    }

    private void addUserDetails(){
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String uid = mAuth.getCurrentUser().getUid().toString();

        User u = new User(uid, email, name);
        Log.d("CheckOnComplete", u.uid);
        DatabaseReference userRef = firebaseDatabase.getReference("users").child(uid);
        u.key = userRef.getKey();
        Log.d("CheckOnComplete", u.key);
        userRef.setValue(u);
    }
}
