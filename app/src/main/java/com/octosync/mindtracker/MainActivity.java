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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
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

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> exportDocumentLauncher;

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

        exportDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            writeCsvToUri(uri);
                        }
                    }
                }
        );

        initViews();
        setupViewPager();
        scheduleDailyNotification();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndPromptBiometric();
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
        } else if (id == R.id.action_app_lock) {
            toggleAppLockSetting();
            return true;
        } else if (id == R.id.action_export) {
            showExportOptions();
            return true;
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

    private void toggleAppLockSetting() {
        boolean isCurrentlyEnabled = sharedPreferences.getBoolean("app_lock_enabled", false);
        new AlertDialog.Builder(this)
                .setTitle("App Lock Privacy")
                .setMessage(isCurrentlyEnabled ? "App Lock is currently ENABLED. Do you want to disable it?" : "Do you want to enable Biometric / Device Passcode Lock for MindTracker?")
                .setPositiveButton(isCurrentlyEnabled ? "Disable" : "Enable", (dialog, which) -> {
                    boolean newState = !isCurrentlyEnabled;
                    sharedPreferences.edit().putBoolean("app_lock_enabled", newState).apply();
                    Toast.makeText(this, newState ? "App Lock Enabled!" : "App Lock Disabled!", Toast.LENGTH_SHORT).show();
                    if (newState) {
                        checkAndPromptBiometric();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkAndPromptBiometric() {
        boolean isLockEnabled = sharedPreferences.getBoolean("app_lock_enabled", false);
        if (!isLockEnabled) return;

        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(MainActivity.this, "Authentication required to access MindTracker", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                        }
                    });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("MindTracker Lock")
                    .setSubtitle("Authenticate to access your private mood journal")
                    .setAllowedAuthenticators(authenticators)
                    .build();

            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void showExportOptions() {
        String[] options = {"Save as CSV File", "Share Text Summary"};
        new AlertDialog.Builder(this)
                .setTitle("Export Mood Summary")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchSaveCsvPicker();
                    } else {
                        shareTextSummary();
                    }
                })
                .show();
    }

    private void launchSaveCsvPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "mind_tracker_summary.csv");
        exportDocumentLauncher.launch(intent);
    }

    private void writeCsvToUri(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                String csvContent = generateCsvString();
                os.write(csvContent.getBytes());
                Toast.makeText(this, "Summary exported successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generateCsvString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"Date\",\"Mood\",\"Activities/Tags\",\"Notes\"\n");

        Map<String, ?> allEntries = sharedPreferences.getAll();
        List<String> dates = new ArrayList<>();
        for (String key : allEntries.keySet()) {
            if (key.matches("\\d{4}_\\d{2}_\\d{2}") && allEntries.get(key) instanceof String) {
                dates.add(key);
            }
        }
        Collections.sort(dates, Collections.reverseOrder());

        for (String dateKey : dates) {
            String mood = (String) allEntries.get(dateKey);
            String note = sharedPreferences.getString(dateKey + "_note", "");
            String tags = sharedPreferences.getString(dateKey + "_tags", "");

            sb.append("\"").append(dateKey.replace("_", "-")).append("\",");
            sb.append("\"").append(mood != null ? mood : "").append("\",");
            sb.append("\"").append(tags.replace("\"", "\"\"")).append("\",");
            sb.append("\"").append(note.replace("\"", "\"\"")).append("\"\n");
        }

        return sb.toString();
    }

    private void shareTextSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 MindTracker Mood Summary\n");
        sb.append("Generated: ").append(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("---------------------------------\n\n");

        Map<String, ?> allEntries = sharedPreferences.getAll();
        List<String> dates = new ArrayList<>();
        for (String key : allEntries.keySet()) {
            if (key.matches("\\d{4}_\\d{2}_\\d{2}") && allEntries.get(key) instanceof String) {
                dates.add(key);
            }
        }
        Collections.sort(dates, Collections.reverseOrder());

        if (dates.isEmpty()) {
            sb.append("No mood entries recorded yet.");
        } else {
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            for (String dateKey : dates) {
                String mood = (String) allEntries.get(dateKey);
                String note = sharedPreferences.getString(dateKey + "_note", "");
                String tags = sharedPreferences.getString(dateKey + "_tags", "");

                String displayDate = dateKey;
                try {
                    Date parsedDate = storageFormat.parse(dateKey);
                    if (parsedDate != null) {
                        displayDate = displayFormat.format(parsedDate);
                    }
                } catch (Exception ignored) {}

                sb.append("• ").append(displayDate).append(": ").append(mood).append("\n");
                if (!tags.isEmpty()) {
                    sb.append("   Tags: ").append(tags).append("\n");
                }
                if (!note.isEmpty()) {
                    sb.append("   Note: “").append(note).append("”\n");
                }
                sb.append("\n");
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "MindTracker Mood Summary");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Mood Summary"));
    }

    // Helper method to get SharedPreferences from fragments
    public SharedPreferences getSharedPrefs() {
        return sharedPreferences;
    }
}