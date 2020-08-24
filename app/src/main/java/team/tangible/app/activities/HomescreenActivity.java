package team.tangible.app.activities;

import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.disposables.CompositeDisposable;
import team.tangible.app.Constants;
import team.tangible.app.R;
import team.tangible.app.TangibleApplication;
import team.tangible.app.services.SocialTouchInteractionService;
import team.tangible.app.services.TangibleBleConnectionService;
import team.tangible.app.utils.ActivityUtils;
import team.tangible.app.utils.URLUtils;
import timber.log.Timber;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetView;

import javax.inject.Inject;
import javax.inject.Named;


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
    TangibleBleConnectionService mTangibleBleConnectionService;

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
                JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                        .setServerURL(URLUtils.parse("https://meet.jit.si"))
                        .setRoom("pramod-katie-testing-demo")
                        .setAudioMuted(false)
                        .setVideoMuted(false)
                        .setAudioOnly(false)
                        .setWelcomePageEnabled(false)
                        .build();
                join(options);
            }});
            addView(mGestureOverlayView = new GestureOverlayView(context) {{
                setId(View.generateViewId());
                setGestureStrokeType(GESTURE_STROKE_TYPE_MULTIPLE);
                setEventsInterceptionEnabled(true);
                setOrientation(ORIENTATION_VERTICAL);
                setUncertainGestureColor(context.getColor(R.color.design_default_color_primary));
                setGestureColor(context.getColor(R.color.design_default_color_secondary));
                setOnTouchListener(HomescreenActivity.this);
            }});
        }});

        relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // We need to delay the setting of the gesture overlay height so that we can set it
            // relative to the height of the controls on the Jitsi view
            mJitsiMeetView.setLayoutParams(new FrameLayout.LayoutParams(
                    /* width: */ FrameLayout.LayoutParams.MATCH_PARENT,
                    /* height: */ ViewGroup.LayoutParams.MATCH_PARENT));
            mGestureOverlayView.setLayoutParams(new FrameLayout.LayoutParams(
                    /* width: */ ViewGroup.LayoutParams.MATCH_PARENT,
                    /* height: */ relativeLayout.getHeight() - JITSI_CONTROLS_HEIGHT_PX));
        });

        mDetector = new GestureDetectorCompat(this, mSocialTouchInteractionService);
        mDetector.setOnDoubleTapListener(mSocialTouchInteractionService);

        mSocialTouchInteractionService.setOnInteractionListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDisposables = new CompositeDisposable();

        mDisposables.add(mTangibleBleConnectionService.getConnection().subscribe(rxBleConnection -> {
            mMainThreadHandler.post(() -> mRxBleConnectionLiveData.setValue(rxBleConnection));
        }, throwable -> {
            Timber.e(throwable);
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

        mMainThreadHandler.post(() -> {
            RxBleConnection bleConnection = mRxBleConnectionLiveData.getValue();
            if (bleConnection == null) {
                return;
            }

            mDisposables.add(bleConnection.writeCharacteristic(Constants.BluetoothLowEnergy.NordicUARTService.Characteristics.RX, interaction.getBleCodeBytesWithCrc()).subscribe(result -> {
                Timber.i(new String(result));
            }, throwable -> {
                throwable.printStackTrace();
                Timber.e(throwable);
            }));
        });
    }
}
