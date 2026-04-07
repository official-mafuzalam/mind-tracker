package com.octosync.mindtracker;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoodFragment extends Fragment {

    private TextView tvDate, tvTodayMood, tvQuote;
    private MoodButton btnHappy, btnNeutral, btnSad, btnAngry, btnTired;
    private MaterialButton btnUpdateMood, btnStats;
    private MoodButton selectedMoodButton;
    private SharedPreferences sharedPreferences;
    private String todayDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood, container, false);

        // Get SharedPreferences from MainActivity
        if (getActivity() != null) {
            sharedPreferences = ((MainActivity) getActivity()).getSharedPrefs();
        }

        todayDate = getTodayDate();

        initViews(view);
        displayDate();
        checkTodayMood();
        tvQuote.setText(randomQuote());


        return view;
    }

    private void initViews(View view) {
        tvDate = view.findViewById(R.id.tvDate);
        tvTodayMood = view.findViewById(R.id.tvTodayMood);
        tvQuote = view.findViewById(R.id.tvQuote);

        // Initialize custom mood buttons
        btnHappy = view.findViewById(R.id.btnHappy);
        btnNeutral = view.findViewById(R.id.btnNeutral);
        btnSad = view.findViewById(R.id.btnSad);
        btnAngry = view.findViewById(R.id.btnAngry);
        btnTired = view.findViewById(R.id.btnTired);

        btnUpdateMood = view.findViewById(R.id.btnUpdateMood);
        btnStats = view.findViewById(R.id.btnStats);

        // Set click listeners
        btnHappy.setOnClickListener(v -> selectMood(btnHappy));
        btnNeutral.setOnClickListener(v -> selectMood(btnNeutral));
        btnSad.setOnClickListener(v -> selectMood(btnSad));
        btnAngry.setOnClickListener(v -> selectMood(btnAngry));
        btnTired.setOnClickListener(v -> selectMood(btnTired));

        btnUpdateMood.setOnClickListener(v -> enableMoodSelection());

        btnStats.setOnClickListener(v -> {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                // Switch to stats tab (position 1)
                ((MainActivity) getActivity()).viewPager.setCurrentItem(1);
            }
        });
    }

    private void selectMood(MoodButton button) {
        if (selectedMoodButton != null) {
            selectedMoodButton.setSelected(false);
        }
        button.setSelected(true);
        selectedMoodButton = button;

        // Save mood
        saveMood(button.getMoodType());
    }

    private void displayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));
    }

    private void checkTodayMood() {
        if (sharedPreferences != null && sharedPreferences.contains(todayDate)) {
            String mood = sharedPreferences.getString(todayDate, "");
            showSavedMood(mood);
        } else {
            enableMoodSelection();
            btnUpdateMood.setVisibility(View.GONE);
            tvTodayMood.setVisibility(View.GONE);
        }
    }

    private void saveMood(String mood) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(todayDate, mood).apply();

            // Show message
            String message = getMoodMessage(mood);
            Toast.makeText(getContext(), "Mood saved! " + message, Toast.LENGTH_SHORT).show();

            updateWidget();
            showSavedMood(mood);
        }
    }

    private void updateWidget() {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), MoodWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(getContext())
                    .getAppWidgetIds(new ComponentName(getContext(), MoodWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            getContext().sendBroadcast(intent);
        }
    }

    private String getMoodMessage(String mood) {
        switch (mood) {
            case "Happy":
                return "Keep smiling! 😊";
            case "Neutral":
                return "Stay balanced! ⚖️";
            case "Sad":
                return "Tomorrow will be better 🦾";
            case "Angry":
                return "Take a deep breath 🧘";
            case "Tired":
                return "Rest well! 😴";
            default:
                return "";
        }
    }

    private String randomQuote() {
        String[] quotes = {
                "“Every day is a fresh start.”",
                "“Happiness is good for health.”",
                "“Small steps still move you forward.”",
                "“Your mood does not define your worth.”",
                "“Progress, not perfection.”"
        };

        int randomIndex = new java.util.Random().nextInt(quotes.length);
        return quotes[randomIndex];
    }

    private void showSavedMood(String mood) {
        tvTodayMood.setText("Today's Mood: " + mood);
        tvTodayMood.setVisibility(View.VISIBLE);

        // Disable mood buttons
        btnHappy.setEnabled(false);
        btnNeutral.setEnabled(false);
        btnSad.setEnabled(false);
        btnAngry.setEnabled(false);
        btnTired.setEnabled(false);

        btnUpdateMood.setVisibility(View.VISIBLE);
    }

    private void enableMoodSelection() {
        tvTodayMood.setVisibility(View.GONE);

        btnHappy.setEnabled(true);
        btnNeutral.setEnabled(true);
        btnSad.setEnabled(true);
        btnAngry.setEnabled(true);
        btnTired.setEnabled(true);

        if (selectedMoodButton != null) {
            selectedMoodButton.setSelected(false);
            selectedMoodButton = null;
        }

        btnUpdateMood.setVisibility(View.GONE);
        Toast.makeText(getContext(), "Select your new mood", Toast.LENGTH_SHORT).show();
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}