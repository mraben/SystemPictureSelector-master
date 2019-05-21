package com.moos.selector.permission.helper;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Delegate class to make permission calls based on the 'host' (Fragment, Activity, etc).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class PermissionHelper<T> {
    private T mHost;

    @NonNull
    public static PermissionHelper newInstance(Activity host) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return new LowApiPermissionsHelper(host);
        }

        if (host instanceof AppCompatActivity) {
            return new AppCompatActivityPermissionHelper((AppCompatActivity) host);
        } else {
            return new ActivityPermissionHelper(host);
        }
    }

    @NonNull
    public static PermissionHelper newInstance(Fragment host) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return new LowApiPermissionsHelper(host);
        }

        return new SupportFragmentPermissionHelper(host);
    }

    @NonNull
    public static PermissionHelper newInstance(android.app.Fragment host) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return new LowApiPermissionsHelper(host);
        }

        return new FrameworkFragmentPermissionHelper(host);
    }

    public PermissionHelper(@NonNull T host) {
        mHost = host;
    }

    public void requestPermissions(int requestCode, @NonNull String... perms) {
        directRequestPermissions(requestCode, perms);
    }

    @NonNull
    public T getHost() {
        return mHost;
    }

    public abstract void directRequestPermissions(int requestCode, @NonNull String... perms);

    public abstract Context getContext();

}
