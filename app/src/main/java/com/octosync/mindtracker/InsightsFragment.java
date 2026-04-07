package com.octosync.mindtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InsightsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private TextView tvStreak, tvTotalEntries, tvMostCommon;
    private RecyclerView recyclerView;
    private MoodHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("MoodPrefs", Context.MODE_PRIVATE);

        tvStreak = view.findViewById(R.id.tvStreak);
        tvTotalEntries = view.findViewById(R.id.tvTotalEntries);
        tvMostCommon = view.findViewById(R.id.tvMostCommon);
        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MoodHistoryAdapter();
        recyclerView.setAdapter(adapter);

        loadInsights();
        loadHistory();

        return view;
    }

    private void loadInsights() {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        int moodEntryCount = 0;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof String && entry.getKey().matches("\\d{4}_\\d{2}_\\d{2}")) {
                moodEntryCount++;
            }
        }

        // Calculate streak
        int streak = calculateStreak();

        // Find most common mood
        String mostCommon = findMostCommonMood();

        tvTotalEntries.setText(" " + moodEntryCount);
        tvStreak.setText(" " + streak + " days");
        tvMostCommon.setText(mostCommon);
    }

    private int calculateStreak() {
        int streak = 0;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());

        while (true) {
            String dateKey = dateFormat.format(calendar.getTime());

            if (sharedPreferences.contains(dateKey) && sharedPreferences.getAll().get(dateKey) instanceof String) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String findMostCommonMood() {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        Map<String, Integer> moodCounts = new java.util.HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof String && entry.getKey().matches("\\d{4}_\\d{2}_\\d{2}")) {
                String moodStr = (String) entry.getValue();
                moodCounts.put(moodStr, moodCounts.getOrDefault(moodStr, 0) + 1);
            }
        }

        String mostCommon = "No data";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }

        return mostCommon;
    }

    private void loadHistory() {
        List<MoodEntry> entries = new ArrayList<>();
        Map<String, ?> allEntries = sharedPreferences.getAll();

        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (!(entry.getValue() instanceof String) || !entry.getKey().matches("\\d{4}_\\d{2}_\\d{2}")) {
                continue;
            }
            try {
                String dateStr = entry.getKey();
                String mood = (String) entry.getValue();

                // Convert storage date to display date
                java.util.Date date = storageFormat.parse(dateStr);
                String displayDate = displayFormat.format(date);

                entries.add(new MoodEntry(displayDate, mood));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Sort by date (newest first)
        Collections.sort(entries, (e1, e2) -> e2.date.compareTo(e1.date));

        adapter.setEntries(entries);
    }

    // Inner class for mood history entries
    private static class MoodEntry {
        String date;
        String mood;

        MoodEntry(String date, String mood) {
            this.date = date;
            this.mood = mood;
        }
    }

    // RecyclerView Adapter
    private class MoodHistoryAdapter extends RecyclerView.Adapter<MoodHistoryAdapter.ViewHolder> {

        private List<MoodEntry> entries = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mood_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MoodEntry entry = entries.get(position);
            holder.tvDate.setText(entry.date);
            holder.tvMood.setText(entry.mood);

            // Set mood icon/color based on mood type
            int color = getMoodColor(entry.mood);
            holder.viewColor.setBackgroundColor(color);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        void setEntries(List<MoodEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvMood;
            View viewColor;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvMood = itemView.findViewById(R.id.tvMood);
                viewColor = itemView.findViewById(R.id.viewColor);
            }
        }

        private int getMoodColor(String mood) {
            switch (mood) {
                case "Happy":
                    return Color.parseColor("#4CAF50");
                case "Neutral":
                    return Color.parseColor("#FFC107");
                case "Sad":
                    return Color.parseColor("#2196F3");
                case "Angry":
                    return Color.parseColor("#F44336");
                case "Tired":
                    return Color.parseColor("#9C27B0");
                default:
                    return Color.GRAY;
            }
        }
    }
}