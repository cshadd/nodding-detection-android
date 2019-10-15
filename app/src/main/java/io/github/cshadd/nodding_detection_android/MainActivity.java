package io.github.cshadd.nodding_detection_android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    public MainActivity() {
        super();
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

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                vibrate(500);
            }
        });
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

    private void vibrate(int milliseconds) {
        final Vibrator vibrator = (Vibrator)super.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(milliseconds);
        return;
    }
}