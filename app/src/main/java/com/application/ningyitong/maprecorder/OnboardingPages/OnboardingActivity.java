package com.application.ningyitong.maprecorder.OnboardingPages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.application.ningyitong.maprecorder.Account.LoginActivity;
import com.application.ningyitong.maprecorder.Account.SettingsActivity;
import com.application.ningyitong.maprecorder.Account.UserSessionManager;
import com.application.ningyitong.maprecorder.R;

public class OnboardingActivity extends AppCompatActivity {
    private ViewPager vpOnboarding;
    private LinearLayout mDotLayout;
    private TextView[] mDots;
    private Button mNextBtn;
    private Button mBackBtn;
    private int mCurrentPage;
    UserSessionManager session;
    OnboardingHelper onboardingHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new UserSessionManager(getApplicationContext());

        setContentView(R.layout.activity_onboarding);

        vpOnboarding = findViewById(R.id.onboarding_viewPager);
        mDotLayout = findViewById(R.id.onboarding_linearLayout);
        mNextBtn = findViewById(R.id.onboarding_next);
        mBackBtn = findViewById(R.id.onboarding_prev);

        onboardingHelper = new OnboardingHelper(this);
        vpOnboarding.setAdapter(onboardingHelper);

        addDotsIndicator(0);
        vpOnboarding.addOnPageChangeListener(viewListener);

        mNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPage = vpOnboarding.getCurrentItem() + 1;
                if (currentPage < mDots.length) {
                    vpOnboarding.setCurrentItem(mCurrentPage + 1);
                } else {
                    if (!session.checkLogin()) {
                        Intent intent_settings = new Intent(OnboardingActivity.this, SettingsActivity.class);
                        startActivity(intent_settings);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    } else {
                        Intent intent_login = new Intent(OnboardingActivity.this, LoginActivity.class);
                        startActivity(intent_login);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }

                }
            }
        });
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vpOnboarding.setCurrentItem(mCurrentPage - 1);
            }
        });
    }

    public void addDotsIndicator(int position){
        mDots = new TextView[3];
        mDotLayout.removeAllViews();

        for (int i=0; i<mDots.length; i++) {
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.colorTransparentWhite));

            mDotLayout.addView(mDots[i]);
        }
        if (mDots.length > 0) {
            mDots[position].setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }
        @SuppressLint("SetTextI18n")
        @Override
        public void onPageSelected(int i) {
            addDotsIndicator(i);
            mCurrentPage = i;
            if (i == 0) {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(false);
                mBackBtn.setVisibility(View.INVISIBLE);

                mNextBtn.setText("Next");
                mBackBtn.setText("");
            } else if (i == mDots.length - 1) {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Finish");
                mBackBtn.setText("Back");
            } else {
                mNextBtn.setEnabled(true);
                mBackBtn.setEnabled(true);
                mBackBtn.setVisibility(View.VISIBLE);

                mNextBtn.setText("Next");
                mBackBtn.setText("Back");
            }
        }
        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };
}
