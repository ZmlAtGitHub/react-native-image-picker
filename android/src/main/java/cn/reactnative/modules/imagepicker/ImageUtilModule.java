package cn.reactnative.modules.imagepicker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

/**
 * Created by lvbingru on 16/4/20.
 */
public class ImageUtilModule extends ReactContextBaseJavaModule {
    public ImageUtilModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "BBImageUtil";
    }

    @ReactMethod
    public void scaleImage(String imageTag, ReadableMap options, Promise promise) {
        new ResizeImage(getReactApplicationContext(), imageTag, options, promise)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class ResizeImage extends GuardedAsyncTask<Void, Void> {
        private final Context mContext;
        private final ReadableMap mOptions;
        private final Promise mPromise;
        private final String mSource;

        private Uri mUri;

        protected ResizeImage(ReactContext reactContext, String source, ReadableMap options, final Promise promise) {
            super(reactContext);
            mContext = reactContext;
            mOptions = options;
            mPromise = promise;
            mSource = source;
        }

        @Override
        protected void doInBackgroundGuarded(Void... params) {
            mUri = null;
            if (mSource != null) {
                try {
                    mUri = Uri.parse(mSource);
                    // Verify scheme is set, so that relative uri (used by static resources) are not handled.
                    if (mUri.getScheme() == null) {
                        mUri = null;
                    }
                } catch (Exception e) {
                    // ignore malformed uri, then attempt to extract resource ID.
                }
                if (mUri == null) {
                    mUri = _getResourceDrawableUri(mContext, mSource);
                }
            }

            int width = mOptions.getInt("width");
            int height = mOptions.getInt("height");
            ResizeOptions resizeOptions = new ResizeOptions(width, height);

            ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(mUri);
            if (resizeOptions != null) {
                builder = builder.setResizeOptions(resizeOptions);
            }
            ImageRequest imageRequest = builder.build();

            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);
            dataSource.subscribe(new DataSubscriber<CloseableReference<CloseableImage>>() {
                @Override
                public void onNewResult(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    double quality = 1.0;
                    if (mOptions.hasKey("quality")) {
                        quality = mOptions.getDouble("quality");
                    }
                    Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
                    if (mOptions.hasKey("type")) {
                        String type = mOptions.getString("type");
                        if (type.equals("png")) {
                            format = Bitmap.CompressFormat.PNG;
                        }
                    }
                    CloseableReference<CloseableImage> image = dataSource.getResult();
                    Drawable drawable = _createDrawable(image);
                    Bitmap bitmap = _drawable2Bitmap(drawable);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        if (bitmap.compress(
                                format,
                                (int) (quality * 100), out)) {
                            out.flush();
                            out.close();
                            mPromise.resolve(Base64.encodeToString(out.toByteArray(), Base64.DEFAULT));
                        } else {
                            mPromise.reject("-1", "compress failed");
                        }
                    } catch (Exception e) {
                        mPromise.reject("-1", e.getMessage());
                    }
                }

                @Override
                public void onFailure(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    mPromise.reject("-1", "failed");
                }

                @Override
                public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    mPromise.reject("-1", "canceled");
                }

                @Override
                public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {

                }
            }, UiThreadImmediateExecutorService.getInstance());
        }

        private static
        @Nullable
        Uri _getResourceDrawableUri(Context context, @Nullable String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            name = name.toLowerCase().replace("-", "_");
            int resId = context.getResources().getIdentifier(
                    name,
                    "drawable",
                    context.getPackageName());
            return new Uri.Builder()
                    .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                    .path(String.valueOf(resId))
                    .build();
        }

        private Drawable _createDrawable(CloseableReference<CloseableImage> image) {
            Preconditions.checkState(CloseableReference.isValid(image));
            CloseableImage closeableImage = image.get();
            if (closeableImage instanceof CloseableStaticBitmap) {
                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
                BitmapDrawable bitmapDrawable = new BitmapDrawable(
                        mContext.getResources(),
                        closeableStaticBitmap.getUnderlyingBitmap());
                if (closeableStaticBitmap.getRotationAngle() == 0 ||
                        closeableStaticBitmap.getRotationAngle() == EncodedImage.UNKNOWN_ROTATION_ANGLE) {
                    return bitmapDrawable;
                } else {
                    return new OrientedDrawable(bitmapDrawable, closeableStaticBitmap.getRotationAngle());
                }
            } else if (closeableImage instanceof CloseableAnimatedImage) {
                return Fresco.getImagePipelineFactory().getAnimatedDrawableFactory().create(
                        ((CloseableAnimatedImage) closeableImage).getImageResult());
            } else {
                throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
            }
        }

        private Bitmap _drawable2Bitmap(Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof NinePatchDrawable) {
                Bitmap bitmap = Bitmap
                        .createBitmap(
                                drawable.getIntrinsicWidth(),
                                drawable.getIntrinsicHeight(),
                                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                        : Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
                drawable.draw(canvas);
                return bitmap;
            } else {
                return null;
            }
        }
    }
}
