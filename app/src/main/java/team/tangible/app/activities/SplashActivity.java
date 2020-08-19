package team.tangible.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import team.tangible.app.R;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3_000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        bind();

        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(() -> {
            /* Create an Intent that will start the Menu-Activity. */
            Intent mainIntent = new Intent(SplashActivity.this, PairingActivity.class);
            SplashActivity.this.startActivity(mainIntent);
        }, SPLASH_DISPLAY_LENGTH);
    }
}