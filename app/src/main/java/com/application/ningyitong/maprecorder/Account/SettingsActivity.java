package com.application.ningyitong.maprecorder.Account;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.application.ningyitong.maprecorder.OnboardingPages.OnboardingActivity;
import com.application.ningyitong.maprecorder.R;

public class SettingsActivity extends AppCompatActivity {

    ImageButton btnBack;
    Button btnReviewIntro, btnFeedback, btnReportBug;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnBack = findViewById(R.id.settings_page_back_btn);
        btnReviewIntro = findViewById(R.id.settings_page_review_intro_btn);
        btnFeedback = findViewById(R.id.settings_page_feedback_btn);
        btnReportBug = findViewById(R.id.settings_page_bug_intro_btn);


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btnReviewIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_onBoarding = new Intent(SettingsActivity.this, OnboardingActivity.class);
                startActivity(intent_onBoarding);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        btnFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendFeedback = new Intent(Intent.ACTION_SENDTO);
                String feedbackEmail = "mailto:" + Uri.encode("yning6@sheffield.ac.uk") +
                        "?subject=" + Uri.encode("MapRecorder Feedback") +
                        "&body=" + Uri.encode("Please write your feedback here, thank you :D");
                Uri uri = Uri.parse(feedbackEmail);
                sendFeedback.setData(uri);
                startActivity(Intent.createChooser(sendFeedback, "Send Email"));
            }
        });

        btnReportBug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendBugReport = new Intent(Intent.ACTION_SENDTO);
                String bugReportEmail = "mailto:" + Uri.encode("yning6@sheffield.ac.uk") +
                        "?subject=" + Uri.encode("MapRecorder Bugs Report") +
                        "&body=" + Uri.encode("Please describe bugs here, thank you! I would fix it ASAP :P");
                Uri uri = Uri.parse(bugReportEmail);
                sendBugReport.setData(uri);
                startActivity(Intent.createChooser(sendBugReport, "Send Email"));
            }
        });

    }
}
