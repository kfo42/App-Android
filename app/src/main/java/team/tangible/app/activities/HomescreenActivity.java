package team.tangible.app.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import team.tangible.app.R;
import team.tangible.app.utils.URLUtils;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.jitsi.meet.sdk.JitsiMeetActivity;


public class HomescreenActivity extends JitsiMeetActivity {

    private JitsiMeetView mJitsiMeetView;
    private static final int JITSI_CONTROLS_HEIGHT_PX = 600;
    private FrameLayout mFrameLayout;
    private GestureOverlayView mGestureOverlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);
        
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.activity_homescreen);
        Context context = relativeLayout.getContext();

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
    }

}