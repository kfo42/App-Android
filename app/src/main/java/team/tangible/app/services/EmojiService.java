package team.tangible.app.services;
import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Objects;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static team.tangible.app.utils.FloatUtils.inRange;

    public class EmojiService extends GestureDetector.SimpleOnGestureListener {
        private DisplayMetrics mDisplayMetrics;
        private OnClickListener mOnClickListener;

        public enum Interaction {
            UNKNOWN("UNKNOWN"),

            /* EMOJI CODES */
            HEART_EYES("LEHE"),
            HEART("LEHT"),
            KISS("LEKI"),
            HAND_WAVE("LEHW"),
            GIGGLE("LEGI"),
            SAD("LESA"),
            ANGRY("LEAN"),
            CRAZY_FACE("LECF"),
            CONFETTI("LECO"),
            STAR("LEST");


            private final String mBleCode;

            Interaction(String bleCode) {
                this.mBleCode = bleCode;
            }

            public String getBleCode() {
                return mBleCode;
            }
        }

        public interface OnClickListener {
            void onClick(team.tangible.app.services.EmojiService.Interaction buttonClick);
        }

        public EmojiService(Context context) {
            mDisplayMetrics = new DisplayMetrics();
            ((WindowManager) Objects.requireNonNull(context.getSystemService(Context.WINDOW_SERVICE)))
                    .getDefaultDisplay().getMetrics(mDisplayMetrics);
        }

        public void setOnClickListener(team.tangible.app.services.EmojiService.OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }

    }

