package com.application.ningyitong.maprecorder.Account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.application.ningyitong.maprecorder.DatabaseHelper;
import com.application.ningyitong.maprecorder.MainActivity;
import com.application.ningyitong.maprecorder.OnboardingPages.OnboardingActivity;
import com.application.ningyitong.maprecorder.R;

public class LoginActivity extends Activity implements TextWatcher, CompoundButton.OnCheckedChangeListener {
    private static final String PREFERENCE_NAME = "prefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_KEEP = "keep";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String FIRST_RUN = "firstRun";
    private EditText emailText, passwordText;
    Button btnLogin, btnRegister;
    private DatabaseHelper db;
    private CheckBox rememberUser, keepSignIn;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    UserSessionManager session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // detect if app is first time running. If so, show onboarding page; if not, show login page
        if (sharedPreferences.getBoolean(FIRST_RUN, true)){
            editor.putBoolean(FIRST_RUN, false);
            editor.apply();
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        // create user login session
        session = new UserSessionManager(getApplicationContext());

        // get data from frontend
        db = new DatabaseHelper(this);
        emailText = findViewById(R.id.login_email);
        passwordText = findViewById(R.id.login_password);
        btnLogin = findViewById(R.id.login_loginBtn);
        rememberUser = findViewById(R.id.login_remember);
        keepSignIn = findViewById(R.id.login_keepSignIn);
        btnRegister = findViewById(R.id.login_registerBtn);

        // set remember me status
        if (sharedPreferences.getBoolean(KEY_REMEMBER, false))
            rememberUser.setChecked(true);
        else
            rememberUser.setChecked(false);
        // set keep me sign in status
        if (sharedPreferences.getBoolean(KEY_KEEP, false))
            keepSignIn.setChecked(true);
        else
            keepSignIn.setChecked(false);

        // save user info to sharedPreferences
        emailText.setText(sharedPreferences.getString(KEY_EMAIL, ""));
        passwordText.setText(sharedPreferences.getString(KEY_PASSWORD, ""));

        // call auto sign in method
        autoSignIn();

        // add text change listener
        emailText.addTextChangedListener(this);
        passwordText.addTextChangedListener(this);
        rememberUser.setOnCheckedChangeListener(this);
        keepSignIn.setOnCheckedChangeListener(this);

        // login button listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAction();
            }
        });

        // register button listener
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }

    // login method
    private void loginAction() {
        String username = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (username.equals("")) {
            emailText.setError("Input username");
            return;
        }
        if (password.equals("")) {
            passwordText.setError("Input password");
            return;
        }
        // verify user username or email and password
        Boolean loginValidation_username = db.loginValidation_username(username, password);
//        Boolean loginValidation_email = db.loginValidation_email(username, password);

        // validate usre login
        if (loginValidation_username) {
            db = new DatabaseHelper(this);
            // Get user_id first
            Cursor data = db.getUserID(username);
            int userID = -1;
            while (data.moveToNext()) {
                userID = data.getInt(0);
            }
            if (userID > -1) {
                // Get map number by searching use user_id
                Cursor userInfo = db.getUserInfoById(userID);
                userInfo.moveToFirst();
                // Save user_id into session
                session.createUserLoginSession(userID);
                Toast.makeText(getApplicationContext(), "Successfully Login", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();

            } else {
                Toast.makeText(getBaseContext(), "Database error!", Toast.LENGTH_SHORT).show();
            }

        } else
            Toast.makeText(getApplicationContext(), "Wrong username or password", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        managePreferences();
    }
    @Override
    public void afterTextChanged(Editable s) {
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        managePreferences();
    }

    // check or uncheck remember user
    private void managePreferences() {
        // checkbox Remember me
        if (rememberUser.isChecked()) {
            editor.putString(KEY_EMAIL, emailText.getText().toString().trim());
            editor.putString(KEY_PASSWORD, passwordText.getText().toString().trim());
            editor.putBoolean(KEY_REMEMBER, true);
            editor.apply();
        } else {
            editor.putBoolean(KEY_REMEMBER, false);
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
            editor.apply();
        }

        // checkbox Keep me sign in
        if (keepSignIn.isChecked()) {
            editor.putString(KEY_EMAIL, emailText.getText().toString().trim());
            editor.putString(KEY_PASSWORD, passwordText.getText().toString().trim());
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putBoolean(KEY_KEEP, true);
            editor.apply();
        } else {
            editor.putBoolean(KEY_KEEP, false);
            editor.apply();
        }
    }

    // verify auto sign in
    private void autoSignIn() {
        if (keepSignIn.isChecked()) {
            if (session.isUserLoggedIn())
                loginAction();
        }
    }
}
