package org.projectbuendia.client.debug;

import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.okhttp.OkHttpClient;

import org.projectbuendia.client.AppModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module that provides debug bindings.
 */
@Module(
    includes = {
        AppModule.class
    },
    library = true,
    overrides = true
)
public class DebugModule {

    private static final StethoInitializer STETHO_INITIALIZER = new StethoInitializer() {
        @Override
        public void initialize(Context context) {
            Stetho.initializeWithDefaults(context);
        }

        @Override
        public void registerInterceptors(OkHttpClient okHttpClient) {
            okHttpClient.networkInterceptors().add(new StethoInterceptor());
        }
    };

    @Provides
    @Singleton
    public StethoInitializer getStethoInitializer() {
        return STETHO_INITIALIZER;
    }
}
