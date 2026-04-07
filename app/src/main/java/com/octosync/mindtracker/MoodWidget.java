package com.octosync.mindtracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 */
public class MoodWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mood_widget);

        // Get today's mood from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("MoodPrefs", Context.MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
        String mood = prefs.getString(todayDate, "Not set yet");

        views.setTextViewText(R.id.widget_mood_text, mood);

        // Update icon based on mood
        int iconRes = R.drawable.ic_mood;
        if (mood.equals("Happy")) iconRes = R.drawable.ic_mood_happy;
        else if (mood.equals("Neutral")) iconRes = R.drawable.ic_mood_neutral;
        else if (mood.equals("Sad")) iconRes = R.drawable.ic_mood_sad;
        else if (mood.equals("Angry")) iconRes = R.drawable.ic_mood_angry;
        else if (mood.equals("Tired")) iconRes = R.drawable.ic_mood_tired;
        
        views.setImageViewResource(R.id.widget_icon, iconRes);

        // Create an Intent to launch MainActivity when clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName watchWidget = new ComponentName(context, MoodWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(watchWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}
