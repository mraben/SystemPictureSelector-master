package com.moos.selector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.moos.selector.Util.ToastUtil;
import com.moos.selector.permission.EasyPermissions;

import java.io.File;
import java.util.List;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button button;
    private FrameLayout container;
    private ImageView imageView;
    public static final String TAG = "MainActivity";


    private SystemPictureSelector pictureSelector;
    private String savedPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".png";
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView() {

        button = findViewById(R.id.btn_select);
        //container = findViewById(R.id.image_container);
        imageView = findViewById(R.id.image_selected);
        imageView.setOnClickListener(this);
        button.setOnClickListener(this);
        loadGlideImage();
    }

    private void loadGlideImage() {

        SystemPictureSelector.Builder builder = new SystemPictureSelector.Builder(this);
        builder.isCropped(true)
                .setCropSize(3, 2)
                .setOutputPath(savedPath)
                .setOnSelectListener(new SystemPictureSelector.OnSystemPictureSelectListener() {
                    @Override
                    public void onSelectedSuccess(File file) {

                        Uri uri = Uri.fromFile(file);
                        Log.e(TAG, "onSelectedSuccess: " + uri.toString());

                        Glide.clear(imageView);
                        Glide.with(MainActivity.this)
                                .load(file)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(new SimpleTarget<GlideDrawable>() {
                                    @Override
                                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                                        imageView.setImageDrawable(resource);
                                    }
                                });
                        //uploadPhoto();
                    }

                    @Override
                    public void onSelectedMessage(String msg) {

                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
        pictureSelector = builder.create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        pictureSelector.bindingActivityForResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_selected:

                showSelectDialog();
                break;

            case R.id.btn_select:

                showSelectDialog();
                break;
        }
    }


    private void showSelectDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        View selectView = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null);
        TextView bt_camera = selectView.findViewById(R.id.btn_by_camera);
        TextView bt_gallery = selectView.findViewById(R.id.btn_by_gallery);
        TextView bt_cancel = selectView.findViewById(R.id.btn_cancel);
        dialog.setContentView(selectView);
        /**
         * 解决bsd显示不全的情况
         */
        View parent = (View) selectView.getParent();
        BottomSheetBehavior behavior = BottomSheetBehavior.from(parent);
        selectView.measure(0, 0);
        behavior.setPeekHeight(selectView.getMeasuredHeight());

        bt_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

                index = 1;
                verifyPeimissions();
                Log.d("bt_camera", 1111 + "");
            }
        });

        bt_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                index = 2;
                verifyPeimissions();
                Log.d("bt_gallery", 2222 + "");
            }
        });

        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    private void verifyPeimissions() {
        String[] permission = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.CAMERA
        };
        EasyPermissions.of(MainActivity.this)
                .reqCode(123)
                .perms(permission)
                .callBack(new com.moos.selector.permission.my.PermissionInfo() {
                    @Override
                    public void onGranted(List<String> perms) {
                        if (index == 1) {
                            Log.d("bt_camera", 3333 + "");
                            pictureSelector.getSystemPhotoByCamera();
                        } else if (index == 2) {
                            Log.d("bt_gallery", 4444 + "");
                            pictureSelector.getSystemPhotoByGallery();
                        }
                    }

                    @Override
                    public void onDenied(List<String> perms) {
                        ToastUtil.showToast(MainActivity.this, "您拒绝了权限，该功能不可用\n可在应用设置里授权拍照哦");

                    }
                });
    }
}
