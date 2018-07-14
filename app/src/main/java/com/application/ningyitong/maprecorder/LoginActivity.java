package com.application.ningyitong.maprecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
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

import javax.net.ssl.SSLSessionContext;

public class LoginActivity extends Activity implements TextWatcher, CompoundButton.OnCheckedChangeListener {
    private static final String PREFERENCE_NAME = "prefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_KEEP = "keep";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String FIRST_RUN = "firstRun";
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private Database db;
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
        db = new Database(this);
        etEmail = (EditText)findViewById(R.id.login_email);
        etPassword = (EditText)findViewById(R.id.login_password);
        btnLogin = (Button)findViewById(R.id.login_loginBtn);
        rememberUser = (CheckBox)findViewById(R.id.login_remember);
        keepSignIn = (CheckBox)findViewById(R.id.login_keepSignIn);
        btnRegister = (Button)findViewById(R.id.login_registerBtn);

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
        etEmail.setText(sharedPreferences.getString(KEY_EMAIL, ""));
        etPassword.setText(sharedPreferences.getString(KEY_PASSWORD, ""));

        // call auto sign in method
        autoSignIn();

        // add text change listener
        etEmail.addTextChangedListener(this);
        etPassword.addTextChangedListener(this);
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
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        // verify user email and password
        Boolean loginValidation = db.loginValidation(email, password);

        // validate usre login
        if (loginValidation) {
            session.createUserLoginSession(email);
            Toast.makeText(getApplicationContext(), "Successfully Login", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else
            Toast.makeText(getApplicationContext(), "Wrong email or password", Toast.LENGTH_SHORT).show();
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
            editor.putString(KEY_EMAIL, etEmail.getText().toString().trim());
            editor.putString(KEY_PASSWORD, etPassword.getText().toString().trim());
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
            editor.putString(KEY_EMAIL, etEmail.getText().toString().trim());
            editor.putString(KEY_PASSWORD, etPassword.getText().toString().trim());
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
