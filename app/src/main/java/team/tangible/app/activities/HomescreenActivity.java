package team.tangible.app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import team.tangible.app.R;

public class HomescreenActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        bind();
    }
}