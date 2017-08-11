package com.hift.nameofthings;

import android.app.ProgressDialog;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by TeguhAS on 31-Jul-17.
 */

public class Preview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;

    SurfaceHolder mHolder;

    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;

    Camera.PictureCallback jpegCallback;

    byte[] pictureByteArray = null;
    Bitmap takenPicture;

    ProgressDialog dialog; //progress dialog to be closed
    SurfaceHolder drawHolder; //surface holder to be drawn on

    public Preview(final Context context) {
        super(context);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                Bitmap bmp = BitmapFactory.decodeStream(byteArrayInputStream);

                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                //rotate 90 deg
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                int size = CameraActivity.sz;
                int halfWidth = CameraActivity.halfWidth;
                int halfHeight = CameraActivity.halfHeight;
                int x = halfWidth - size;
                int y = halfHeight - size;
                int rectSize = size*2;
                //crop
                bmp = Bitmap.createBitmap(bmp, x, y, rectSize, rectSize);
                //scale
                bmp = Bitmap.createScaledBitmap(bmp, 250, 250, true);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream);
                byte[] byteArray = stream.toByteArray();

                pictureByteArray = byteArray;

                UploadTask task = new UploadTask();

                UploadBean uploadBean = new UploadBean();
                uploadBean.setDialog(dialog);
                uploadBean.setHolder(drawHolder);
                uploadBean.setPictureBytes(pictureByteArray);
                uploadBean.setContext(context);
                uploadBean.setApiKey(CameraActivity.API_KEY);

                task.execute(uploadBean);

                refreshCamera();
            }
        };
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            mCamera.setDisplayOrientation(90);
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int croppedWidth = 1080;
        int croppedHeight = 1920;
        setMeasuredDimension(croppedWidth, croppedHeight);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, croppedWidth, croppedHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
//            if (mPreviewSize != null) {
//                previewWidth = mPreviewSize.width;
//                previewHeight = mPreviewSize.height;
//            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            setWillNotDraw(false);
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / (double) h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / (double) size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureFormat(PixelFormat.JPEG);
            requestLayout();

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

    public void takePicture(ProgressDialog dialog, SurfaceHolder holder) {
        this.drawHolder = holder;
        this.dialog = dialog;
        mCamera.takePicture(null, null, jpegCallback);
    }

    public void refreshCamera() {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {

        }
    }

    public Bitmap getPicture() {
        return this.takenPicture;
    }

    public byte[] getPictureByteArray() {
        return this.pictureByteArray;
    }
}
