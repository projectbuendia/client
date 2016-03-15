package org.projectbuendia.client.debug;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;

/**
 * Potentially initializes Stetho, the debug bridge.
 */
public interface StethoInitializer {
  void initialize(Context context);
  void registerInterceptors(OkHttpClient okHttpClient);

  class NoOp implements StethoInitializer {

    @Override
    public void initialize(Context context) {}

    @Override
    public void registerInterceptors(OkHttpClient okHttpClient) {}
  }
}
