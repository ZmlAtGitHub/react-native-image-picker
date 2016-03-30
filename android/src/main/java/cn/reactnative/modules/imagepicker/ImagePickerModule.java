package cn.reactnative.modules.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by lvbingru on 11/5/15.
 */
public class ImagePickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    public ImagePickerModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    private static final String KEY_SOURCE_TYPE = "sourceType";
    private static final String KEY_SAVE_PHOTO = "savePhoto";
    private static final String KEY_VIDEO_MODE = "videoMode";
    private static final String KEY_VIDEO_QUALITY = "videoQuality";
    private static final String KEY_VIDEO_MAX_DURATION = "videoMaximumDuration";
    private static final String KEY_ALLOWS_EDITING = "allowsEditing";


    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
    private static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 102;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 103;

    @Override
    public String getName() {
        return "BBImagePicker";
    }

    Callback mResolve = null;
    Callback mReject = null;
    boolean mSavePhoto = false;
    boolean mAllowsEditing = false;
    Activity mActivity = null;

    private Uri mFileUri = null;

    @Override
    public void initialize() {
        super.initialize();
        getReactApplicationContext().addActivityEventListener(this);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        getReactApplicationContext().removeActivityEventListener(this);
    }

    @Override
    public Map<String, Object> getConstants() {

        final Map<String, Object> constants = new HashMap<>();
        constants.put("canRecordVideos", getReactApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
        constants.put("canUseCamera", getReactApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
        return constants;
    }

    @ReactMethod
    public void openCamera(ReadableMap option, Callback resolve, Callback reject) {
        mResolve = resolve;
        mReject = reject;

        String sourceType = "camera";
        if (option.hasKey(KEY_SOURCE_TYPE)) {
            sourceType = option.getString(KEY_SOURCE_TYPE);
        }

        boolean savePhoto = false;
        if (option.hasKey(KEY_SAVE_PHOTO)) {
           savePhoto =  option.getBoolean(KEY_SAVE_PHOTO);
        }
        mSavePhoto = savePhoto;

        boolean videoMode = false;
        if (option.hasKey(KEY_VIDEO_MODE)) {
            videoMode =  option.getBoolean(KEY_VIDEO_MODE);
        }

        boolean allowsEditing = false;
        if (option.hasKey(KEY_ALLOWS_EDITING)) {
            allowsEditing =  option.getBoolean(KEY_ALLOWS_EDITING);
        }
        mAllowsEditing = allowsEditing;

        mFileUri = null;
        Intent intent;
        int requestCode = CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE;
        if (sourceType.equals("camera")) {
            if (videoMode) {
                intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (option.hasKey(KEY_VIDEO_QUALITY)) {
                    double videoQuality = option.getDouble(KEY_VIDEO_QUALITY);
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, videoQuality);
                }
                if (option.hasKey(KEY_VIDEO_MAX_DURATION)) {
                    int videoMaxDuration = option.getInt(KEY_VIDEO_MAX_DURATION);
                    intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, videoMaxDuration);
                }
                mFileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
                requestCode = CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE;
            }
            else {
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                mFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
            }
        }
        else {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            requestCode = PICK_IMAGE_ACTIVITY_REQUEST_CODE;
        }

        if (mFileUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
        }

        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
            mActivity = activity;
        }
        else {
            reject("no activity");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
                resolve(mFileUri.toString());
            }
            else if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
                if (mSavePhoto) {
                    try {
                        File file = new File(mFileUri.getPath());
                        MediaStore.Images.Media.insertImage(getReactApplicationContext().getContentResolver(),
                                mFileUri.getPath(), file.getName(), null);
                        getReactApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mFileUri));
                    } catch (FileNotFoundException e) {
                        reject(e.toString());
                    }
                }
                if (mAllowsEditing) {
                    startCropIntent(mFileUri);
                }
                else {
                    resolve(mFileUri.toString());
                }
            }
            else if (requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE) {
                Uri uri = data.getData();
                if (mAllowsEditing) {
                    startCropIntent(uri);
                }
                else {
                    String path = getRealPath(uri);
                    resolve(path);
                }
            }
            else if (requestCode == CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                resolve(mFileUri.toString());
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            reject("Cancelled");
        }
        else {
            reject("ResultError");
        }
    }

    void startCropIntent(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        mFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);

        if (mActivity != null) {
            mActivity.startActivityForResult(intent, CROP_IMAGE_ACTIVITY_REQUEST_CODE);
        }
        else {
            reject("no activity");
        }
    }

    String getRealPath(Uri uri) {
        String[] filePathColumns={MediaStore.Images.Media.DATA};
        Cursor c = getReactApplicationContext().getContentResolver().query(uri, filePathColumns, null,null, null);
        if (c != null) {
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String path= c.getString(columnIndex);
            c.close();

            path = Uri.fromFile(new File(path)).toString();
            return path;
        }
        else {
            return uri.toString();
        }
    }

    void reject(String message) {
        if (mReject != null) {
            mReject.invoke(message);
        }
        mReject = null;
        mResolve = null;
    }

    void resolve(String path) {
        if (mResolve != null) {
            mResolve.invoke(path);
        }
        mReject = null;
        mResolve = null;
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private Uri getOutputMediaFileUri(int type)
    {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private File getOutputMediaFile(int type)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                .format(new Date());
        String fileName = "";
        if (type == MEDIA_TYPE_IMAGE)
        {
            fileName = "IMG_" + timeStamp + ".jpg";
        }
        else if (type == MEDIA_TYPE_VIDEO)
        {
            fileName = "VID_" + timeStamp + ".mp4";
        }

        File fileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getReactApplicationContext().getPackageName());
//        File fileDir;
//        if (mSavePhoto) {
//            fileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getReactApplicationContext().getPackageName());
//        }
//        else {
//            fileDir = getReactApplicationContext().getCacheDir();
//        }
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        File file = new File(fileDir, fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
