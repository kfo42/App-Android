package team.tangible.app.services;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
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
    float width = mDisplayMetrics.widthPixels;
    float height = mDisplayMetrics.heightPixels;
    private OnInteractionListener mOnInteractionListener;

    public enum Interaction {
        UNKNOWN("UNKNOWN"),

        /* FLING */
        FLING_UP("FLUP"),
        FLING_DOWN("FLDN"),
        FLING_LEFT("FLLT"),
        FLING_RIGHT("FLRT"),

        /* SINGLE TAP */
        SINGLE_BACK_RIGHT("BR"),
        SINGLE_TOP_RIGHT("TR"),
        SINGLE_FRONT_RIGHT("FR"),
        SINGLE_BACK_LEFT("BL"),
        SINGLE_TOP_LEFT("TL"),
        SINGLE_FRONT_LEFT("FL"),

        /* DOUBLE TAP */
        DOUBLE_BACK_RIGHT("DTBR"),
        DOUBLE_TOP_RIGHT("DTTR"),
        DOUBLE_FRONT_RIGHT("DTFR"),
        DOUBLE_BACK_LEFT("DTBL"),
        DOUBLE_TOP_LEFT("DTTL"),
        DOUBLE_FRONT_LEFT("DTFL"),

        /* LONG PRESS */
        LONG_BACK_RIGHT("LPBR"),
        LONG_TOP_RIGHT("LPTR"),
        LONG_FRONT_RIGHT("LPFR"),
        LONG_BACK_LEFT("LPBL"),
        LONG_TOP_LEFT("LPTL"),
        LONG_FRONT_LEFT("LPFL");

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
        mOnInteractionListener = onInteractionListener;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

        float deltaX = event2.getRawX() - event1.getRawX();
        float deltaY = event2.getRawY() - event1.getRawY();

        float normalizedDeltaX = deltaX / width;
        float normalizedDeltaY = deltaY / height;

        boolean isFlingVertical = Math.abs(normalizedDeltaY) > Math.abs(normalizedDeltaX);

        Interaction interaction;

        if (isFlingVertical) {
            interaction = normalizedDeltaY < 0 ? Interaction.FLING_UP : Interaction.FLING_DOWN;
        } else {
            interaction = normalizedDeltaX < 0 ? Interaction.FLING_LEFT : Interaction.FLING_RIGHT;
        }

        mOnInteractionListener.onInteraction(interaction);

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();
        Actuator actuator = getActuator(x, y);
        Interaction interaction;

        if (actuator.toString() == "back_right") {
            interaction = Interaction.SINGLE_BACK_RIGHT;
        } else if (actuator.toString() == "top_right") {
            interaction = Interaction.SINGLE_TOP_RIGHT;
        } else if (actuator.toString() == "front_right") {
            interaction = Interaction.SINGLE_FRONT_RIGHT;
        } else if (actuator.toString() == "back_left") {
            interaction = Interaction.SINGLE_BACK_LEFT;
        } else if (actuator.toString() == "top_left") {
            interaction = Interaction.SINGLE_TOP_LEFT;
        } else if (actuator.toString() == "front_left") {
            interaction = Interaction.SINGLE_FRONT_LEFT;
        } else{
            interaction = Interaction.UNKNOWN;
        }

        mOnInteractionListener.onInteraction(interaction);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.i("TAG", "onDoubleTap: ");
        float x = e.getX();
        float y = e.getY();
        Actuator actuator = getActuator(x, y);
        Interaction interaction;

        if (actuator.toString() == "back_right") {
            interaction = Interaction.DOUBLE_BACK_RIGHT;
        } else if (actuator.toString() == "top_right") {
            interaction = Interaction.DOUBLE_TOP_RIGHT;
        } else if (actuator.toString() == "front_right") {
            interaction = Interaction.DOUBLE_FRONT_RIGHT;
        } else if (actuator.toString() == "back_left") {
            interaction = Interaction.DOUBLE_BACK_LEFT;
        } else if (actuator.toString() == "top_left") {
            interaction = Interaction.DOUBLE_TOP_LEFT;
        } else if (actuator.toString() == "front_left") {
            interaction = Interaction.DOUBLE_FRONT_LEFT;
        } else{
            interaction = Interaction.UNKNOWN;
        }

        mOnInteractionListener.onInteraction(interaction);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.i("TAG", "onLongPress: ");
        float x = e.getX();
        float y = e.getY();
        Actuator actuator = getActuator(x, y);
        Interaction interaction;

        if (actuator.toString() == "back_right") {
            interaction = Interaction.LONG_BACK_RIGHT;
        } else if (actuator.toString() == "top_right") {
            interaction = Interaction.LONG_TOP_RIGHT;
        } else if (actuator.toString() == "front_right") {
            interaction = Interaction.LONG_FRONT_RIGHT;
        } else if (actuator.toString() == "back_left") {
            interaction = Interaction.LONG_BACK_LEFT;
        } else if (actuator.toString() == "top_left") {
            interaction = Interaction.LONG_TOP_LEFT;
        } else if (actuator.toString() == "front_left") {
            interaction = Interaction.LONG_FRONT_LEFT;
        } else{
            interaction = Interaction.UNKNOWN;
        }

        mOnInteractionListener.onInteraction(interaction);

        // growing heart animation at point of long press

        // add "bump" sensation
        // add "delta wave massage" sensation sequence
    }

    //Finds the correct actuator for single tap, double tap, and long press

    public Actuator getActuator(float x, float y){
        return Actuator.fromCoords(x, y, width, height);
    }

    public enum Actuator{
        front_left,
        front_right,
        top_left,
        top_right,
        back_left,
        back_right;

        public static Actuator fromCoords(float x, float y, float width, float height){
            if(inRange(x, 0, width/2)){
                if(inRange(y,  0, height/3)) return Actuator.back_left;
                if(inRange(y, height/3, 2*height/3)) return Actuator.top_left;
                else return Actuator.front_left;
            }else{
                if(inRange(y,  0, height/3)) return Actuator.back_right;
                if(inRange(y, height/3, 2*height/3)) return Actuator.top_right;
                else return Actuator.front_right;
            }
        }
        private static boolean inRange(float coord, float init, float end){
            return (coord >= init) && (coord < end);
        }
    }



}
