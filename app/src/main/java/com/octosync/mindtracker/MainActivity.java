package com.octosync.mindtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private Button btnHappy, btnNeutral, btnSad, btnAngry, btnTired;
    private Button btnStats, btnUpdateMood;

    private TextView tvTodayMood, tvDate;

    private String todayDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MoodPrefs", MODE_PRIVATE);
        todayDate = getTodayDate();

        initViews();
        displayDate();
        checkMoodStatus();
        scheduleDailyNotification();
    }

    private void initViews() {

        tvTodayMood = findViewById(R.id.tvTodayMood);
        tvDate = findViewById(R.id.tvDate);

        btnHappy = findViewById(R.id.btnHappy);
        btnNeutral = findViewById(R.id.btnNeutral);
        btnSad = findViewById(R.id.btnSad);
        btnAngry = findViewById(R.id.btnAngry);
        btnTired = findViewById(R.id.btnTired);

        btnUpdateMood = findViewById(R.id.btnUpdateMood);
        btnStats = findViewById(R.id.btnStats);

        btnHappy.setOnClickListener(v -> saveMood("Happy"));
        btnNeutral.setOnClickListener(v -> saveMood("Neutral"));
        btnSad.setOnClickListener(v -> saveMood("Sad"));
        btnAngry.setOnClickListener(v -> saveMood("Angry"));
        btnTired.setOnClickListener(v -> saveMood("Tired"));

        btnUpdateMood.setOnClickListener(v -> enableUpdateMode());

        btnStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
    }

    private void displayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    private void checkMoodStatus() {
        if (sharedPreferences.contains(todayDate)) {
            String mood = sharedPreferences.getString(todayDate, "");
            showSavedMood(mood);
        } else {
            enableMoodSelection();
            btnUpdateMood.setVisibility(View.GONE);
            tvTodayMood.setVisibility(View.GONE);
        }
    }

    private void saveMood(String mood) {

        sharedPreferences.edit().putString(todayDate, mood).apply();

        // Show motivational message
        String message;
        switch (mood) {
            case "Happy":
                message = "Keep smiling! 😊";
                break;
            case "Neutral":
                message = "Stay balanced today! ⚖️";
                break;
            case "Sad":
                message = "It's okay to feel sad. Take a deep breath. 🌧️";
                break;
            case "Angry":
                message = "Take a moment to relax. 😤";
                break;
            case "Tired":
                message = "Rest well and recharge! 😴";
                break;
            default:
                message = "";
        }

        Toast.makeText(this, "Mood saved! " + message, Toast.LENGTH_LONG).show();

        showSavedMood(mood);
    }

    private void showSavedMood(String mood) {

        tvTodayMood.setText("Today's Mood: " + mood);
        tvTodayMood.setVisibility(View.VISIBLE);

        disableMoodButtons();
        btnUpdateMood.setVisibility(View.VISIBLE);
    }

    private void enableUpdateMode() {

        tvTodayMood.setVisibility(View.GONE);
        enableMoodSelection();
        btnUpdateMood.setVisibility(View.GONE);

        Toast.makeText(this, "Select new mood", Toast.LENGTH_SHORT).show();
    }

    private void disableMoodButtons() {
        btnHappy.setEnabled(false);
        btnNeutral.setEnabled(false);
        btnSad.setEnabled(false);
        btnAngry.setEnabled(false);
        btnTired.setEnabled(false);
    }

    private void enableMoodSelection() {
        btnHappy.setEnabled(true);
        btnNeutral.setEnabled(true);
        btnSad.setEnabled(true);
        btnAngry.setEnabled(true);
        btnTired.setEnabled(true);
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ================= DAILY 10 AM NOTIFICATION =================
    // ================= DAILY 5:30 PM NOTIFICATION =================
    private void scheduleDailyNotification() {

        // Calendar instance for 5:30 PM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17); // 5 PM
        calendar.set(Calendar.MINUTE, 30);      // 30 minutes
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long currentTime = System.currentTimeMillis();
        long scheduledTime = calendar.getTimeInMillis();
        long initialDelay = scheduledTime - currentTime;

        // If 5:30 PM already passed today, schedule for tomorrow
        if (initialDelay < 0) {
            initialDelay += TimeUnit.DAYS.toMillis(1);
        }

        // Create a periodic work request repeating every 24 hours
        PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                MoodReminderWorker.class,
                24,
                TimeUnit.HOURS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        // Enqueue unique work to prevent duplicates
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_mood_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
        );
    }
}