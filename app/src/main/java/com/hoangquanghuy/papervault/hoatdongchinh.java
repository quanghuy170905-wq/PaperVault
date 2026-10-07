package com.hoangquanghuy.papervault;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class hoatdongchinh extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Intent intent;

        if (currentUser == null) {
            intent = new Intent(this, dangnhap.class);
        } else {
            intent = new Intent(this, mokhoa.class);
        }

        startActivity(intent);
        finish();
    }
}