package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.media.Image;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Analyzer
        implements ImageAnalysis.Analyzer {
    private static final String TAG = "KAPLAN-2";

    private Activity activity;
    private FirebaseVisionFaceDetector detector;
    private boolean faceDetected;
    private ImageView faceDetection;
    private long lastAnalyzedTimestamp;

    private Analyzer() {
        this(null);
        return;
    }

    public Analyzer(Activity activity) {
        super();
        this.activity = activity;
        this.faceDetected = false;
        this.faceDetection = (ImageView)this.activity.findViewById(R.id.face_detection);
        final FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
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

    public void onDestroy() throws IOException {
        this.detector.close();
        return;
    }

    private void vibrate(int milliseconds) {
        final Vibrator vibrator = (Vibrator)this.activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
        return;
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
                        FirebaseVisionImage
                                .fromMediaImage(mediaImage, rotation);
                mediaImage.close();
                final Task<List<FirebaseVisionFace>> result =
                        detector.detectInImage(image)
                                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                                            @Override
                                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                                // Log.i(CameraAnalyzer.TAG, "Faces: " + faces.size());
                                                if (faces.size() > 0) {
                                                    faceDetection.setImageResource(R.drawable.smile_green);
                                                    if (!faceDetected) {
                                                        vibrate(500);
                                                        faceDetected = true;
                                                        Toast.makeText(activity, R.string.ask_move, Toast.LENGTH_SHORT)
                                                                .show();
                                                    }
                                                    final FirebaseVisionFace face = faces.get(0);
                                                    final Rect bounds = face.getBoundingBox();
                                                }
                                                else {
                                                    faceDetection.setImageResource(R.drawable.smile);
                                                    Toast.makeText(activity, R.string.ask_look, Toast.LENGTH_SHORT)
                                                            .show();
                                                    faceDetected = false;
                                                }
                                                return;
                                            }
                                        })
                                .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                return;
                                            }
                                        });
            }
            imageProxy.close();
        }
    }
}
