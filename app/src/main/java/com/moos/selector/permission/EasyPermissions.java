package com.moos.selector.permission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.moos.selector.permission.helper.PermissionHelper;
import com.moos.selector.permission.my.PermBean;
import com.moos.selector.permission.my.PermissionInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EasyPermissions {
    private static final int APP_SETTINGS_RC = 7534;
    private static List<PermBean> permList = new ArrayList<>();
    private int requestCode;
    private String[] p;
    private PermissionHelper helper;

    private EasyPermissions(Activity host) {
        helper = PermissionHelper.newInstance(host);
    }

    private EasyPermissions(Fragment host) {
        helper = PermissionHelper.newInstance(host);
    }

    private EasyPermissions(android.app.Fragment host) {
        helper = PermissionHelper.newInstance(host);
    }

    public static EasyPermissions of(Activity host) {
        return new EasyPermissions(host);
    }

    public static EasyPermissions of(Fragment host) {
        return new EasyPermissions(host);
    }

    public static EasyPermissions of(android.app.Fragment host) {
        return new EasyPermissions(host);
    }

    //必选/////////////////////////////////////////////////////////////////////////////////////////////////////
    public EasyPermissions perms(@NonNull String... perms){
         p = perms;
         return this;
    }

    public EasyPermissions reqCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public void callBack(PermissionInfo info) {
        PermBean b = new PermBean();
        b.requestCode = requestCode;
        b.info = info;
        permList.add(b);

        request(helper, requestCode, p);
    }

    //必选/////////////////////////////////////////////////////////////////////////////////////////////////////
    private static void request(@NonNull PermissionHelper helper, int requestCode, @NonNull String... perms) {
        if (hasPermissions(helper.getContext(), perms)) {
            for (PermBean pb : permList) {
                if (pb.requestCode == requestCode) {
                    PermissionInfo pi = pb.info;
                    pi.onGranted(Arrays.asList(perms));
                    break;
                }
            }
            return;
        }

        // Request permissions
        helper.requestPermissions(requestCode, perms);
    }

    public static boolean hasPermissions(Context context, @NonNull String... perms) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (context == null) {
            throw new IllegalArgumentException("Can't check permissions for null context");
        }

        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    public static void onResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> granted = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm);
            } else {
                denied.add(perm);
            }
        }

        for (PermBean pb : permList) {
            if (pb.requestCode == requestCode) {
                PermissionInfo pi = pb.info;

                if (denied.isEmpty()) {
                    pi.onGranted(granted);
                } else {
                    pi.onDenied(denied);
                }

                break;
            }
        }

        Iterator<PermBean> it = permList.iterator();
        while(it.hasNext()){
            PermBean e = it.next();
            if(e.requestCode == requestCode){
                it.remove();
            }
        }
    }

    public static void toSettingAct(Activity act){
        act.startActivityForResult(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", act.getPackageName(), null)),
                APP_SETTINGS_RC);
    }
}
