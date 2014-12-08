package org.msf.records;

/**
 * The modules used for injection in a release build.
 */
final class Modules {

    static Object[] list(App app) {
        return new Object[] {
                new AppModule(app)
        };
    }

    private Modules() {}
}
