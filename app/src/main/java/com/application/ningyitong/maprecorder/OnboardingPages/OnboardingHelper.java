package com.application.ningyitong.maprecorder.OnboardingPages;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.application.ningyitong.maprecorder.R;

public class OnboardingHelper extends PagerAdapter{
    private Context context;
    LayoutInflater layoutInflater;

    public OnboardingHelper(Context context) {
        this.context = context;
    }

    // add arrays
    public int[] onboarding_images = {
            R.drawable.ic_onboarding1,
            R.drawable.ic_onboarding2,
            R.drawable.ic_onboarding3
    };
    public String[] onboarding_heading = {
            "Tracking your GPS",
            "Editing Map",
            "Sharing Data"
    };
    public String[] onboarding_content = {
            "MapRecorder can record your route by accessing the phone GPS data",
            "You can add objects and modify your map",
            "You also can share your local map data with other users"
    };

    @Override
    public int getCount() {
        return onboarding_heading.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (RelativeLayout)object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.onboarding_layout, container, false);

        ImageView onboardingBackground = view.findViewById(R.id.onboarding_background);
        TextView onboardingHeading = view.findViewById(R.id.onboarding_heading);
        TextView onboardingContent = view.findViewById(R.id.onboarding_content);

        onboardingBackground.setImageResource(onboarding_images[position]);
        onboardingHeading.setText(onboarding_heading[position]);
        onboardingContent.setText(onboarding_content[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout)object);
    }
}
