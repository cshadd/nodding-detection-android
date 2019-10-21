package io.github.cshadd.nodding_detection_android;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraView;
import java.util.List;

public class MainActivity
        extends CommonActivity {
    private static final String TAG = "NOGA";

    private boolean cameraAllowed;
    private CameraControl cameraControl;
    private int cameraPreviewVisible;

    public MainActivity() {
        super();
        this.cameraAllowed = false;
        this.cameraPreviewVisible = -1;
        return;
    }

    private void swapViewsAnimated(final View v1, final View v2) {
        v1.animate().alpha(0.0f).setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        v1.clearAnimation();
                        v2.setVisibility(View.INVISIBLE);
                        v1.setVisibility(View.GONE);
                        v2.animate().alpha(1.0f).setDuration(500)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        v2.clearAnimation();
                                        v2.setVisibility(View.VISIBLE);
                                        return;
                                    }
                        });
                        return;
                    }
                });
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        final Toolbar toolbar = super.findViewById(R.id.toolbar);
        super.setSupportActionBar(toolbar);

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                )
                .withListener(new MultiplePermissionsListener() {
                    private static final String TAG = "WIEGLY";

                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Log.i(MainActivity.TAG, "Needed permissions were granted!");
                            cameraAllowed = true;
                        }
                        else {
                            Log.w(MainActivity.TAG, "Needed permissions were not granted!");
                            cameraAllowed = false;
                        }
                        return;
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        return;
                    }
                }).check();

        if (this.cameraAllowed) {
            this.cameraControl = new CameraControl(this);
            this.cameraControl.onCreate();
        }

        final CameraView cameraPreviewView = (CameraView)super.findViewById(R.id.camera_preview_view);
        this.cameraPreviewVisible = cameraPreviewView.getVisibility();
        final ConstraintLayout detailsView = (ConstraintLayout)super.findViewById(R.id.detail_view);

        final FloatingActionButton fabLeft = (FloatingActionButton)super.findViewById(R.id.fab_left);
        fabLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraPreviewVisible == View.VISIBLE) {
                    swapViewsAnimated(cameraPreviewView, detailsView);
                    cameraPreviewVisible = View.GONE;
                }
                else {
                    swapViewsAnimated(detailsView, cameraPreviewView);
                    cameraPreviewVisible = View.VISIBLE;
                }
                vibrate(300);
                return;
            }
        });

        final FloatingActionButton fabRight = (FloatingActionButton)super.findViewById(R.id.fab_right);
        fabRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraControl.swapLens();
                vibrate(300);
                return;
            }
        });

        Log.d(MainActivity.TAG, "I love nodding detection!");
        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_clear) {
            if (this.cameraControl != null) {
                this.cameraControl.clearPosition();
            }
            super.vibrate(300);
            return true;
        }
        else if (id == R.id.action_clear_cap) {
            if (this.cameraControl != null) {
                this.cameraControl.clearCapturedPosition();
            }
            super.vibrate(300);
            return true;
        }
        else if (id == R.id.action_clear_status) {
            if (this.cameraControl != null) {
                this.cameraControl.clearStatus();
            }
            super.vibrate(300);
            return true;
        }
        else if (id == R.id.action_close) {
            super.vibrate(100);
            final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory( Intent.CATEGORY_HOME );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            super.startActivity(homeIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.cameraControl != null) {
            try {
                this.cameraControl.onDestroy();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, e.toString());
                e.printStackTrace();
                super.showError(e.getMessage());
            }
        }
        return;
    }
}