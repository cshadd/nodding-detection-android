package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

public class CameraControl {
    private static final String TAG = "KAPLAN-1";

    private Activity activity;
    private CameraAnalyzer cameraAnalyzer;
    private TextureView cameraPreview;
    private HandlerThread imageAnalysisHandlerThread;

    private CameraControl() {
        this(null);
        return;
    }

    public CameraControl(Activity activity) {
        super();
        this.activity = activity;
        if (this.activity != null) {
            this.cameraAnalyzer = new CameraAnalyzer(activity);
            this.cameraPreview = (TextureView)this.activity.findViewById(R.id.camera_preview);
        }
        this.imageAnalysisHandlerThread = null;
        return;
    }

    public void onStop() {
        this.imageAnalysisHandlerThread.quitSafely();
        this.cameraAnalyzer.onStop();
        return;
    }

    public void start() {
        if (this.cameraPreview != null) {
            this.cameraPreview.post(new Runnable() {
                @Override
                public void run() {
                    startView();
                }
            });
            this.cameraPreview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                    updateTransform();
                }
            });
        }
        return;
    }

    private void startView() {
        final PreviewConfig config = new PreviewConfig.Builder().build();
        final Preview preview = new Preview(config);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        if (cameraPreview != null) {
                            cameraPreview.setSurfaceTexture(previewOutput.getSurfaceTexture());
                            updateTransform();
                        }
                    };
                });


        this.imageAnalysisHandlerThread = new HandlerThread(
                "CameraAnalysis");
        this.imageAnalysisHandlerThread.start();

        final ImageAnalysisConfig analyzerConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(this.imageAnalysisHandlerThread.getLooper()))
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();

        final ImageAnalysis analyzerUseCase = new ImageAnalysis(analyzerConfig);
        analyzerUseCase.setAnalyzer(this.cameraAnalyzer);

        CameraX.bindToLifecycle((LifecycleOwner) this.activity, preview, analyzerUseCase);
        return;
    }

    private void updateTransform() {
        if (cameraPreview != null) {

            final Matrix matrix = new Matrix();

            final float centerX = this.cameraPreview.getWidth() / 2f;
            final float centerY = this.cameraPreview.getHeight() / 2f;

            final int rawRot = this.cameraPreview.getDisplay().getRotation();
            int rotation = 0;
            if (rawRot == Surface.ROTATION_0) {
                rotation = 0;
            }
            else if (rawRot == Surface.ROTATION_90) {
                rotation = 90;
            }
            else if (rawRot == Surface.ROTATION_180) {
                rotation = 180;
            }
            else if (rawRot == Surface.ROTATION_270) {
                rotation = 270;
            }
            else {
                return;
            }

            matrix.postRotate(-rotation, centerX, centerY);

            this.cameraPreview.setTransform(matrix);
        }
        return;
    }
}
