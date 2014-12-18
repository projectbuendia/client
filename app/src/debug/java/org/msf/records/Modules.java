package org.msf.records;

/**
 * The modules used for injection in a debug build.
 */
final class Modules {

    static Object[] list(App app) {
        return new Object[] {
                new AppModule(app),
                DebugAppModule.class
        };
    }

    private Modules() {}
}
