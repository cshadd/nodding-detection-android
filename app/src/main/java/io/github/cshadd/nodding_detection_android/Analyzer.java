package io.github.cshadd.nodding_detection_android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.media.Image;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Analyzer
        implements ImageAnalysis.Analyzer {
    private static final String TAG = "KAPLAN-2";
    private static final Float THRESHOLD = 50f;

    private Activity activity;
    private ImageView arrowBottom;
    private ImageView arrowLeft;
    private ImageView arrowRight;
    private ImageView arrowTop;
    private TextView[] capPos;
    private TextView[] capPosMid;
    private boolean currentPosCaptured;
    private CorrectedFirebaseVisionPointWrapper currentLeftEyePos;
    private CorrectedFirebaseVisionPointWrapper currentMidpoint;
    private CorrectedFirebaseVisionPointWrapper currentNoseBasePos;
    private CorrectedFirebaseVisionPointWrapper currentRightEyePos;
    private FirebaseVisionFaceDetector detector;
    private boolean faceDetected;
    private long lastAnalyzedTimestamp;
    private TextView[] pos;
    private TextView[] posMid;
    private Resources res;
    private ImageView smile;

    private Analyzer() {
        this(null);
        return;
    }

    public Analyzer(Activity activity) {
        super();
        this.activity = activity;
        this.currentPosCaptured = false;
        this.currentLeftEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentMidpoint = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentNoseBasePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentRightEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.faceDetected = false;
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

    private void clearCapturedPosition() {
        this.currentPosCaptured = false;
        this.currentLeftEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentMidpoint = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentNoseBasePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentRightEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);

        capPos[0].setText(res.getString(R.string.detail_position3,
                "L. Eye X",  "~0",
                "Nose X",  "~0",
                "R. Eye X",  "~0"
        ));
        capPos[1].setText(res.getString(R.string.detail_position3,
                "L. Eye Y",  "0",
                "Nose Y",  "~0",
                "R. Eye Y",  "~0"
        ));
        capPos[2].setText(res.getString(R.string.detail_position3,
                "L. Eye Z: ",  "~0",
                "Nose Z",  "~0",
                "R. Eye Z",  "~0"
        ));

        capPosMid[0].setText(res.getString(R.string.detail_position,
                "X",  "~0"
        ));
        capPosMid[1].setText(res.getString(R.string.detail_position,
                "Y",  "~0"
        ));
        capPosMid[2].setText(res.getString(R.string.detail_position,
                "Z",  "~0"
        ));
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

    private CorrectedFirebaseVisionPointWrapper firebaseVisionMidpoint(CorrectedFirebaseVisionPointWrapper p1,
                                                                       CorrectedFirebaseVisionPointWrapper p2) {
        float x = (p1.getX() + p2.getX()) / 2;
        float y = (p1.getY() + p2.getY()) / 2;
        float z = (p1.getZ() + p2.getZ()) / 2;
        return new CorrectedFirebaseVisionPointWrapper(x, y, z);
    }

    public void onCreate() throws Exception {
        this.res = this.activity.getResources();

        this.arrowBottom = (ImageView)this.activity.findViewById(R.id.arrow_bottom);
        this.arrowLeft = (ImageView)this.activity.findViewById(R.id.arrow_left);
        this.arrowRight = (ImageView)this.activity.findViewById(R.id.arrow_right);
        this.arrowTop = (ImageView)this.activity.findViewById(R.id.arrow_top);

        this.capPos = new TextView[3];
        this.capPos[0] = (TextView)this.activity.findViewById(R.id.cap_pos_x);
        this.capPos[1] = (TextView)this.activity.findViewById(R.id.cap_pos_y);
        this.capPos[2] = (TextView)this.activity.findViewById(R.id.cap_pos_z);

        this.capPosMid = new TextView[3];
        this.capPosMid[0] = (TextView)this.activity.findViewById(R.id.cap_pos_mid_x);
        this.capPosMid[1] = (TextView)this.activity.findViewById(R.id.cap_pos_mid_y);
        this.capPosMid[2] = (TextView)this.activity.findViewById(R.id.cap_pos_mid_z);

        final Button clearCaptured = (Button)this.activity.findViewById(R.id.clear_cap);
        clearCaptured.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearCapturedPosition();
                vibrate(300);
                return;
            }
        });

        this.pos = new TextView[3];
        this.pos[0] = (TextView)this.activity.findViewById(R.id.pos_x);
        this.pos[1] = (TextView)this.activity.findViewById(R.id.pos_y);
        this.pos[2] = (TextView)this.activity.findViewById(R.id.pos_z);

        pos[0].setText(res.getString(R.string.detail_position3,
                "L. Eye X",  "~0",
                "Nose X",  "~0",
                "R. Eye X",  "~0"
        ));
        pos[1].setText(res.getString(R.string.detail_position3,
                "L. Eye Y",  "0",
                "Nose Y",  "~0",
                "R. Eye Y",  "~0"
        ));
        pos[2].setText(res.getString(R.string.detail_position3,
                "L. Eye Z: ",  "~0",
                "Nose Z",  "~0",
                "R. Eye Z",  "~0"
        ));

        this.posMid = new TextView[3];
        this.posMid[0] = (TextView)this.activity.findViewById(R.id.pos_mid_x);
        this.posMid[1] = (TextView)this.activity.findViewById(R.id.pos_mid_y);
        this.posMid[2] = (TextView)this.activity.findViewById(R.id.pos_mid_z);

        posMid[0].setText(res.getString(R.string.detail_position,
                "X",  "~0"
        ));
        posMid[1].setText(res.getString(R.string.detail_position,
                "Y",  "~0"
        ));
        posMid[2].setText(res.getString(R.string.detail_position,
                "Z",  "~0"
        ));

        this.smile = (ImageView)this.activity.findViewById(R.id.smile);
        return;
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
                                                // Log.i(Analyzer.TAG, "Faces: " + faces.size());
                                                if (faces.size() > 0) {
                                                    smile.setImageResource(R.drawable.smile_green);
                                                    if (!faceDetected) {
                                                        Toast.makeText(activity, R.string.face_detected, Toast.LENGTH_SHORT)
                                                                .show();
                                                        vibrate(500);
                                                        faceDetected = true;
                                                    }
                                                    final FirebaseVisionFace face = faces.get(0);
                                                    // final Rect bounds = face.getBoundingBox();

                                                    final FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                                                    final FirebaseVisionFaceLandmark noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                                                    final FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);

                                                    final CorrectedFirebaseVisionPointWrapper leftEyePos =
                                                            new CorrectedFirebaseVisionPointWrapper(leftEye.getPosition());
                                                    final CorrectedFirebaseVisionPointWrapper noseBasePos =
                                                            new CorrectedFirebaseVisionPointWrapper(noseBase.getPosition());
                                                    final CorrectedFirebaseVisionPointWrapper rightEyePos =
                                                            new CorrectedFirebaseVisionPointWrapper(rightEye.getPosition());

                                                    final CorrectedFirebaseVisionPointWrapper eyesMidpoint = firebaseVisionMidpoint(leftEyePos, rightEyePos);
                                                    final CorrectedFirebaseVisionPointWrapper eyesNoseMidpoint = firebaseVisionMidpoint(eyesMidpoint, noseBasePos);

                                                    pos[0].setText(res.getString(R.string.detail_position3,
                                                            "L. Eye X: ",  "~" + Math.floor(leftEyePos.getX()),
                                                            "Nose X: ",  "~" + Math.floor(noseBasePos.getX()),
                                                            "R. Eye X: ",  "~" + Math.floor(rightEyePos.getX())
                                                    ));
                                                    pos[1].setText(res.getString(R.string.detail_position3,
                                                            "L. Eye Y",  "~" + Math.floor(leftEyePos.getY()),
                                                            "Nose Y",  "~" + Math.floor(noseBasePos.getY()),
                                                            "R. Eye Y",  "~" + Math.floor(rightEyePos.getY())
                                                    ));
                                                    pos[2].setText(res.getString(R.string.detail_position3,
                                                            "L. Eye Z",  "~" + Math.floor(leftEyePos.getZ()),
                                                            "Nose Z",  "~" + Math.floor(noseBasePos.getZ()),
                                                            "R. Eye Z",  "~" + Math.floor(rightEyePos.getZ())
                                                    ));

                                                    posMid[0].setText(res.getString(R.string.detail_position,
                                                            "X",  "~" + Math.floor(eyesNoseMidpoint.getX())
                                                    ));
                                                    posMid[1].setText(res.getString(R.string.detail_position,
                                                            "Y",  "~" + Math.floor(eyesNoseMidpoint.getY())
                                                    ));
                                                    posMid[2].setText(res.getString(R.string.detail_position,
                                                            "Z",  "~" + Math.floor(eyesNoseMidpoint.getZ())
                                                    ));

                                                    if (!currentPosCaptured) {
                                                        currentLeftEyePos = leftEyePos;
                                                        currentNoseBasePos = noseBasePos;
                                                        currentRightEyePos = rightEyePos;
                                                        currentMidpoint = eyesNoseMidpoint;

                                                        capPos[0].setText(res.getString(R.string.detail_position3,
                                                                "L. Eye X",  "~" + Math.floor(currentLeftEyePos.getX()),
                                                                "Nose X",  "~" + Math.floor(currentNoseBasePos.getX()),
                                                                "R. Eye X",  "~" + Math.floor(currentRightEyePos.getX())
                                                        ));
                                                        capPos[1].setText(res.getString(R.string.detail_position3,
                                                                "L. Eye Y",  "~" + Math.floor(currentLeftEyePos.getY()),
                                                                "Nose Y",  "~" + Math.floor(currentNoseBasePos.getY()),
                                                                "R. Eye Y",  "~" + Math.floor(currentRightEyePos.getY())
                                                        ));
                                                        capPos[2].setText(res.getString(R.string.detail_position3,
                                                                "L. Eye Z: ",  "~" + Math.floor(currentLeftEyePos.getZ()),
                                                                "Nose Z",  "~" + Math.floor(currentNoseBasePos.getZ()),
                                                                "R. Eye Z",  "~" + Math.floor(currentRightEyePos.getZ())
                                                        ));

                                                        capPosMid[0].setText(res.getString(R.string.detail_position,
                                                                "X",  "~" + Math.floor(currentMidpoint.getX())
                                                        ));
                                                        capPosMid[1].setText(res.getString(R.string.detail_position,
                                                                "Y",  "~" + Math.floor(currentMidpoint.getY())
                                                        ));
                                                        capPosMid[2].setText(res.getString(R.string.detail_position,
                                                                "Z",  "~" + Math.floor(currentMidpoint.getZ())
                                                        ));

                                                        currentPosCaptured = true;
                                                    }

                                                    if (currentMidpoint.getX() - eyesNoseMidpoint.getX() < -THRESHOLD) {
                                                        arrowLeft.setImageResource(R.drawable.arrow_green);
                                                        arrowRight.setImageResource(R.drawable.arrow);
                                                    }
                                                    else if (currentMidpoint.getX() - eyesNoseMidpoint.getX() > THRESHOLD) {
                                                        arrowLeft.setImageResource(R.drawable.arrow);
                                                        arrowRight.setImageResource(R.drawable.arrow_green);
                                                    }
                                                    else {
                                                        arrowLeft.setImageResource(R.drawable.arrow);
                                                        arrowRight.setImageResource(R.drawable.arrow);
                                                    }

                                                    if (currentMidpoint.getY() - eyesNoseMidpoint.getY() < -THRESHOLD) {
                                                        arrowBottom.setImageResource(R.drawable.arrow_green);
                                                        arrowTop.setImageResource(R.drawable.arrow);
                                                    }
                                                    else if (currentMidpoint.getY() - eyesNoseMidpoint.getY() > THRESHOLD) {
                                                        arrowBottom.setImageResource(R.drawable.arrow);
                                                        arrowTop.setImageResource(R.drawable.arrow_green);
                                                    }
                                                    else {
                                                        arrowBottom.setImageResource(R.drawable.arrow);
                                                        arrowTop.setImageResource(R.drawable.arrow);
                                                    }

                                                    /*Log.i(Analyzer.TAG, "Results: "
                                                            + "{Left Eye: " + leftEyePos
                                                            + ", Nose Base: " + noseBasePos
                                                            + ", Right Eye: " + rightEyePos + "}");
                                                    Log.i(Analyzer.TAG, "Results (Midpoints): "
                                                            + "{Eyes: " + eyesMidpoint
                                                            + ", Eyes-Nose: " + eyesNoseMidpoint + "}");*/
                                                }
                                                else {
                                                    Toast.makeText(activity, R.string.ask_look, Toast.LENGTH_SHORT)
                                                            .show();
                                                    clearCapturedPosition();
                                                    arrowBottom.setImageResource(R.drawable.arrow);
                                                    arrowLeft.setImageResource(R.drawable.arrow);
                                                    arrowRight.setImageResource(R.drawable.arrow);
                                                    arrowTop.setImageResource(R.drawable.arrow);
                                                    smile.setImageResource(R.drawable.smile);
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
