package com.application.ningyitong.maprecorder;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {
    Database db;
    EditText emailText, passwordText, confirmPasswordText;
    Button registerBtn;
    TextView hasAccountText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_register);

        db = new Database(this);
        emailText = (EditText)findViewById(R.id.register_email);
        passwordText = (EditText)findViewById(R.id.register_password);
        confirmPasswordText = (EditText)findViewById(R.id.register_confirmPassword);
        registerBtn = (Button)findViewById(R.id.register_registerBtn);
        hasAccountText = (TextView)findViewById(R.id.register_hasAccount);

        registerBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String email = emailText.getText().toString();
                String pass = passwordText.getText().toString();
                String re_pass = confirmPasswordText.getText().toString();

                if (email.equals("")||pass.equals("")||re_pass.equals("")) {
                    Toast.makeText(getApplicationContext(), "Fields should not be empty", Toast.LENGTH_SHORT).show();
                } else {
                    if (pass.equals(re_pass)) {
                        Boolean checkEmail = db.checkEmail(email);
                        if (checkEmail) {
                            Boolean insert = db.saveUser(email, pass);
                            if (insert) {
                                Toast.makeText(getApplicationContext(), "Registered Successfully", Toast.LENGTH_SHORT).show();
                                renderToLogin();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Email Already Exists", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Password do not match", Toast.LENGTH_SHORT).show();
                    }
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

    // render page to login
    private void renderToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
