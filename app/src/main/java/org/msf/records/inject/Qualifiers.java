package org.msf.records.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

public class Qualifiers {

    // .events

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface CrudEventBusBuilder {}


    // .net

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsRootUrl {}

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsUser {}

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface OpenMrsPassword {}

    @Qualifier @Documented @Retention(RetentionPolicy.RUNTIME)
    public @interface XformUpdateClientCache {}

    private Qualifiers() {}
}
