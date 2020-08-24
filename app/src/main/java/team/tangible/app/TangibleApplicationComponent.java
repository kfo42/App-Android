package team.tangible.app;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.logging.Handler;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import team.tangible.app.activities.PairingActivity;
import team.tangible.app.activities.SplashActivity;
import team.tangible.app.services.AuthenticationService;

/**
 * A Dagger Component is the link between the application and it's dependencies.
 * Here, you register activities or fragments that require injected objects.
 * You must also specify the @Module-annotated classes that @Provide these objects.
 */
@Singleton
@Component(modules = { TangibleApplicationModule.class })
public interface TangibleApplicationComponent {

    // This tells Dagger that LoginActivity requests injection so the graph needs to
    // satisfy all the dependencies of the fields that LoginActivity is injecting.
    void inject(SplashActivity splashActivity);
    void inject(PairingActivity pairingActivity);

    /**
     * This is a custom builder that provides the Context object used as dependencies in the
     * MyPHDApplicationModule class. This interface is used in the build process to automatically
     * generate a builder. Kinda magical!
     */
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder context(Context context);

        /**
         * Required method for Dagger. Otherwise a build issue will pop-up
         */
        Builder setTangibleApplicationModule(TangibleApplicationModule module);

        /**
         * Required method
         */
        TangibleApplicationComponent build();
    }
}
