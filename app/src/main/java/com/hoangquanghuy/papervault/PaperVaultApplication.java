package com.hoangquanghuy.papervault;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class PaperVaultApplication extends Application
        implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();

        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(this);
    }
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!VaultSession.isFilePickerOpen()) {
            VaultSession.lock();
        }
    }
}