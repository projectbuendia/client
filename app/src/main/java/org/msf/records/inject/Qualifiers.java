package org.msf.records.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

public class Qualifiers {

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsRootUrl {}

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsUser {}

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsPassword {}

    private Qualifiers() {}
}
