package com.application.ningyitong.maprecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {
    Database db;
    EditText usernameText, emailText, passwordText, confirmPasswordText;
    Button registerBtn;
    TextView hasAccountText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_register);

        // Init UI items
        initVIew();
        db = new Database(this);


        registerBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String username = usernameText.getText().toString();
                String email = emailText.getText().toString();
                String pass = passwordText.getText().toString();
                String re_pass = confirmPasswordText.getText().toString();

                if (username.equals("")) {
                    usernameText.setError("Input username");
                }
                if (email.equals("")) {
                    emailText.setError("Input Email");
                    return;
                }
                if (pass.equals("")) {
                    passwordText.setError("Input password");
                    return;
                }
                if (re_pass.equals("")) {
                    confirmPasswordText.setError("Input confirm password");
                    return;
                }

                if (pass.equals(re_pass)) {
                    Boolean checkUsername = db.checkUsername(username);
                    Boolean checkEmail = db.checkEmail(email);
                    if (checkUsername) {
                        if (checkEmail) {
                            Boolean insert = db.saveUser(username, email, pass);
                            if (insert) {
                                Toast.makeText(getApplicationContext(), "Registered Successfully", Toast.LENGTH_SHORT).show();
                                renderToLogin();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Email already exists, please change to anther one.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Username already exists, please change to anther one.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Password do not match", Toast.LENGTH_SHORT).show();
                }
            }
        });
        hasAccountText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderToLogin();
            }
        });
    }

    // Init UI items
    private void initVIew() {
        usernameText = findViewById(R.id.register_username);
        emailText = findViewById(R.id.register_email);
        passwordText = findViewById(R.id.register_password);
        confirmPasswordText = findViewById(R.id.register_confirmPassword);
        registerBtn = findViewById(R.id.register_registerBtn);
        hasAccountText = findViewById(R.id.register_hasAccount);
    }

    // render page to login
    private void renderToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
