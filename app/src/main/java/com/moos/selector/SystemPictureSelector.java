package com.moos.selector;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * Created by moos on 2018/5/10.
 * <p>
 * <p>
 * the picture selector by system way, use {@link android.content.Intent} to realize it.
 * support camera photo and local gallery, we can also crop the picture as we want.
 * </p>
 */

public class SystemPictureSelector {

    public static final int TAKE_PHOTO = 101;
    public static final int LOCAL_GALLERY = 102;
    public static final String TAG = "SystemPictureSelector";
    public static final int REQUEST_TAKE_PHOTO = 2018;
    public static final int REQUEST_GALLERY_PHOTO = 2019;
    public static final int REQUEST_CROP_PICTURE = 2020;
    private Activity mContext;
    private int mBehavior = 1;
    private int mAspectX = 1;
    private int mAspectY = 1;
    private int mOutputX = 200;
    private int mOutputY = 200;
    private String mSavedPath;
    private boolean mWeatherCrop = true;
    private OnSystemPictureSelectListener mListener;

    private File mTargetPhoto;
    private File mOutputFile;

    public SystemPictureSelector(Builder builder) {

        this.mContext = builder.context;
        this.mAspectX = builder.aspectX;
        this.mAspectY = builder.aspectY;
        this.mOutputX = builder.outputX;
        this.mOutputY = builder.outputY;
        this.mWeatherCrop = builder.weatherCrop;
        this.mSavedPath = builder.savedPath;
        this.mListener = builder.listener;
    }

    Uri imgUri = null;

    /**
     * get picture by taking photo
     */
    public void getSystemPhotoByCamera() {
        mSavedPath = getSavedPath();
        mBehavior = TAKE_PHOTO;
        File imgFile = new File(mSavedPath);
        if (!imgFile.getParentFile().exists()) {
            imgFile.getParentFile().mkdirs();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 适配Android7.0通过contentResolver获取uri
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());
            imgUri = mContext.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        } else {
            // 7.0以下通过文件获取uri
            imgUri = Uri.fromFile(imgFile);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        mContext.startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    /**
     * get picture by local gallery
     */
    public void getSystemPhotoByGallery() {

        mBehavior = LOCAL_GALLERY;
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        mContext.startActivityForResult(intent, REQUEST_GALLERY_PHOTO);

    }

    /**
     * 采取系统自带方式裁剪图片
     *
     * @param targetPhoto 需要裁剪的目标图片
     * @param outputFile  我们指定的输出文件
     */
    private void cropPictureBySystemWay(File targetPhoto, File outputFile) {

        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // android7.0设置输出文件的uri
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setDataAndType(getUriByFileForN(mContext, targetPhoto), "image/*");
            //intent.setDataAndType(file2Uri(mContext,targetPhoto),"image/*");
        } else {
            intent.setDataAndType(Uri.fromFile(targetPhoto), "image/*");
        }

        //设置图片裁剪区域宽高比
        intent.putExtra("aspectX", mAspectX);
        intent.putExtra("aspectY", mAspectY);
        intent.putExtra("crop", "true");

        //设置图片裁剪后输出质量（一般来说，值越大，越清晰，但需要根据情况而定）
        //intent.putExtra("outputX", mOutputX);
        //intent.putExtra("outputY", mOutputY);
        intent.putExtra("scale", true);//支持缩放
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);//取消人脸识别
        mContext.startActivityForResult(intent, REQUEST_CROP_PICTURE);
    }

    /**
     * 根据文件转换成对应的Uri
     *
     * @param ctx
     * @param file
     * @return
     */
    public static Uri file2Uri(Context ctx, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    /**
     * Android7.0根据文件获取uri
     */
    private Uri getUriByFileForN(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private String getSavedPathByTime() {
        return getExternalStoragePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".png";
    }

    private String getPhotoPathByUri(Intent data) {
        Uri sourceUri = data.getData();
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.managedQuery(sourceUri, projection, null, null, null);
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String imgPath = cursor.getString(columnIndex);
        return imgPath;
    }

    /**
     * 获取SD下的对应的应用路径
     */
    private String getExternalStoragePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        sb.append(File.separator);
        String ROOT_DIR = "Android/data/" + mContext.getPackageName();
        sb.append(ROOT_DIR);
        sb.append(File.separator);
        return sb.toString();
    }

    /**
     * by moos on 2018/05/12
     * desc:绑定所在activity的onActivityForResult方法，处理回调数据
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data        返回结果
     */
    public void bindingActivityForResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO:
                    // 系统拍照
                    mTargetPhoto = new File(mSavedPath);
                    if (mWeatherCrop) {
                        // 去裁剪
                        mOutputFile = new File(getSavedPathByTime());
                        cropPictureBySystemWay(mTargetPhoto, mOutputFile);
                    } else {
                        // 不裁剪
                        if (mListener != null) {
                            mListener.onSelectedSuccess(mTargetPhoto);
                            mListener.onSelectedMessage("拍照成功～");
                        }
                    }
                    break;

                case REQUEST_GALLERY_PHOTO:
                    // 本地图库
                    if (data != null) {
                        mTargetPhoto = new File(getPhotoPathByUri(data));
                        if (mWeatherCrop) {
                            // 去裁剪
                            mOutputFile = new File(getSavedPathByTime());
                            cropPictureBySystemWay(mTargetPhoto, mOutputFile);
                        } else {
                            // 不裁剪
                            if (mListener != null) {
                                mListener.onSelectedSuccess(mTargetPhoto);
                                mListener.onSelectedMessage("从本地选取图片成功～");
                            }
                        }
                    } else {
                        mListener.onSelectedMessage("从本地图库获取到的数据为空！");
                    }

                    break;

                case REQUEST_CROP_PICTURE:
                    // 系统裁剪
                    if (data != null) {
                        Log.d("----------data != ", "null");
                        if (mBehavior == TAKE_PHOTO && mTargetPhoto != null) {


                        }
                        LuBanYaSuo(mOutputFile);
                    } else {
                        Log.d("---------data = ", "null");
                        mListener.onSelectedMessage("系统裁剪失败，无法获取裁剪数据");
                    }
                    break;
            }
        }
    }

    private void LuBanYaSuo(File file) {
        Luban.with(mContext)
                .load(file.getAbsolutePath())                   // 传人要压缩的图片列表
                .ignoreBy(100)                                  // 忽略不压缩图片的大小
                .setTargetDir(getYaSuoPath())             // 设置压缩后文件存储位置
                .setCompressListener(new OnCompressListener() { //设置回调
                    @Override
                    public void onStart() {
                        // TODO 压缩开始前调用，可以在方法内启动 loading UI
                    }

                    @Override
                    public void onSuccess(File file) {
                        // TODO 压缩成功后调用，返回压缩后的图片文件
                        if (mListener != null) {
                            mListener.onSelectedSuccess(file);
                            mListener.onSelectedMessage("裁剪成功～");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 当压缩过程出现问题时调用
                    }
                }).launch();
    }

    private String getYaSuoPath() {
        String path = Environment.getExternalStorageDirectory() + "/Luban/image/";
        File file = new File(path);
        if (file.mkdirs()) {
            return path;
        }
        return path;
    }

    private String getSavedPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".png";
    }

    public static class Builder {

        private Activity context;
        private OnSystemPictureSelectListener listener;
        private int aspectX = 1;
        private int aspectY = 1;
        private int outputX = 200;
        private int outputY = 200;
        private String savedPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".png";
        private boolean weatherCrop = true;

        public Builder(Activity context) {
            this.context = context;
        }

        public Builder isCropped(boolean weatherCrop) {
            this.weatherCrop = weatherCrop;
            return this;
        }


        /**
         * 为图片设置裁剪的区域比例
         *
         * @param aspectX width aspect
         * @param aspectY height aspect
         * @return
         */
        public Builder setCropSize(int aspectX, int aspectY) {
            this.aspectX = aspectX;
            this.aspectY = aspectY;
            return this;
        }

        /**
         * 为裁剪后的图片设置输出质量
         *
         * @param outputX output width quality
         * @param outputY output height quality
         * @return
         */
        public Builder setOutputSize(int outputX, int outputY) {
            this.outputX = outputX;
            this.outputY = outputY;
            return this;
        }

        /**
         * 为图片设置存储路径
         *
         * @param path saved path you wanted
         * @return
         */
        public Builder setOutputPath(String path) {
            this.savedPath = path;
            return this;
        }

        public Builder setOnSelectListener(OnSystemPictureSelectListener listener) {
            this.listener = listener;
            return this;
        }

        public SystemPictureSelector create() {
            return new SystemPictureSelector(this);
        }
    }

    interface OnSystemPictureSelectListener {
        void onSelectedSuccess(File file);

        void onSelectedMessage(String message);
    }
}
