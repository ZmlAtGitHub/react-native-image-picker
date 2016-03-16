package cn.reactnative.modules;

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
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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


    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;

    @Override
    public String getName() {
        return "BBImagePicker";
    }

    Callback mResolve = null;
    Callback mReject = null;
    boolean mSavePhoto = false;

    private Uri fileUri;

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
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
            }
            else {
                intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        }
        else {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            requestCode = PICK_IMAGE_ACTIVITY_REQUEST_CODE;
        }

        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
        }
        else {
            reject("no activity");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
                if (mSavePhoto) {
                    try {
                        File file = new File(fileUri.getPath());
                        MediaStore.Images.Media.insertImage(getReactApplicationContext().getContentResolver(),
                                fileUri.getPath(), file.getName(), null);
                        getReactApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri));
                    } catch (FileNotFoundException e) {
                        reject(e.toString());
                    }
                }
                resolve(fileUri.toString());
            }
            else if (requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE) {
                Uri selectedImage = data.getData();
                String[] filePathColumns={MediaStore.Images.Media.DATA};
                Cursor c = getReactApplicationContext().getContentResolver().query(selectedImage, filePathColumns, null,null, null);
                if (c != null) {
                    c.moveToFirst();
                    int columnIndex = c.getColumnIndex(filePathColumns[0]);
                    String path= c.getString(columnIndex);
                    c.close();

                    path = Uri.fromFile(new File(path)).toString();
                    resolve(path);
                }
                else {
                    reject("error");
                }
            }

        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            reject("Cancelled");
        }
        else {
            reject("ResultError");
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
        getReactApplicationContext().getPackageName();
        File mediaStorageDir = null;
        try
        {
            mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "camera");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (!mediaStorageDir.exists())
        {
            if (!mediaStorageDir.mkdirs())
            {
                // <uses-permission
                // android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        }
        else if (type == MEDIA_TYPE_VIDEO)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        }
        else
        {
            return null;
        }

        return mediaFile;
    }
}
