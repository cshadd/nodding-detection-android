package io.github.cshadd.nodding_detection_android;

import android.content.res.Resources;
import android.graphics.Rect;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.controls.Facing;
import java.io.IOException;
import java.util.List;

public class Analyzer {
    private static final String TAG = "KAPLAN-2";
    private static final Float THRESHOLD = 50f;
    private static final int TIMEOUT_DETECTION = 5;
    private static final int TIMEOUT_RESET = 3;

    private CommonActivity activity;
    private ImageView arrowBottom;
    private ImageView arrowLeft;
    private ImageView arrowRight;
    private ImageView arrowTop;
    private TextView[] capPos;
    private TextView[] capPosMid;
    private boolean currentPosCaptured;
    private CorrectedFirebaseVisionPointWrapper currentLeftEyePos;
    private Facing currentLensFacing;
    private CorrectedFirebaseVisionPointWrapper currentMidpoint;
    private CorrectedFirebaseVisionPointWrapper currentNoseBasePos;
    private CorrectedFirebaseVisionPointWrapper currentRightEyePos;
    private FirebaseVisionFaceDetector detector;
    private boolean faceDetected;
    private TextView[] pos;
    private TextView[] posMid;
    private long recordedNodStart;
    private long recordedShakeStart;
    private Resources res;
    private Task<List<FirebaseVisionFace>> result;
    private ImageView smile;
    private TextView status;
    private boolean waitingForFace;

    private Analyzer() {
        this(null);
        return;
    }

    public Analyzer(CommonActivity activity) {
        this(activity, Facing.FRONT);
        return;
    }

    public Analyzer(CommonActivity activity, Facing lensFacing) {
        super();
        this.activity = activity;
        this.currentLeftEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentLensFacing = lensFacing;
        this.currentMidpoint = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentNoseBasePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentRightEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        final FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .enableTracking()
                        .build();

        this.detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        this.result = null;
        return;
    }

    public void analyze(FirebaseVisionImage image) {
        this.result = detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {
                        // Log.i(Analyzer.TAG, "Faces: " + faces.size());
                        if (waitingForFace) {
                            Toast.makeText(activity,
                                    res.getString(R.string.ask_look, "" + currentLensFacing),
                                    Toast.LENGTH_SHORT).show();
                            clearPosition();
                            waitingForFace = false;
                        }
                        else {
                            if (faces.size() > 0) {
                                if (faceDetected) {
                                    smile.setImageResource(R.drawable.smile_green);
                                    final FirebaseVisionFace face = faces.get(0);
                                    final Rect bounds = face.getBoundingBox();
                                    // Log.i(Analyzer.TAG, "Bounds: " + bounds);

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

                                    // Left
                                    if (currentMidpoint.getX() - eyesNoseMidpoint.getX() < -Analyzer.THRESHOLD) {
                                        arrowLeft.setImageResource(R.drawable.arrow_green);
                                        arrowRight.setImageResource(R.drawable.arrow);
                                        recordedShakeStart = System.currentTimeMillis();
                                    }
                                    // Right
                                    else if (currentMidpoint.getX() - eyesNoseMidpoint.getX() > Analyzer.THRESHOLD) {
                                        arrowLeft.setImageResource(R.drawable.arrow);
                                        arrowRight.setImageResource(R.drawable.arrow_green);

                                        if (recordedShakeStart != 0) {
                                            final long recordedShakeEnd = System.currentTimeMillis();
                                            final float durationDetection = (recordedShakeEnd - recordedShakeStart) / 1000f;

                                            if (durationDetection <= Analyzer.TIMEOUT_DETECTION) {
                                                status.setText(res.getString(R.string.status, "shaking", "no"));
                                            }
                                        }
                                    }
                                    else {
                                        arrowLeft.setImageResource(R.drawable.arrow);
                                        arrowRight.setImageResource(R.drawable.arrow);
                                    }

                                    // Bottom
                                    if (currentMidpoint.getY() - eyesNoseMidpoint.getY() < -THRESHOLD) {
                                        arrowBottom.setImageResource(R.drawable.arrow_green);
                                        arrowTop.setImageResource(R.drawable.arrow);

                                        if (recordedNodStart != 0) {
                                            final long recordedNodEnd = System.currentTimeMillis();
                                            final float durationDetection = (recordedNodEnd - recordedNodStart) / 1000f;

                                            if (durationDetection <= Analyzer.TIMEOUT_DETECTION) {
                                                status.setText(res.getString(R.string.status, "nodding", "yes"));
                                            }
                                        }
                                    }
                                    // Top
                                    else if (currentMidpoint.getY() - eyesNoseMidpoint.getY() > THRESHOLD) {
                                        arrowBottom.setImageResource(R.drawable.arrow);
                                        arrowTop.setImageResource(R.drawable.arrow_green);
                                        recordedNodStart = System.currentTimeMillis();
                                    }
                                    else {
                                        arrowBottom.setImageResource(R.drawable.arrow);
                                        arrowTop.setImageResource(R.drawable.arrow);
                                    }

                                    /* Log.i(Analyzer.TAG, "Results: "
                                            + "{Left Eye: " + leftEyePos
                                            + ", Nose Base: " + noseBasePos
                                            + ", Right Eye: " + rightEyePos + "}");
                                    Log.i(Analyzer.TAG, "Results (Midpoints): "
                                            + "{Eyes: " + eyesMidpoint
                                            + ", Eyes-Nose: " + eyesNoseMidpoint + "}"); */
                                }
                                else {
                                    Toast.makeText(activity, R.string.face_detected, Toast.LENGTH_SHORT)
                                            .show();
                                    activity.vibrate(500);
                                    clearPosition();
                                    faceDetected = true;
                                }
                            }
                            else {
                                waitingForFace = true;
                            }
                        }
                        return;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        activity.showError(e.getMessage());
                        return;
                    }
                });
        return;
    }

    public void clear() {
        this.clearPosition();
        this.faceDetected = false;
        this.waitingForFace = true;
        return;
    }

    private void clearPosition() {
        this.arrowBottom.setImageResource(R.drawable.arrow);
        this.arrowLeft.setImageResource(R.drawable.arrow);
        this.arrowRight.setImageResource(R.drawable.arrow);
        this.arrowTop.setImageResource(R.drawable.arrow);
        this.currentPosCaptured = false;
        this.currentLeftEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentMidpoint = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentNoseBasePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);
        this.currentRightEyePos = new CorrectedFirebaseVisionPointWrapper(0f, 0f, 0f);

        this.capPos[0].setText(res.getString(R.string.detail_position3,
                "L. Eye X",  "~0",
                "Nose X",  "~0",
                "R. Eye X",  "~0"
        ));
        this.capPos[1].setText(res.getString(R.string.detail_position3,
                "L. Eye Y",  "0",
                "Nose Y",  "~0",
                "R. Eye Y",  "~0"
        ));
        this.capPos[2].setText(res.getString(R.string.detail_position3,
                "L. Eye Z: ",  "~0",
                "Nose Z",  "~0",
                "R. Eye Z",  "~0"
        ));

        this.capPosMid[0].setText(res.getString(R.string.detail_position,
                "X",  "~0"
        ));
        this.capPosMid[1].setText(res.getString(R.string.detail_position,
                "Y",  "~0"
        ));
        this.capPosMid[2].setText(res.getString(R.string.detail_position,
                "Z",  "~0"
        ));

        this.faceDetected = false;

        this.pos[0].setText(res.getString(R.string.detail_position3,
                "L. Eye X",  "~0",
                "Nose X",  "~0",
                "R. Eye X",  "~0"
        ));
        this.pos[1].setText(res.getString(R.string.detail_position3,
                "L. Eye Y",  "0",
                "Nose Y",  "~0",
                "R. Eye Y",  "~0"
        ));
        this.pos[2].setText(res.getString(R.string.detail_position3,
                "L. Eye Z: ",  "~0",
                "Nose Z",  "~0",
                "R. Eye Z",  "~0"
        ));

        this.posMid[0].setText(res.getString(R.string.detail_position,
                "X",  "~0"
        ));
        this.posMid[1].setText(res.getString(R.string.detail_position,
                "Y",  "~0"
        ));
        this.posMid[2].setText(res.getString(R.string.detail_position,
                "Z",  "~0"
        ));

        this.recordedNodStart = 0;
        this.recordedShakeStart = 0;
        this.smile.setImageResource(R.drawable.smile);
        this.status.setText(R.string.empty);
        return;
    }

    private CorrectedFirebaseVisionPointWrapper firebaseVisionMidpoint(CorrectedFirebaseVisionPointWrapper p1,
                                                                       CorrectedFirebaseVisionPointWrapper p2) {
        float x = (p1.getX() + p2.getX()) / 2;
        float y = (p1.getY() + p2.getY()) / 2;
        float z = (p1.getZ() + p2.getZ()) / 2;
        return new CorrectedFirebaseVisionPointWrapper(x, y, z);
    }

    public void onCreate() {
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

        this.pos = new TextView[3];
        this.pos[0] = (TextView)this.activity.findViewById(R.id.pos_x);
        this.pos[1] = (TextView)this.activity.findViewById(R.id.pos_y);
        this.pos[2] = (TextView)this.activity.findViewById(R.id.pos_z);

        this.posMid = new TextView[3];
        this.posMid[0] = (TextView)this.activity.findViewById(R.id.pos_mid_x);
        this.posMid[1] = (TextView)this.activity.findViewById(R.id.pos_mid_y);
        this.posMid[2] = (TextView)this.activity.findViewById(R.id.pos_mid_z);

        this.smile = (ImageView)this.activity.findViewById(R.id.smile);
        this.status = (TextView)this.activity.findViewById(R.id.status);

        this.clear();
        return;
    }

    public void onDestroy() throws IOException {
        this.detector.close();
        return;
    }

    public void swapLens() {
        if (this.currentLensFacing == Facing.BACK) {
            this.currentLensFacing = Facing.FRONT;
        }
        else {
            this.currentLensFacing = Facing.BACK;
        }
        this.clear();
        return;
    }
}
