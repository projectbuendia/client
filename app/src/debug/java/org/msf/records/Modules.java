package org.msf.records;

/**
 * Created by dxchen on 12/8/14.
 */
final class Modules {
    static Object[] list(App app) {
        return new Object[] {
                new AppModule(app),
        };
    }

    private Modules() {}
}
