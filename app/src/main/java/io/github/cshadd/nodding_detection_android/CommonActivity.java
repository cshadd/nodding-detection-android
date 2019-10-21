package io.github.cshadd.nodding_detection_android;

import android.content.Context;
import android.content.res.Resources;
import android.os.Vibrator;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public abstract class CommonActivity
        extends AppCompatActivity {

    public CommonActivity() {
        super();
    }

    public void showError(String errorMessage) {
        final Resources res = super.getResources();
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

    public void vibrate(int milliseconds) {
        final Vibrator vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(milliseconds);
        }
        return;
    }
}
