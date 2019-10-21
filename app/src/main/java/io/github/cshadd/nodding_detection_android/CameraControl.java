package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;
import java.io.IOException;

public class CameraControl {
    private static final String TAG = "KAPLAN-1";

    private Activity activity;
    private Analyzer analyzer;
    private ImageAnalysis analyzerUseCase;
    private TextureView cameraPreviewView;
    private CameraX.LensFacing currentLensFacing;
    private View.OnLayoutChangeListener cameraPreviewViewLayoutChangeListener;
    private Runnable cameraPreviewViewRunnable;
    private HandlerThread imageAnalysisHandlerThread;
    private Preview preview;
    private Preview.OnPreviewOutputUpdateListener previewOutputUpdateListener;

    private CameraControl() {
        this(null);
        return;
    }

    public CameraControl(Activity activity) {
        super();
        this.activity = activity;
        this.currentLensFacing = CameraX.LensFacing.FRONT;
        this.imageAnalysisHandlerThread = new HandlerThread("Analyzer");
        return;
    }

    public void clearCapturedPosition() {
        this.analyzer.clearCapturedPosition();
        return;
    }

    public void onCreate() throws Exception {
        this.analyzer = new Analyzer(activity);
        this.analyzer.onCreate();
        this.cameraPreviewView = (TextureView)this.activity.findViewById(R.id.camera_preview_view);
        this.imageAnalysisHandlerThread.start();
        this.setupCamera(this.currentLensFacing);
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

    private void setupCamera(CameraX.LensFacing lensFacing) throws Exception {
        if (CameraX.hasCameraWithLensFacing(lensFacing)) {
            final ImageAnalysisConfig analyzerConfig = new ImageAnalysisConfig.Builder()
                    .setCallbackHandler(new Handler(this.imageAnalysisHandlerThread.getLooper()))
                    .setLensFacing(lensFacing)
                    .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_NEXT_IMAGE)
                    .build();
            if (this.analyzerUseCase != null) {
                this.analyzerUseCase.removeAnalyzer();
            }
            this.analyzerUseCase = new ImageAnalysis(analyzerConfig);

            if (this.preview != null
                    && this.previewOutputUpdateListener != null) {
                preview.removePreviewOutputListener();
            }
            this.preview = new Preview(new PreviewConfig.Builder()
                    .setLensFacing(lensFacing)
                    .build());

            if (this.cameraPreviewView != null) {
                if (this.cameraPreviewViewLayoutChangeListener != null) {
                    this.cameraPreviewView.removeOnLayoutChangeListener(this.cameraPreviewViewLayoutChangeListener);
                }

                if (this.cameraPreviewViewRunnable != null) {
                    this.cameraPreviewView.removeCallbacks(this.cameraPreviewViewRunnable);
                }
            }

            this.previewOutputUpdateListener = new Preview.OnPreviewOutputUpdateListener() {
                @Override
                public void onUpdated(Preview.PreviewOutput previewOutput) {
                    final ViewGroup parent = (ViewGroup)cameraPreviewView.getParent();
                    parent.removeView(cameraPreviewView);
                    cameraPreviewView.setSurfaceTexture(previewOutput.getSurfaceTexture());
                    parent.addView(cameraPreviewView);
                    updateTransform(cameraPreviewView);
                    return;
                }
            };

            this.cameraPreviewViewRunnable = new Runnable() {
                @Override
                public void run() {
                    CameraX.unbindAll();
                    preview.setOnPreviewOutputUpdateListener(previewOutputUpdateListener);
                    analyzerUseCase.setAnalyzer(analyzer);
                    CameraX.bindToLifecycle((LifecycleOwner)activity, analyzerUseCase, preview);
                    return;
                }
            };

            this.cameraPreviewViewLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                    updateTransform(cameraPreviewView);
                    return;
                }
            };

            this.cameraPreviewView.addOnLayoutChangeListener(this.cameraPreviewViewLayoutChangeListener);
            this.cameraPreviewView.post(this.cameraPreviewViewRunnable);
        }
        else {
            throw new Exception("No lens called " + lensFacing + "!");
        }
    }

    public void swapLens() throws Exception {
        if (this.currentLensFacing == CameraX.LensFacing.BACK) {
            this.currentLensFacing = CameraX.LensFacing.FRONT;
        }
        else {
            this.currentLensFacing = CameraX.LensFacing.BACK;
        }
        this.setupCamera(this.currentLensFacing);
        this.analyzer.swapLens();
        return;
    }

    private void updateTransform(TextureView view) {
        if (view != null) {
            final Matrix matrix = new Matrix();
            final float centerX = view.getWidth() / 2f;
            final float centerY = view.getHeight() / 2f;
            final int rawRot = view.getDisplay().getRotation();

            int rotation = 0;
            if (rawRot == Surface.ROTATION_0) {
                rotation = 0;
            } else if (rawRot == Surface.ROTATION_90) {
                rotation = 90;
            } else if (rawRot == Surface.ROTATION_180) {
                rotation = 180;
            } else if (rawRot == Surface.ROTATION_270) {
                rotation = 270;
            } else {
                return;
            }

            matrix.postRotate(-rotation, centerX, centerY);
            view.setTransform(matrix);
        }
        return;
    }
}
