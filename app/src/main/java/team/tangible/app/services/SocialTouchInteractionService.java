package team.tangible.app.services;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.util.Objects;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class SocialTouchInteractionService extends GestureDetector.SimpleOnGestureListener {
    private DisplayMetrics mDisplayMetrics;

    private OnInteractionListener mOnInteractionListender;

    public enum Interaction {
        UNKNOWN("UNKNOWN"),

        /* FLING */
        FLING_UP("FLUP"),
        FLING_DOWN("FLDN"),
        FLING_LEFT("FLLT"),
        FLING_RIGHT("FLRT");

        private final String mBleCode;

        Interaction(String bleCode) {
            this.mBleCode = bleCode;
        }

        public String getBleCode() {
            return mBleCode;
        }
    }

    public interface OnInteractionListener {
        void onInteraction(Interaction interaction);
    }

    public SocialTouchInteractionService(Context context) {
        mDisplayMetrics = new DisplayMetrics();

        ((WindowManager) Objects.requireNonNull(context.getSystemService(Context.WINDOW_SERVICE)))
                .getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    public void setOnInteractionListener(OnInteractionListener onInteractionListener) {
        mOnInteractionListender = onInteractionListener;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

        float deltaX = event2.getRawX() - event1.getRawX();
        float deltaY = event2.getRawY() - event1.getRawY();

        float normalizedDeltaX = deltaX / mDisplayMetrics.widthPixels;
        float normalizedDeltaY = deltaY / mDisplayMetrics.heightPixels;

        boolean isFlingVertical = Math.abs(normalizedDeltaY) > Math.abs(normalizedDeltaX);

        Interaction interaction;

        if (isFlingVertical) {
            interaction = normalizedDeltaY < 0 ? Interaction.FLING_UP : Interaction.FLING_DOWN;
        } else {
            interaction = normalizedDeltaX < 0 ? Interaction.FLING_LEFT : Interaction.FLING_RIGHT;
        }

        mOnInteractionListender.onInteraction(interaction);

        return true;
    }
}
