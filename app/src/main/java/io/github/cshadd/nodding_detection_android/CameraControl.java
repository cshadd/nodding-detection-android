package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;
import java.io.IOException;

public class CameraControl {
    private static final CameraX.LensFacing LENS_FACING = CameraX.LensFacing.FRONT;
    private static final String TAG = "KAPLAN-1";

    private Activity activity;
    private Analyzer analyzer;
    private ImageAnalysis analyzerUseCase;
    private TextureView cameraPreviewView;
    private View.OnLayoutChangeListener cameraPreviewViewLayoutChangeListener;
    private Runnable cameraPreviewViewRunnable;
    private HandlerThread imageAnalysisHandlerThread;
    private Preview preview;
    private Preview.OnPreviewOutputUpdateListener previewOutputUpdateListener;

    private CameraControl() {
        this(null);
        return;
    }

    public CameraControl(final Activity activity) {
        super();
        this.activity = activity;
        this.imageAnalysisHandlerThread = new HandlerThread("Analyzer");
        this.imageAnalysisHandlerThread.start();
        final ImageAnalysisConfig analyzerConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(this.imageAnalysisHandlerThread.getLooper()))
                .setLensFacing(CameraControl.LENS_FACING)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_NEXT_IMAGE)
                .build();
        this.analyzerUseCase = new ImageAnalysis(analyzerConfig);

        final PreviewConfig config = new PreviewConfig.Builder()
                .setLensFacing(CameraControl.LENS_FACING)
                .build();
        this.preview = new Preview(config);
        return;
    }

    public void clearCapturedPosition() {
        this.analyzer.clearCapturedPosition();
        return;
    }

    public void onCreate() throws Exception {
        if (CameraX.hasCameraWithLensFacing(CameraControl.LENS_FACING)) {
            if (this.activity != null) {
                this.cameraPreviewView = (TextureView)this.activity.findViewById(R.id.camera_preview_view);
            }
            this.cameraPreviewViewRunnable = new Runnable() {
                @Override
                public void run() {
                    preview.setOnPreviewOutputUpdateListener(previewOutputUpdateListener);
                    analyzerUseCase.setAnalyzer(analyzer);
                    CameraX.bindToLifecycle((LifecycleOwner)activity, analyzerUseCase, preview);
                }
            };

            this.cameraPreviewViewLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                    updateTransform(cameraPreviewView);
                }
            };

            this.previewOutputUpdateListener = new Preview.OnPreviewOutputUpdateListener() {
                @Override
                public void onUpdated(Preview.PreviewOutput previewOutput) {
                    if (cameraPreviewView != null) {
                        cameraPreviewView.setSurfaceTexture(previewOutput.getSurfaceTexture());
                        updateTransform(cameraPreviewView);
                    }
                }
            };

            this.analyzer = new Analyzer(activity);
            this.analyzer.onCreate();
            this.analyzerUseCase.setAnalyzer(this.analyzer);
            this.cameraPreviewView.post(this.cameraPreviewViewRunnable);
            this.cameraPreviewView.addOnLayoutChangeListener(this.cameraPreviewViewLayoutChangeListener);
            this.preview.setOnPreviewOutputUpdateListener(this.previewOutputUpdateListener);
        }
        else {
            throw new Exception("Unable to detect front camera!");
        }
        return;
    }

    public void onDestroy() throws IOException {
        this.analyzer.onDestroy();
        this.preview.removePreviewOutputListener();
        this.cameraPreviewView.removeOnLayoutChangeListener(this.cameraPreviewViewLayoutChangeListener);
        this.cameraPreviewView.removeCallbacks(this.cameraPreviewViewRunnable);
        this.analyzerUseCase.removeAnalyzer();
        CameraX.unbindAll();
        this.imageAnalysisHandlerThread.getLooper().quitSafely();
        this.imageAnalysisHandlerThread.quitSafely();
        return;
    }

    private void updateTransform(TextureView view) {
        final Matrix matrix = new Matrix();
        final float centerX = view.getWidth() / 2f;
        final float centerY = view.getHeight() / 2f;
        final int rawRot = this.cameraPreviewView.getDisplay().getRotation();

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
        this.cameraPreviewView.setTransform(matrix);
        return;
    }
}
