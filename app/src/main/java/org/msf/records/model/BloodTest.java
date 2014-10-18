package org.msf.records.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Gil on 13/10/2014.
 */
public class BloodTest {

    public String type;
    public Date created;
    public String comment;
    public Date completed;

    public BloodTest(String type, String comment, Date created, Date completed){
        this.type = type;
        this.comment = comment;
        this.created = created;
        this.completed = completed;
    }


    public static List<BloodTest> GETDUMMYDATA1() {
        ArrayList<BloodTest> bloodTests = new ArrayList<BloodTest>();
        bloodTests.add(new BloodTest("Ebola blood test", "Sample received", new Date(), null));
        bloodTests.add(new BloodTest("Ebola blood test", "Sample received", new Date(), null));
        bloodTests.add(new BloodTest("Ebola blood test", "Sample received", new Date(), null));
        bloodTests.add(new BloodTest("Ebola blood test", "Sample received", new Date(), null));
        bloodTests.add(new BloodTest("Ebola blood test", "Sample received", new Date(), null));
        return bloodTests;
    }
}
