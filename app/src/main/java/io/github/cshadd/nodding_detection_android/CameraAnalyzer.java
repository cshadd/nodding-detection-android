package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.view.TextureView;

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
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraAnalyzer
        implements ImageAnalysis.Analyzer {
    private static final String TAG = "KAPLAN-2";

    private Activity activity;
    private List<RectOverlay> boundings;
    private TextureView cameraPreview;
    private FirebaseVisionFaceDetector detector;
    private GraphicOverlay graphicOverlay;
    private long lastAnalyzedTimestamp;

    private CameraAnalyzer() {
        this(null);
        return;
    }

    public CameraAnalyzer(Activity activity) {
        super();
        this.activity = activity;
        if (this.activity != null) {
            this.cameraPreview = (TextureView)this.activity.findViewById(R.id.camera_preview);
            this.graphicOverlay = (GraphicOverlay)this.activity.findViewById(R.id.graphic_overlay);
        }
        this.boundings = new LinkedList<>();
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

    public void onStop() {
        try {
            this.detector.close();
        }
        catch (IOException e) {
            Log.e(CameraAnalyzer.TAG, e.toString());
            e.printStackTrace();
        }
        catch (Exception e) {
            Log.e(CameraAnalyzer.TAG, e.toString());
            e.printStackTrace();
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

                /// aaaa
                /// https://proandroiddev.com/machine-learning-in-android-using-firebase-ml-kit-6e71a14e11f8
                ByteBuffer buffer = mediaImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                if (bitmapImage != null && cameraPreview != null) {
                    Log.d(TAG, "" + bitmapImage);
                    final FirebaseVisionImage image =
                            FirebaseVisionImage.fromBitmap(Bitmap.createScaledBitmap(bitmapImage,
                                    cameraPreview.getWidth(), cameraPreview.getHeight(), false));

                    final Task<List<FirebaseVisionFace>> result =
                            detector.detectInImage(image)
                                    .addOnSuccessListener(
                                            new OnSuccessListener<List<FirebaseVisionFace>>() {
                                                @Override
                                                public void onSuccess(List<FirebaseVisionFace> faces) {
                                                    for (RectOverlay rect : boundings) {
                                                        graphicOverlay.remove(rect);
                                                    }
                                                    boundings.clear();
                                                    for (FirebaseVisionFace face : faces) {
                                                        final Rect bounds = face.getBoundingBox();

                                                        final RectOverlay rectOverLay = new RectOverlay(graphicOverlay, bounds);
                                                        boundings.add(rectOverLay);
                                                        graphicOverlay.add(rectOverLay);
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

                /*final FirebaseVisionImage image =
                        FirebaseVisionImage
                                .fromMediaImage(mediaImage, rotation);

                final Task<List<FirebaseVisionFace>> result =
                        detector.detectInImage(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<FirebaseVisionFace>>() {
                                            @Override
                                            public void onSuccess(List<FirebaseVisionFace> faces) {
                                                for (RectOverlay rect : boundings) {
                                                    graphicOverlay.remove(rect);
                                                }
                                                boundings.clear();
                                                for (FirebaseVisionFace face : faces) {
                                                    final Rect bounds = face.getBoundingBox();

                                                    final RectOverlay rectOverLay = new RectOverlay(graphicOverlay, bounds);
                                                    boundings.add(rectOverLay);
                                                    graphicOverlay.add(rectOverLay);
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
                                        });*/
            }
        }
    }
}
