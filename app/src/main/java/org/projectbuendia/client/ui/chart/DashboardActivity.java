// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.chart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.BaseLoggedInActivity;
import org.projectbuendia.client.ui.chart.DashboardController.MinimalHandler;
import org.projectbuendia.client.ui.chart.DashboardController.Stat;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** Activity displaying the overall program dashboard. */
public final class DashboardActivity extends BaseLoggedInActivity {
    private DashboardController mController;

    @Inject AppModel mAppModel;
    @Inject EventBus mEventBus;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;
    @Inject ChartDataHelper mChartDataHelper;
    @InjectView(R.id.dashboard_webview) WebView mWebView;

    public static void start(Context caller) {
        Intent intent = new Intent(caller, DashboardActivity.class);
        caller.startActivity(intent);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Go back rather than reloading the activity, so that the patient list retains its
            // filter state.
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onCreateImpl(Bundle savedInstanceState) {
        super.onCreateImpl(savedInstanceState);
        setContentView(R.layout.dashboard_fragment);

        ButterKnife.inject(this);
        App.getInstance().inject(this);

        final MinimalHandler minimalHandler = new DashboardController.MinimalHandler() {
            private final Handler mHandler = new Handler();

            @Override public void post(Runnable runnable) {
                mHandler.post(runnable);
            }
        };
        mController = new DashboardController(
            mAppModel,
            new EventBusWrapper(mEventBus),
            mCrudEventBusProvider.get(),
            new Ui(),
            mChartDataHelper,
            mSyncManager,
            minimalHandler,
            getContentResolver());

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override protected void onStartImpl() {
        super.onStartImpl();
        mController.init();
    }

    @Override protected void onStopImpl() {
        mController.suspend();
        super.onStopImpl();
    }

    private String renderHtml(
        LocationTree tree, Map<String, Stat> statsByLocationUuid) {
        Map<String, Object> context = new HashMap<>();
        context.put("statsByLocationUuid", statsByLocationUuid);
        context.put("locations", tree.getDescendantsAtDepth(2));
        return Utils.renderTemplate("dashboard.html", context);
    }

    private final class Ui implements DashboardController.Ui {
        @Override public void updateDashboard(
            LocationTree tree, Map<String, Stat> statsByLocationUuid) {

            if (tree == null) return;

            // setDefaultFontSize is supposed to take a size in sp, but in practice
            // the fonts don't change size when the user font size preference changes.
            // So, we apply the scaling factor explicitly, defining 1 em to be 10 sp.
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            float defaultFontSize = 10*metrics.scaledDensity/metrics.density;
            mWebView.getSettings().setDefaultFontSize((int) defaultFontSize);

            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setWebChromeClient(new WebChromeClient());
            String html = renderHtml(tree, statsByLocationUuid);
            // If we only call loadData once, the WebView doesn't render the new HTML.
            // If we call loadData twice, it works.  TODO: Figure out what's going on.
            mWebView.loadDataWithBaseURL("file:///android_asset/", html,
                "text/html; charset=utf-8", "utf-8", null);
            mWebView.loadDataWithBaseURL("file:///android_asset/", html,
                "text/html; charset=utf-8", "utf-8", null);
            mWebView.setWebContentsDebuggingEnabled(true);
        }
    }
}
