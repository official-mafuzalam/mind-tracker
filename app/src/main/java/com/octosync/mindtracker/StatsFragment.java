package com.octosync.mindtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private static final String TAG = "StatsFragment";
    private SharedPreferences sharedPreferences;
    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvDebug;

    private final String[] moods = {"Happy", "Neutral", "Sad", "Angry", "Tired"};
    private final String[] moodLabels = {"😊 Happy", "😐 Neutral", "😔 Sad", "😡 Angry", "😴 Tired"};
    private final int[] moodColors = {
            Color.parseColor("#4CAF50"), // Happy - Green
            Color.parseColor("#FFC107"), // Neutral - Yellow
            Color.parseColor("#2196F3"), // Sad - Blue
            Color.parseColor("#F44336"), // Angry - Red
            Color.parseColor("#9C27B0")  // Tired - Purple
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        // Get SharedPreferences - use the same name as MainActivity
        sharedPreferences = requireActivity().getSharedPreferences("MoodPrefs", Context.MODE_PRIVATE);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);

        // Configure charts
        configurePieChart();
        configureBarChart();

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            loadChartData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChartData();
    }

    private void configurePieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setTextColor(Color.GRAY);
        pieLegend.setDrawInside(false);
        pieLegend.setEnabled(true);

        pieChart.setDrawEntryLabels(false);
        pieChart.animateY(1000);
    }

    private void configureBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moodLabels));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend barLegend = barChart.getLegend();
        barLegend.setEnabled(false);

        barChart.animateY(1000);
        barChart.setFitBars(true);
    }

    private void loadChartData() {
        setupWeeklyPieChart();
        setupMonthlyBarChart();
    }

    private void setupWeeklyPieChart() {
        Map<String, Integer> weeklyCounts = calculateMoodStats(7);
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        // Add entries for moods that have data
        for (int i = 0; i < moods.length; i++) {
            String mood = moods[i];
            int count = weeklyCounts.getOrDefault(mood, 0);
            if (count > 0) {
                entries.add(new PieEntry(count, moodLabels[i]));
                colors.add(moodColors[i]);
            }
        }

        // If no data, show placeholder
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No Data"));
            colors.add(Color.LTGRAY);
            pieChart.setCenterText("No Data Yet");
        } else {
            pieChart.setCenterText("Weekly\nMood");
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setCenterTextSize(16f);
        pieChart.invalidate();
    }

    private void setupMonthlyBarChart() {
        Map<String, Integer> monthlyCounts = calculateMoodStats(30);
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        int maxValue = 0;
        for (int i = 0; i < moods.length; i++) {
            int count = monthlyCounts.getOrDefault(moods[i], 0);
            entries.add(new BarEntry(i, count));
            colors.add(moodColors[i]);
            if (count > maxValue) maxValue = count;
        }

        barChart.getAxisLeft().setAxisMaximum(Math.max(5, maxValue + 1));

        BarDataSet dataSet = new BarDataSet(entries, "Last 30 Days");
        dataSet.setColors(colors);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        barChart.setData(data);
        barChart.invalidate();
    }

    private Map<String, Integer> calculateMoodStats(int days) {
        Map<String, Integer> counts = new HashMap<>();
        Calendar calendar = Calendar.getInstance();

        // Initialize counts for all moods to 0
        for (String mood : moods) {
            counts.put(mood, 0);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());

        Log.d(TAG, "Calculating stats for last " + days + " days");

        for (int i = 0; i < days; i++) {
            String dateKey = dateFormat.format(calendar.getTime());
            String mood = sharedPreferences.getString(dateKey, "");

            if (!mood.isEmpty()) {
                Log.d(TAG, "Found entry for " + dateKey + ": " + mood);
                counts.put(mood, counts.getOrDefault(mood, 0) + 1);
            } else {
                Log.d(TAG, "No entry for " + dateKey);
            }

            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return counts;
    }
}