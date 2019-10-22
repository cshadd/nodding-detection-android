package io.github.cshadd.nodding_detection_android;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;
import java.io.IOException;

public class CameraControl {
    private static final String TAG = "KAPLAN-1";

    private CommonActivity activity;
    private Analyzer analyzer;
    private CameraView cameraPreviewView;
    private Facing currentLensFacing;
    private FrameProcessor frameProcessor;

    private CameraControl() {
        this(null);
        return;
    }

    public CameraControl(CommonActivity activity) {
        super();
        this.activity = activity;
        this.currentLensFacing = Facing.FRONT;
        return;
    }

    public void clear() {
        this.analyzer.clear();
        return;
    }

    private FirebaseVisionImageMetadata extractFrameMetadata(Frame frame) {
        return new FirebaseVisionImageMetadata.Builder()
                .setWidth(frame.getSize().getWidth())
                .setHeight(frame.getSize().getHeight())
                .setFormat(frame.getFormat())
                .setRotation(frame.getRotation() / 90)
                .build();
    }

    public void onCreate() {
        this.analyzer = new Analyzer(activity);
        this.analyzer.onCreate();
        this.cameraPreviewView = (CameraView)this.activity.findViewById(R.id.camera_preview_view);
        this.setupCamera(this.currentLensFacing);
        return;
    }

    public void onDestroy() throws IOException {
        this.analyzer.onDestroy();
        this.cameraPreviewView.removeFrameProcessor(this.frameProcessor);
        return;
    }

    private void setupCamera(Facing lensFacing) {
        this.cameraPreviewView.removeFrameProcessor(this.frameProcessor);
        this.cameraPreviewView.setFacing(lensFacing);
        this.cameraPreviewView.setLifecycleOwner(this.activity);

        this.frameProcessor = new FrameProcessor() {
            @Override
            public void process(Frame frame) {
                final byte[] data = frame.getData();
                final int format = frame.getFormat();
                final int rotation = frame.getRotation();
                final Size size = frame.getSize();
                final long time = frame.getTime();

                final FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(data, extractFrameMetadata(frame));
                analyzer.analyze(image);
                return;
            }
        };

        this.cameraPreviewView.addFrameProcessor(this.frameProcessor);
    }

    public void swapLens() {
        if (this.currentLensFacing == Facing.BACK) {
            this.currentLensFacing = Facing.FRONT;
        }
        else {
            this.currentLensFacing = Facing.BACK;
        }
        this.setupCamera(this.currentLensFacing);
        this.analyzer.swapLens();
        return;
    }
}
