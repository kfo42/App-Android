package team.tangible.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import team.tangible.app.R;

public class LoginActivity extends BaseActivity {

    @BindView(R.id.activity_login_login_button)
    public Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.activity_login_login_button)
    public void onLoginButtonClicked() {
        Intent moveToHomeScreenIntent = new Intent(LoginActivity.this, HomescreenActivity.class);
        startActivity(moveToHomeScreenIntent);
    }
}
