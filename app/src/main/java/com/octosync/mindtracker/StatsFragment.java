package com.octosync.mindtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private PieChart pieChart;
    private BarChart barChart;
    private final String[] moods = {"Happy", "Neutral", "Sad", "Angry", "Tired"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("MoodPrefs", Context.MODE_PRIVATE);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);

        setupWeeklyPieChart();
        setupMonthlyBarChart();

        return view;
    }

    private void setupWeeklyPieChart() {
        Map<String, Integer> weeklyCounts = calculateMoodStats(7);
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (String mood : moods) {
            int count = weeklyCounts.getOrDefault(mood, 0);
            if (count > 0) {
                entries.add(new PieEntry(count, mood));
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Last 7 Days");
        dataSet.setColors(getMoodColors());
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupMonthlyBarChart() {
        Map<String, Integer> monthlyCounts = calculateMoodStats(30);
        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < moods.length; i++) {
            entries.add(new BarEntry(i, monthlyCounts.getOrDefault(moods[i], 0)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Last 30 Days");
        dataSet.setColors(getMoodColors());

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moods));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        barChart.invalidate();
    }

    private Map<String, Integer> calculateMoodStats(int days) {
        Map<String, Integer> counts = new HashMap<>();
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < days; i++) {
            String dateKey = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault())
                    .format(calendar.getTime());
            String mood = sharedPreferences.getString(dateKey, "");

            if (!mood.isEmpty()) {
                counts.put(mood, counts.getOrDefault(mood, 0) + 1);
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return counts;
    }

    private ArrayList<Integer> getMoodColors() {
        return new ArrayList<>(Arrays.asList(
                Color.parseColor("#4CAF50"), // Happy - Green
                Color.parseColor("#FFC107"), // Neutral - Yellow
                Color.parseColor("#2196F3"), // Sad - Blue
                Color.parseColor("#F44336"), // Angry - Red
                Color.parseColor("#9C27B0")  // Tired - Purple
        ));
    }
}