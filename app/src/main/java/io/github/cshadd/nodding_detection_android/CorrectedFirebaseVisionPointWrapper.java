package io.github.cshadd.nodding_detection_android;

import com.google.android.gms.common.internal.Objects;
import com.google.android.gms.internal.firebase_ml.zzlq;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;

public final class CorrectedFirebaseVisionPointWrapper {
    private FirebaseVisionPoint point;

    public CorrectedFirebaseVisionPointWrapper() {
        this(0f, 0f, 0f);
        return;
    }

    public CorrectedFirebaseVisionPointWrapper(float x, float y, float z) {
        this(new FirebaseVisionPoint(x, y, z));
        return;
    }

    public CorrectedFirebaseVisionPointWrapper(FirebaseVisionPoint point) {
        super();
        this.point = point;
        return;
    }

    public final Float getX() {

        return this.point.getX();
    }
    public final Float getY()
    {
        return this.point.getY();
    }
    public final Float getZ()
    {
        // We do not want anything null, thus this wrapper was created.
        return 0f;
    }

    @Override
    public final boolean equals(Object var1) {
        if (var1 == this) {
            return true;
        }
        else if (!(var1 instanceof CorrectedFirebaseVisionPointWrapper)) {
            return false;
        }
        final CorrectedFirebaseVisionPointWrapper var2 = (CorrectedFirebaseVisionPointWrapper)var1;
        return Objects.equal(this.getX(), var2.getX())
                && Objects.equal(this.getY(), var2.getY())
                && Objects.equal(this.getZ(), var2.getZ());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(new Object[]{
                this.getX(),
                this.getY(),
                this.getZ(),
        });
    }

    @Override
    public final String toString() {
        return zzlq.zzay("FirebaseVisionPoint (Corrected)")
                .zzh("x", this.getX())
                .zzh("y", this.getY())
                .zzh("z", this.getZ()).toString();
    }
}
