package com.example.firebasetest;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class LogInActivity extends AppCompatActivity {

    private TextView txtStatus;
    private EditText edtpassword, edtEmail;
    private LinearLayout layoutLogin, layoutLogout, layoutSignup;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private Button loginBtn, logoutBtn, signupBtn, goToMain, continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        txtStatus = findViewById(R.id.txtStatus);
        edtpassword = findViewById(R.id.edtpassword);
        edtEmail = findViewById(R.id.edtEmail);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutLogout = findViewById(R.id.layoutLogout);
        layoutSignup = findViewById(R.id.layoutSignup);
        loginBtn = findViewById(R.id.loginBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        signupBtn = findViewById(R.id.signupBtn);
        goToMain = findViewById(R.id.goToMain);
        continueBtn = findViewById(R.id.continueBtn);

        goToMain.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, MainActivity.class);
            startActivity(intent);
        });
        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        signupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(LogInActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        loginBtn.setOnClickListener(view -> {
            String email = String.valueOf(edtEmail.getText());
            String password = String.valueOf(edtpassword.getText());
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LogInActivity.this, "Email or password is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("FireBaseConnection", "signInWithEmail:success");
                                Toast.makeText(LogInActivity.this, "Log in success", Toast.LENGTH_SHORT).show();
                                updateUI();
                                Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.w("FireBaseConnection", "signInWithEmail:failure", task.getException());
                                Toast.makeText(LogInActivity.this, "Log in fail", Toast.LENGTH_SHORT).show();
                                updateUI();
                            }
                        }
                    });
        });

        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI();
    }

    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {

            layoutLogin.setVisibility(View.VISIBLE);
            layoutLogout.setVisibility(View.GONE);
            layoutSignup.setVisibility(View.VISIBLE);
            txtStatus.setText("Hello Guest!");
            edtpassword.setText("");
            return;
        }
        Log.d("CheckOnComplete", "user was found: "+user.getUid());
        String uid = user.getUid();
        firebaseDatabase.getReference("users").child(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        Log.d("CheckOnComplete", "Entered the function");
                        String name;
                        if (!task.isSuccessful()){
                            name = user.getEmail();
                        } else {
                            User u = task.getResult().getValue(User.class);
                            name = (u != null && u.name != null && !u.name.isEmpty()) ? u.name : user.getEmail();
                        }
                        layoutLogin.setVisibility(View.GONE);
                        layoutLogout.setVisibility(View.VISIBLE);
                        layoutSignup.setVisibility(View.GONE);
                        txtStatus.setText("Hello, " + name);
                        txtStatus.setTextColor(Color.GREEN);

                    }
                });

    }
}
