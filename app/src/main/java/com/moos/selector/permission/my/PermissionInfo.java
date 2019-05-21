package com.moos.selector.permission.my;

import java.util.List;

/**
 * Created by Kellan on 2017/11/13.
 */

public interface PermissionInfo {
    void onGranted(List<String> perms);

    void onDenied(List<String> perms);
}
