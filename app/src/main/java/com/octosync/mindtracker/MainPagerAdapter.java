package com.octosync.mindtracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private final Fragment[] fragments;
    private final String[] titles;

    public MainPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);

        // Initialize fragments
        fragments = new Fragment[]{
                new MoodFragment(),      // Today's mood
                new StatsFragment(),      // Statistics
                new InsightsFragment()    // Insights/History
        };

        // Initialize titles
        titles = new String[]{
                "Today",
                "Stats",
                "History"
        };
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }
}