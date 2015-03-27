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

package org.msf.records.utils;

import org.msf.records.ui.BaseActivity;
import org.msf.records.ui.userlogin.UserLoginActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/** A Dagger module that provides bindings for utilities. */
@Module(
        injects = {
                BaseActivity.class,
                UserLoginActivity.class
        },
        complete = false,
        library = true)
public class UtilsModule {

    @Provides
    @Singleton
    AsyncTaskRunner provideAsyncTaskRunner() {
        return AsyncTaskRunner.DEFAULT;
    }

    @Provides
    @Singleton
    ActivityHierarchyServer provideActivityHierarchyServer() {
        return ActivityHierarchyServer.NONE;
    }

    @Provides
    @Singleton
    Colorizer provideUserColorizer() {
        return Colorizer
                .withPalette(
                        0xffb0120a, 0xff880e4f, 0xff4a148c, 0xff311b92, 0xff1a237e, 0xff2a36b1,
                        0xff01579b, 0xff006064, 0xff004d40, 0xff0d5302, 0xff33691e, 0xff827717,
                        0xfff57f17, 0xffff6f00, 0xffe65100, 0xffbf360c, 0xff3e2723)
                .withTint(.2);
    }
}
