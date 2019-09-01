package org.projectbuendia.client.models;

public class VoidObs {
    public final String obsUuid;
    public final String patientUuid;

    public VoidObs(String obsUuid, String patientUuid) {
        this.obsUuid = obsUuid;
        this.patientUuid = patientUuid;
    }
}
