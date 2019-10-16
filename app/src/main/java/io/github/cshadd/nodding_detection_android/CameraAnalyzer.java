package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraAnalyzer
        implements ImageAnalysis.Analyzer {
    private static final String TAG = "KAPLAN-2";

    private Activity activity;
    private FirebaseVisionFaceDetector detector;
    private Button faceBounds;
    private long lastAnalyzedTimestamp;

    private CameraAnalyzer() {
        this(null);
        return;
    }

    public CameraAnalyzer(Activity activity) {
        super();
        this.activity = activity;
        if (this.activity != null) {
            this.faceBounds = (Button)this.activity.findViewById(R.id.face_bounds);
        }

        final FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        this.detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        this.lastAnalyzedTimestamp = 0;
        return;
    }

    private int degreesToFirebaseRotation(int degrees) {
        if (degrees == 0) {
            return FirebaseVisionImageMetadata.ROTATION_0;
        }
        else if (degrees == 90) {
            return FirebaseVisionImageMetadata.ROTATION_90;
        }
        else if (degrees == 180) {
            return FirebaseVisionImageMetadata.ROTATION_180;
        }
        else if (degrees == 270) {
            return FirebaseVisionImageMetadata.ROTATION_270;
        }
        return -1;
    }

    @Override
    public void analyze(ImageProxy imageProxy, int degrees) {
        if (imageProxy != null && imageProxy.getImage() != null) {

            final long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp - this.lastAnalyzedTimestamp >=
                    TimeUnit.SECONDS.toMillis(1)) {
                this.lastAnalyzedTimestamp = currentTimestamp;


                final Image mediaImage = imageProxy.getImage();
                final int rotation = this.degreesToFirebaseRotation(degrees);
                final FirebaseVisionImage image =
                        FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

                final Task<List<FirebaseVisionFace>> result =
                        detector.detectInImage(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                                            @Override
                                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                                for (FirebaseVisionFace face : faces) {
                                                    final Rect bounds = face.getBoundingBox();

                                                    Log.d(TAG, "L" + faceBounds);

                                                    if (faceBounds != null) {
                                                        final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)faceBounds.getLayoutParams();
                                                        layoutParams.leftMargin = bounds.left;
                                                        layoutParams.topMargin = bounds.top;
                                                        faceBounds.setLayoutParams(layoutParams);
                                                    }
                                                }
                                                return;
                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                return;
                                            }
                                        });
            }
        }
    }
}
