package team.tangible.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.polidea.rxandroidble2.RxBleClient;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import team.tangible.app.Constants;
import team.tangible.app.services.AuthenticationService;
import team.tangible.app.services.TangibleBleConnectionService;


/**
 * This is the core of Dagger. Here dependencies between different objects are laid out.
 * This class specifies the structure of the "object graph." This class representation is then used
 * by Dagger to inject dependencies to objects when required through the application. The arguments
 * to each method in this class specify the @return type's dependencies. For example, if I add a
 * method:
 * <code>
 *     \@Provides public Apple provideApple() { return new Apple(); }
 * </code>
 * We are telling Dagger that the Apple has no dependencies. However, if we write:
 * <code>
 *     @Provides
 *     public Car provideFruit(EngineFactory engineFactory, WheelFactory wheelFactory) {
 *         return new Car(engineFactory, wheelFactory);
 *     }
 *
 *     @Provides
 *     public EngineFactory provideEngineFactory(@Named("cylinderCount") int cylinderCount) {
 *          return new EngineFactory(cylinderCount);
 *     }
 *
 *     @Provides
 *     @Named("cylinderCount")
 *     public int providesCylinderCount() { return 6; }
 *
 *     @Provides
 *     public WheelFactory provideWheelFactory(@Named("spokeCount") int spokeCount) {
 *          return new WheelFactory(spokeCount);
 *     }
 *
 *     @Provides
 *     @Named("spokeCount")
 *     public int providesSpokeCount() { return 8; }
 * </code>
 * This specification for a Car tells Dagger that a Car requires an EngineFactory and WheelFactory
 * which are provided by other methods. Further, we see that EngineFactory depends on something else
 * a cylinderCount which is matched to provideEngineFactory by the @Named parameter/method annotation.
 * Using @Named, we can have multiple methods that have the same return type without confusing Dagger
 * and leading to a build error. Magic!
 */
@Module
public class TangibleApplicationModule {
    @Provides
    @Named(Constants.SharedPreferences.TEAM_TANGIBLE_APP)
    public SharedPreferences provideTeamTangibleAppSharedPreferences(Context context) {
        return context.getSharedPreferences(Constants.SharedPreferences.TEAM_TANGIBLE_APP, Context.MODE_PRIVATE);
    }

    @Provides
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    public AuthenticationService provideAuthenticationService(FirebaseAuth firebaseAuth) {
        return new AuthenticationService(firebaseAuth);
    }

    @Provides
    @Named(Constants.Threading.MAIN_THREAD)
    public Handler provideMainThreadHandler(Context context) {
        return new Handler(context.getMainLooper());
    }

    @Provides
    public TangibleBleConnectionService provideTangibleBleConnectionService(
            RxBleClient rxBleClient,
            @Named(Constants.SharedPreferences.TEAM_TANGIBLE_APP)
            SharedPreferences sharedPreferences) {
        return new TangibleBleConnectionService(rxBleClient, sharedPreferences);
    }

    @Provides
    public RxBleClient provideRxBleClient(Context context) {
        return RxBleClient.create(context);
    }
}
