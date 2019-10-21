package io.github.cshadd.nodding_detection_android;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraX;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements LifecycleOwner {
    private static final String TAG = "NOGA";

    private boolean cameraAllowed;
    private CameraControl cameraControl;
    private CameraX.LensFacing currentCameraFacing;
    private boolean isCameraPreviewVisible;

    public MainActivity() {
        super();
        this.cameraAllowed = false;
        this.currentCameraFacing = CameraX.LensFacing.FRONT;
        this.isCameraPreviewVisible = false;
        return;
    }

    private void showError(String errorMessage) {
        final Resources res = getResources();
        final View contextView = (View)super.findViewById(R.id.main_view);
        final Snackbar snackBar = Snackbar.make(contextView,
                res.getString(R.string.error, errorMessage), Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction(R.string.dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();
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
    }

    private void vibrate(int milliseconds) {
        final Vibrator vibrator = (Vibrator)super.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
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
                        Manifest.permission.CAMERA
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
            try {
                this.cameraControl.onCreate();
            }
            catch (Exception e) {
                Log.e(MainActivity.TAG, e.toString());
                e.printStackTrace();
            }
        }

        final TextureView cameraPreviewView = (TextureView)super.findViewById(R.id.camera_preview_view);
        final LinearLayout detailsView = (LinearLayout) super.findViewById(R.id.detail_view);

        final FloatingActionButton fab = (FloatingActionButton)super.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCameraPreviewVisible) {
                    swapViewsAnimated(cameraPreviewView, detailsView);
                    isCameraPreviewVisible = false;
                }
                else {
                    swapViewsAnimated(detailsView, cameraPreviewView);
                    isCameraPreviewVisible = true;
                }
                vibrate(300);
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
        if (id == R.id.action_close) {
            this.vibrate(100);
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
                this.showError(e.getMessage());
            }
        }
        return;
    }
}