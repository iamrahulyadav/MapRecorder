package com.application.ningyitong.maprecorder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class EditProfileActivity extends AppCompatActivity {

    ImageButton btnBack, editUsernameBtn, editEmailBtn;
    EditText editUsernameText, editEmailText;
    Database db;
    private Dialog changePasswordDialog;

    // user session
    UserSessionManager session;
    int userID;
    String username, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check user session
        checkSession();

        setContentView(R.layout.activity_edit_profile);
        // get user data
        HashMap<String, Integer> user = session.getUserDetails();
        userID = user.get(UserSessionManager.KEY_USERID);

        btnBack = (ImageButton)findViewById(R.id.edit_profile_page_back_btn);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // Init UI items
        initView();
        // Setup change password dialog
        changePasswordDialog = new Dialog(this);

        // Show current username and email
        showUserInfo();
        // Change user name
        changeUsername();
        // Change user email
        changeEmail();

    }

    // Show change password dialog
    public void ShowChangePasswordDialog(View view) {
        // Define parameters
        ImageButton closeDialogBtn;
        final EditText currentPasswordText, newPasswordText, rePasswordText;
        Button cancelChangePassBtn, confirmChangePassBtn;
        // Init change password dialog
        changePasswordDialog.setContentView(R.layout.change_password_dialog);

        closeDialogBtn = (ImageButton)changePasswordDialog.findViewById(R.id.change_password_close_btn);
        currentPasswordText = (EditText)changePasswordDialog.findViewById(R.id.change_password_old_pass_text);
        newPasswordText = (EditText)changePasswordDialog.findViewById(R.id.change_password_new_pass_text);
        rePasswordText = (EditText)changePasswordDialog.findViewById(R.id.change_password_re_pass_text);
        cancelChangePassBtn = (Button)changePasswordDialog.findViewById(R.id.change_password_cancel_btn);
        confirmChangePassBtn = (Button)changePasswordDialog.findViewById(R.id.change_password_confirm_btn);

        // Set close dialog button
        closeDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePasswordDialog.dismiss();
            }
        });
        cancelChangePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePasswordDialog.dismiss();
            }
        });

        // Change password
        confirmChangePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentPassword = currentPasswordText.getText().toString();
                String newPassword = newPasswordText.getText().toString();
                String rePassword = rePasswordText.getText().toString();

                if (currentPassword.equals("")) {
                    currentPasswordText.setError("Input current password");
                    return;
                }
                if (newPassword.equals("")) {
                    newPasswordText.setError("Input new password");
                    return;
                }
                if (rePassword.equals("")) {
                    rePasswordText.setError("Confirm new password");
                    return;
                }
                if (newPassword.equals(rePassword)) {
                    if (db.loginValidation_userID(userID, currentPassword)) {
                        db.updatePassword(newPassword, currentPassword, userID);
                        Toast.makeText(getBaseContext(), "Update password successful, please login again.", Toast.LENGTH_LONG).show();
                        changePasswordDialog.dismiss();
                        session.logoutUser();
                    } else {
                        Toast.makeText(getBaseContext(), "Please input correct current password.", Toast.LENGTH_LONG).show();
                    }
                } else
                    Toast.makeText(getBaseContext(), "Re-pasword and New password should the same.", Toast.LENGTH_LONG).show();
            }
        });

        changePasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        changePasswordDialog.show();
    }

    public void initChangePasswordDialog() {
        ImageButton closeDialogBtn;
        final EditText currentPasswordText, newPasswordText, rePasswordText;
        Button cancelChangePassBtn, confirmChangePassBtn;

        closeDialogBtn = (ImageButton)findViewById(R.id.change_password_close_btn);
        currentPasswordText = (EditText)findViewById(R.id.change_password_old_pass_text);
        newPasswordText = (EditText)findViewById(R.id.change_password_new_pass_text);
        rePasswordText = (EditText)findViewById(R.id.change_password_re_pass_text);
        cancelChangePassBtn = (Button)findViewById(R.id.change_password_cancel_btn);
        confirmChangePassBtn = (Button)findViewById(R.id.change_password_confirm_btn);
    }

    // Change user Email
    private void changeEmail() {
        editEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newUserEmail = editEmailText.getText().toString();
                if (db.checkEmail(newUserEmail)) {
                    db.updateEmail(newUserEmail, userID);
                    Toast.makeText(getBaseContext(), "Update email successful", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getBaseContext(), "The Email address has been registered, please change to another one.", Toast.LENGTH_LONG).show();
            }
        });
    }
    // Change user name
    private void changeUsername() {
        editUsernameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newUsername = editUsernameText.getText().toString();
                if (newUsername.equals(username)) {
                    Toast.makeText(getBaseContext(), "No chages are made.", Toast.LENGTH_SHORT).show();
                } else if (db.checkUsername(newUsername)) {
                    db.updateUsername(newUsername, userID);
                    Toast.makeText(getBaseContext(), "Update username successful, please login again!", Toast.LENGTH_SHORT).show();
                    session.logoutUser();
                } else {
                    Toast.makeText(getBaseContext(), "Username exists, please change to another one.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Check user session
    private void checkSession() {
        // session class instance
        session = new UserSessionManager(getApplicationContext());
        // Check user login status
        if (session.checkLogin()) {
            finish();
        }
    }

    // Show user info
    private void showUserInfo() {
        db = new Database(this);
        Cursor userInfo = db.getUserInfoById(userID);
        userInfo.moveToFirst();
        username = userInfo.getString(userInfo.getColumnIndex("username"));
        email = userInfo.getString(userInfo.getColumnIndex("email"));
        editUsernameText.setText(username);
        editEmailText.setText(email);
    }

    // Init buttons, text fields
    private void initView() {
        editUsernameBtn = (ImageButton)findViewById(R.id.edit_profile_change_username_btn);
        editEmailBtn = (ImageButton)findViewById(R.id.edit_profile_change_email_btn);
        editUsernameText = (EditText)findViewById(R.id.edit_profile_username_text);
        editEmailText = (EditText)findViewById(R.id.edit_profile_email_text);
    }
}
