package com.octosync.mindtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    // ViewPager related
    public ViewPager viewPager;
    private TabLayout tabLayout;
    private MainPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("MoodPrefs", MODE_PRIVATE);

        initViews();
        setupViewPager();
        scheduleDailyNotification();
    }

    private void initViews() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("MindTracker");
        }
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        pagerAdapter = new MainPagerAdapter(getSupportFragmentManager(),
                MainPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        tabLayout.setupWithViewPager(viewPager);

        // Setup tab icons
        setupTabIcons();
    }

    private void setupTabIcons() {
        int[] tabIcons = {
                R.drawable.ic_today,
                R.drawable.ic_stats,
                R.drawable.ic_history
        };

        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setIcon(tabIcons[i]);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_export) {
            Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            Toast.makeText(this, "MindTracker v1.0", Toast.LENGTH_SHORT).show();
            openWebsite("https://octosyncsoftware.com/");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a website in the browser
     * @param url The URL to open
     */
    private void openWebsite(String url) {
        try {
            // Create a Custom Tabs intent
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            // Customize the tab (optional)
            builder.setToolbarColor(ContextCompat.getColor(this, R.color.purple_500));
            builder.setShowTitle(true);

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(this, Uri.parse(url));

        } catch (Exception e) {
            // Fallback to regular browser if Custom Tabs fails
            try {
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(websiteIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open website", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scheduleDailyNotification() {
        // Calendar instance for 5:30 PM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 30);
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

    // Helper method to get SharedPreferences from fragments
    public SharedPreferences getSharedPrefs() {
        return sharedPreferences;
    }
}