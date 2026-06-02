package com.example.firebasetest;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 10;
    private static final int RACE_REMINDER_REQUEST_CODE = 2026;
    private static final String RACE_NOTIFICATION_CHANNEL_ID = RaceReminderReceiver.CHANNEL_ID;

    ImageView imageView;
    private TextView welcomeText;
    private TextView nextRacePlace;
    private TextView countdownText;
    private Button createNewCarBtn, myGarageBtn, popularBtn, remindRaceBtn;
    private ImageButton goToLogin;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private CountDownTimer raceCountdownTimer;
    private RaceData nextRace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        createNewCarBtn = findViewById(R.id.createNewCarBtn);
        myGarageBtn = findViewById(R.id.myGarageBtn);
        popularBtn = findViewById(R.id.popularBtn);
        imageView = findViewById(R.id.imageView);
        welcomeText = findViewById(R.id.text);
        nextRacePlace = findViewById(R.id.nextRacePlace);
        countdownText = findViewById(R.id.countdownText);
        goToLogin = findViewById(R.id.goToLogin);
        remindRaceBtn = findViewById(R.id.remindRaceBtn);
        imageView.setImageResource(R.drawable.redbull_car);
        createRaceNotificationChannel();
        remindRaceBtn.setEnabled(false);
        loadNextRace();


        createNewCarBtn.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(MainActivity.this, "Please log in first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LogInActivity.class));
                return;
            }

            startActivity(new Intent(MainActivity.this, CreateCarActivity.class));

        });
        goToLogin.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
        myGarageBtn.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(MainActivity.this, "Please log in first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LogInActivity.class));
                return;
            }
            Intent intent = new Intent(MainActivity.this, GarageActivity.class);
            startActivity(intent);
        });
        popularBtn.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(MainActivity.this, "Please log in first", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LogInActivity.class));
                return;
            }
            Intent intent = new Intent(MainActivity.this, PopularActivity.class);
            startActivity(intent);
        });
        remindRaceBtn.setOnClickListener(v -> scheduleRaceReminderWithPermissionCheck());

    }

    @Override
    protected void onStart() {
        super.onStart();
        updateWelcomeText();
        updateProfileButton();
    }

    private void updateWelcomeText() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            welcomeText.setText("Welcome guest");
            return;
        }

        firebaseDatabase
                .getReference("users")
                .child(currentUser.getUid())
                .child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String name = snapshot.getValue(String.class);
                    if (name == null || name.trim().isEmpty()) {
                        name = currentUser.getEmail();
                    }
                    welcomeText.setText("Welcome " + name + "!");
                });
    }

    private void updateProfileButton() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin.setImageResource(R.drawable.ic_profile);
            return;
        }

        firebaseDatabase
                .getReference("users")
                .child(currentUser.getUid())
                .child("avatarBase64")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String avatarBase64 = snapshot.getValue(String.class);
                    if (avatarBase64 == null || avatarBase64.isEmpty()) {
                        goToLogin.setImageResource(R.drawable.ic_profile);
                        return;
                    }

                    byte[] imageBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    goToLogin.setPadding(0, 0, 0, 0);
                    goToLogin.setImageBitmap(bitmap);
                })
                .addOnFailureListener(e -> {
                    goToLogin.setImageResource(R.drawable.ic_profile);
                });
    }

    private void startRaceCountdown() {
        if (nextRace == null || nextRace.getTimestamp() <= 0) {
            countdownText.setText("Race unavailable");
            remindRaceBtn.setEnabled(false);
            return;
        }

        long raceStartMillis = nextRace.getTimestamp();
        long millisUntilRace = raceStartMillis - System.currentTimeMillis();

        if (raceCountdownTimer != null) {
            raceCountdownTimer.cancel();
        }

        if (millisUntilRace <= 0) {
            countdownText.setText("Race started");
            remindRaceBtn.setEnabled(false);
            return;
        }

        remindRaceBtn.setEnabled(true);
        raceCountdownTimer = new CountDownTimer(millisUntilRace, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownText.setText(formatCountdown(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                countdownText.setText("Race started");
                remindRaceBtn.setEnabled(false);
            }
        };
        raceCountdownTimer.start();
    }

    private String formatCountdown(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / (24 * 60 * 60);
        long hours = (totalSeconds / (60 * 60)) % 24;
        long minutes = (totalSeconds / 60) % 60;
        long seconds = totalSeconds % 60;

        return String.format(Locale.getDefault(), "%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    private void loadNextRace() {
        nextRacePlace.setText("Loading...");
        countdownText.setText("Loading...");
        remindRaceBtn.setEnabled(false);

        F1ApiClient apiClient = new F1ApiClient(this);
        apiClient.getNextRace(new F1ApiClient.RaceCallback() {
            @Override
            public void onSuccess(RaceData race) {
                nextRace = race;
                nextRacePlace.setText(race.getRaceName() + "\n" + race.getCircuitName());
                startRaceCountdown();
            }

            @Override
            public void onError(String error) {
                nextRace = null;
                nextRacePlace.setText("Race unavailable");
                countdownText.setText("Try again later");
                remindRaceBtn.setEnabled(false);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                Log.d("fail countdown", error);
            }
        });
    }

    private void scheduleRaceReminderWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        scheduleRaceReminder();
    }

    private void scheduleRaceReminder() {
        if (nextRace == null || nextRace.getTimestamp() <= 0) {
            Toast.makeText(this, "Race is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        long reminderMillis = nextRace.getTimestamp() - 60 * 60 * 1000;
        if (reminderMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Reminder time already passed", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, RaceReminderReceiver.class);
        intent.putExtra(RaceReminderReceiver.EXTRA_RACE_NAME, nextRace.getRaceName());
        intent.putExtra(RaceReminderReceiver.EXTRA_RACE_TIME, nextRace.getTimestamp());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                RACE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderMillis, pendingIntent);
            }
            Toast.makeText(this, "Reminder set for 1 hour before the race", Toast.LENGTH_SHORT).show();
        }
    }

    private void createRaceNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                RACE_NOTIFICATION_CHANNEL_ID,
                "Race reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications before upcoming races");
        channel.enableVibration(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleRaceReminder();
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Notifications permission is needed for reminders", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (raceCountdownTimer != null) {
            raceCountdownTimer.cancel();
        }
    }
}
