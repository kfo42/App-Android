package team.tangible.app.activities;

import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amrdeveloper.reactbutton.ReactButton;
import com.amrdeveloper.reactbutton.Reaction;

import com.facebook.react.common.ReactConstants;
import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.disposables.CompositeDisposable;
import team.tangible.app.Constants;
import team.tangible.app.R;
import team.tangible.app.TangibleApplication;
import team.tangible.app.services.SocialTouchInteractionService;
import team.tangible.app.services.EmojiService;
import team.tangible.app.services.TangibleBleConnectionService;
import team.tangible.app.services.TangibleDataService;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.URLUtils;
import timber.log.Timber;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetView;

import javax.inject.Inject;
import javax.inject.Named;

import static team.tangible.app.Constants.BluetoothLowEnergy.NordicUARTService.*;


public class HomescreenActivity extends JitsiMeetActivity implements View.OnTouchListener, SocialTouchInteractionService.OnInteractionListener {

    private JitsiMeetView mJitsiMeetView;
    private static final int JITSI_CONTROLS_HEIGHT_PX = 600;
    private FrameLayout mFrameLayout;
    private GestureOverlayView mGestureOverlayView;
    private GestureDetectorCompat mDetector;

    CompositeDisposable mDisposables;
    MutableLiveData<RxBleConnection> mRxBleConnectionLiveData = new MutableLiveData<>();

    @Inject
    SocialTouchInteractionService mSocialTouchInteractionService;

    @Inject
    EmojiService mEmojiService;

    @Inject
    TangibleBleConnectionService mTangibleBleConnectionService;

    @Inject
    TangibleDataService mTangibleDataService;

    @Inject
    @Named(Constants.Threading.MAIN_THREAD)
    Handler mMainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
        
        RelativeLayout relativeLayout = findViewById(R.id.activity_homescreen);
        Context context = relativeLayout.getContext();

        ((TangibleApplication) getApplication()).getApplicationComponent().inject(this);

        relativeLayout.addView(mFrameLayout = new FrameLayout(context) {{
            setId(View.generateViewId());
            addView(mJitsiMeetView = new JitsiMeetView(context) {{
                setId(View.generateViewId());
            }});
            addView(mGestureOverlayView = new GestureOverlayView(context) {{
                setId(View.generateViewId());
                setGestureStrokeType(GESTURE_STROKE_TYPE_MULTIPLE);
                setEventsInterceptionEnabled(true);
                setOrientation(ORIENTATION_VERTICAL);
                setUncertainGestureColor(context.getColor(R.color.design_default_color_primary));
                setGestureColor(context.getColor(R.color.design_default_color_background));
                setOnTouchListener(HomescreenActivity.this);
                setFadeEnabled(true);
                setFadeOffset(500);
            }});
        }});

        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // We need to delay the setting of the gesture overlay height so that we can set it
            // relative to the height of the controls on the Jitsi view
            mJitsiMeetView.setLayoutParams(new FrameLayout.LayoutParams(
                    /* width: */ FrameLayout.LayoutParams.MATCH_PARENT,
                    /* height: */ FrameLayout.LayoutParams.MATCH_PARENT));
            mGestureOverlayView.setLayoutParams(new FrameLayout.LayoutParams(
                    /* width: */ FrameLayout.LayoutParams.MATCH_PARENT,
                    /* height: */ relativeLayout.getHeight() - JITSI_CONTROLS_HEIGHT_PX));
        });

        mDetector = new GestureDetectorCompat(this, mSocialTouchInteractionService);
        mDetector.setOnDoubleTapListener(mSocialTouchInteractionService);
        mSocialTouchInteractionService.setOnInteractionListener(this);

        Reaction heartEyes = new Reaction("HEART EYES", "HEART EYES", "RED", R.drawable.heart_eyes);
        Reaction heart = new Reaction("HEART", "HEART","RED",R.drawable.heart);
        Reaction kiss = new Reaction("KISS", "KISS", "BLUE",R.drawable.kiss);
        Reaction handWave = new Reaction("HAND WAVE", "HAND WAVE","BLUE",R.drawable.hand_wave);
        Reaction giggle = new Reaction("GIGGLE", "GIGGLE","BLUE",R.drawable.giggle);
        Reaction sad = new Reaction("ANGRY", "ANGRY", "RED",R.drawable.angry_face);
        Reaction angry = new Reaction("SAD", "SAD","BLUE",R.drawable.sad_face);
        Reaction crazyFace = new Reaction("CRAZY FACE", "CRAZY FACE","BLUE",R.drawable.crazy_face);
        Reaction confetti = new Reaction("CONFETTI", "CONFETTI","BLUE",R.drawable.confetti);
        Reaction star = new Reaction("STAR", "STAR","BLUE",R.drawable.star);

        ReactButton emojiButton = new ReactButton(context);
        emojiButton.setReactions(heartEyes, heart, kiss, handWave, giggle, confetti);
        emojiButton.setDefaultReaction(heartEyes);
        relativeLayout.addView(emojiButton);


    }

    @Override
    protected void onStart() {
        super.onStart();

        mDisposables = new CompositeDisposable();

        mDisposables.add(mTangibleBleConnectionService.getConnection().subscribe(rxBleConnection -> {
            Timber.i("Successfully acquired BLE connection");

            mMainThreadHandler.post(() -> mRxBleConnectionLiveData.setValue(rxBleConnection));

        }, throwable -> {
            Timber.e(throwable);

            runOnUiThread(() -> {
                Toast.makeText(HomescreenActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            });

        }));

        mDisposables.add(mTangibleDataService.getCurrentUserRoom().subscribe(dataRecord -> {
            Timber.i("Received jitsiRoom %s from data service", dataRecord.getData().getJitsiRoom());

            runOnUiThread(() -> {
                mJitsiMeetView.join(new JitsiMeetConferenceOptions.Builder()
                        .setServerURL(URLUtils.parse("https://meet.jit.si"))
                        .setRoom(dataRecord.getData().getJitsiRoom())
                        .setAudioMuted(false)
                        .setVideoMuted(false)
                        .setAudioOnly(false)
                        .setWelcomePageEnabled(false)
                        .build());
            });

        }, throwable -> {
            Timber.e(throwable);

            runOnUiThread(() -> {
                Toast.makeText(HomescreenActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            });

        }));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDisposables != null) {
            mDisposables.dispose();
            mDisposables = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ActivityUtils.hideSystemUI(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return this.mDetector.onTouchEvent(event);
    }

    @Override
    public void onInteraction(SocialTouchInteractionService.Interaction interaction) {
        Timber.i(interaction.getBleCode());

        if (interaction == SocialTouchInteractionService.Interaction.UNKNOWN) {
            return;
        }

        mMainThreadHandler.post(() -> {
            RxBleConnection bleConnection = mRxBleConnectionLiveData.getValue();
            if (bleConnection == null) {
                return;
            }

            byte[] message = mTangibleBleConnectionService.getTangibleInteractionMessageWithCrc(interaction);

            mDisposables.add(bleConnection.writeCharacteristic(Characteristics.RX, message).subscribe(result -> {

                Timber.i("Received ack from BLE device for message %s", new String(result));

            }, throwable -> {

                Timber.e(throwable);

                runOnUiThread(() -> {
                    Toast.makeText(HomescreenActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }));
        });
    }
}
