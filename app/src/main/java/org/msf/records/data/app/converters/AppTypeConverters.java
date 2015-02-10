package org.msf.records.data.app.converters;

import javax.annotation.concurrent.Immutable;

/**
 * A convenience object that provides access to all {@link AppTypeConverter} instances available.
 */
@Immutable
public class AppTypeConverters {

    public final AppPatientConverter patient;
    public final AppLocationConverter location;
    public final AppEncounterConverter encounter;

    AppTypeConverters(
            AppPatientConverter patientConverter,
            AppLocationConverter locationConverter,
            AppEncounterConverter encounterConverter) {
        patient = patientConverter;
        location = locationConverter;
        encounter = encounterConverter;
    }
}
