package io.github.cshadd.nodding_detection_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;
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

    public MainActivity() {
        super();
        this.cameraAllowed = false;
        return;
    }

    private Point getPointMidpoint(Point a, Point b) {
        final int x = (a.x + b.x)/2;
        final int y = (a.y + b.y)/2;
        return new Point(x, y);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        final Toolbar toolbar = findViewById(R.id.toolbar);
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
                            Log.i(TAG, "Needed permissions were granted!");
                            cameraAllowed = true;
                        }
                        else {
                            Log.w(TAG, "Needed permissions were not granted!");
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
            this.cameraControl.start();
        }

        Log.d(MainActivity.TAG, "I love nodding detection!");
        return;
    }

    private void vibrate(int milliseconds) {
        final Vibrator vibrator = (Vibrator)super.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(milliseconds);
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
    protected void onStop() {
        super.onStop();
        this.cameraControl.onStop();
        return;
    }
}