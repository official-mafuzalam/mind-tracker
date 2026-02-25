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
        // Set default background
        setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.mood_button_default)
        ));

        // Set corner radius
        setCornerRadius(getResources().getDimensionPixelSize(R.dimen.mood_button_corner));

        // FIXED: Use selector for text color instead of static color
        setTextColor(ContextCompat.getColorStateList(context, R.color.mood_button_text));

        // Set icon position
        setIconGravity(ICON_GRAVITY_TEXT_START);

        // Add padding
        setPadding(
                getResources().getDimensionPixelSize(R.dimen.mood_button_padding_start),
                getResources().getDimensionPixelSize(R.dimen.mood_button_padding_top),
                getResources().getDimensionPixelSize(R.dimen.mood_button_padding_end),
                getResources().getDimensionPixelSize(R.dimen.mood_button_padding_bottom)
        );

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
            // Highlight selected button
            setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.mood_button_stroke_selected));
            setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.primary)
            ));
            setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.mood_button_selected)
            ));
            animateSelection();
        } else {
            // Reset to normal
            setStrokeWidth(0);
            setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.mood_button_default)
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