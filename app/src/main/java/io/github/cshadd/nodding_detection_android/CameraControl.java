package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.lifecycle.LifecycleOwner;
import java.io.IOException;

public class CameraControl {
    private static final String TAG = "KAPLAN-1";

    private Activity activity;
    private Analyzer analyzer;
    private ImageAnalysis analyzerUseCase;
    private HandlerThread imageAnalysisHandlerThread;

    private CameraControl() {
        this(null);
        return;
    }

    public CameraControl(Activity activity) {
        super();
        this.activity = activity;
        if (this.activity != null) {
            this.analyzer = new Analyzer(activity);
        }
        this.imageAnalysisHandlerThread = new HandlerThread("Analyzer");
        this.imageAnalysisHandlerThread.start();
        final ImageAnalysisConfig analyzerConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(this.imageAnalysisHandlerThread.getLooper()))
                .setLensFacing(CameraX.LensFacing.FRONT)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_NEXT_IMAGE)
                .build();
        this.analyzerUseCase = new ImageAnalysis(analyzerConfig);
        return;
    }

    public void onCreate() throws CameraInfoUnavailableException, Exception {
        if (CameraX.hasCameraWithLensFacing(CameraX.LensFacing.FRONT)) {
            this.analyzerUseCase.setAnalyzer(this.analyzer);
            CameraX.bindToLifecycle((LifecycleOwner) this.activity, analyzerUseCase);
        }
        else {
            throw new Exception("Unable to detect front camera!");
        }
        return;
    }

    public void onDestroy() throws IOException {
        this.analyzer.onDestroy();
        this.imageAnalysisHandlerThread.getLooper().quitSafely();
        this.imageAnalysisHandlerThread.quitSafely();
        return;
    }

    public void onResume() {
        this.analyzerUseCase.setAnalyzer(this.analyzer);
        return;
    }

    public void onStop() {
        this.analyzerUseCase.removeAnalyzer();
        return;
    }
}
