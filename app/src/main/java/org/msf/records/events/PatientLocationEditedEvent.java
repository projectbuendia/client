package org.msf.records.events;

import org.msf.records.model.Location2;

/**
 * An event bus event that indicates that the patient location has been edited.
 */
public class PatientLocationEditedEvent {

    public final Location2 mLocation;

    public PatientLocationEditedEvent(Location2 location) {
        mLocation = location;
    }
}
