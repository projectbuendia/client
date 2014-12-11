package org.msf.records.data.app.converters;

/**
 * A convenience object that provides access to all {@link AppTypeConverter} instances available.
 */
public class AppTypeConverters {

    public final AppPatientConverter patient;

    AppTypeConverters(AppPatientConverter patientConverter) {
        patient = patientConverter;
    }
}
