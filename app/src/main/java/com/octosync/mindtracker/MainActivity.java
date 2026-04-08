package com.octosync.mindtracker;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.github.mikephil.charting.BuildConfig;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.Locale;
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

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Apply system bar insets to root layout
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

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
            showTimePickerDialog();
            return true;
//        } else if (id == R.id.action_export) {
//            // Test notification
//            OneTimeWorkRequest testRequest = new OneTimeWorkRequest.Builder(MoodReminderWorker.class).build();
//            WorkManager.getInstance(this).enqueue(testRequest);
//            Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
//            return true;
        } else if (id == R.id.action_about) {
            String appName = getString(R.string.app_name);
            String version = BuildConfig.VERSION_NAME;
            Toast.makeText(this, appName + " v" + version, Toast.LENGTH_SHORT).show();
            openWebsite("https://octosyncsoftware.com/");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Open a website in the browser
     *
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

    private void showTimePickerDialog() {
        int hour = sharedPreferences.getInt("notification_hour", 20);
        int minute = sharedPreferences.getInt("notification_minute", 0);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    sharedPreferences.edit()
                            .putInt("notification_hour", hourOfDay)
                            .putInt("notification_minute", minuteOfHour)
                            .apply();
                    
                    scheduleDailyNotification();
                    Toast.makeText(this, "Reminder set for " + 
                            String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour),
                            Toast.LENGTH_SHORT).show();
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void scheduleDailyNotification() {
        Calendar calendar = Calendar.getInstance();
        long nowMillis = calendar.getTimeInMillis();

        // Get saved time or default to 8:00 PM
        int hour = sharedPreferences.getInt("notification_hour", 20);
        int minute = sharedPreferences.getInt("notification_minute", 0);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If it's already past the target time today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= nowMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = calendar.getTimeInMillis() - nowMillis;

        // Create a periodic work request for every 24 hours
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                MoodReminderWorker.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("daily_mood_tag")
                .build();

        // Use REPLACE to ensure that if the user changes the time, the schedule is updated
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyMoodReminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
        );
    }

    // Helper method to get SharedPreferences from fragments
    public SharedPreferences getSharedPrefs() {
        return sharedPreferences;
    }
}