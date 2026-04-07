package com.octosync.mindtracker;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class MoodButton extends MaterialButton {

    private String moodType;
    private boolean isSelected = false;
    private static final int ANIMATION_DURATION = 200;

    // Constructors
    public MoodButton(Context context) {
        super(context);
        init(context);
    }

    public MoodButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MoodButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // Constructor with mood type for easy creation
    public MoodButton(Context context, String moodType, int iconRes) {
        super(context);
        this.moodType = moodType;
        setText(moodType);
        if (iconRes != 0) {
            setIcon(ContextCompat.getDrawable(context, iconRes));
        }
        init(context);
    }

    private void init(Context context) {
        // Set default background to surface variant for modern look
        setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.surface_variant)
        ));

        // Set corner radius to be more rounded
        setCornerRadius(getResources().getDimensionPixelSize(R.dimen.mood_button_corner));

        // Use modern text color
        setTextColor(ContextCompat.getColor(context, R.color.on_surface));

        // Set icon position and size
        setIconGravity(ICON_GRAVITY_TEXT_START);
        setIconPadding(16);
        setIconTintResource(R.color.on_surface);

        // Remove uppercase
        setAllCaps(false);
        setLetterSpacing(0.02f);
        setTextSize(16f);

        // Set elevation for better look
        setElevation(0f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            animatePress();
        } else if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            animateRelease();
        }
        return super.onTouchEvent(event);
    }

    private void animatePress() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.97f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.97f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.setDuration(80);
        animSet.start();
    }

    private void animateRelease() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0.97f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0.97f, 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.setDuration(80);
        animSet.start();
    }

    public void animateSelection() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.05f, 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.setDuration(ANIMATION_DURATION);
        animSet.start();
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (selected) {
            // Highlight selected button with modern Material 3 style
            setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.mood_button_stroke_selected));
            setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.id.btnHappy == getId() ? R.color.brand_primary : R.color.brand_primary)
            ));
            
            // Apply mood-specific background colors if selected
            int bgColor = R.color.brand_primary_container;
            if (getId() == R.id.btnHappy) bgColor = R.color.mood_happy;
            else if (getId() == R.id.btnNeutral) bgColor = R.color.mood_neutral;
            else if (getId() == R.id.btnSad) bgColor = R.color.mood_sad;
            else if (getId() == R.id.btnAngry) bgColor = R.color.mood_angry;
            else if (getId() == R.id.btnTired) bgColor = R.color.mood_tired;
            
            setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), bgColor)
            ));
            animateSelection();
        } else {
            // Reset to normal surface variant
            setStrokeWidth(0);
            setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.surface_variant)
            ));
        }
        // Force text color update
        refreshDrawableState();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public String getMoodType() {
        return moodType != null ? moodType : getText().toString();
    }

    public void setMoodType(String moodType) {
        this.moodType = moodType;
        setText(moodType);
    }
}