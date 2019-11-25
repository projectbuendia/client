package org.projectbuendia.client.models;

public interface MsfCatalog extends Catalog {
    Route PO = new Route("PO", "oral", "PO");
    Route IV = new Route("IV", "intravenous", "IV");
    Route SC = new Route("SC", "subcutaneous", "SC");
    Route IM = new Route("IM", "intramuscular", "IM");

    Unit TABLET = new Unit("TABLET", "tablet [fr:comprimé]", "tablets [fr:comprimés]", "tab. [fr:comp.]");
    Unit CAPSULE = new Unit("CAPSULE", "capsule", "capsules", "cap.");
    Unit MG = new Unit("MG", "milligram", "milligrams", "mg");
    Unit ML = new Unit("ML", "milliliter", "milliliters", "mL");
    Unit DROP = new Unit("DROP", "drop", "drops", "drop");
    Unit PUFF = new Unit("PUFF", "puff", "puffs", "puff");
    Unit AMPOULE = new Unit("AMPOULE", "ampoule", "ampoules", "amp.");
    Unit SACHET = new Unit("SACHET", "sachet", "sachets", "sach.");

    Unit HOUR = new Unit("HOUR", "hour", "hours", "hr", "h");
    Unit MINUTE = new Unit("MINUTE", "minute", "minutes", "min");
    Unit SECOND = new Unit("SECOND", "second", "seconds", "sec", "s");

    Unit PER_DAY = new Unit("PER_DAY", "time per day", "times per day", "\bx per day", "\bx/day");

    Category ORAL = new Category("DORA", "oral", DosingType.QUANTITY, PO).withDrugs(
        new Drug("DORAABCV", "ABACAVIR sulfate", "ABC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
            new Format("DORAABCV3T", "eq. 300 mg base, tab.", TABLET),
            new Format("DORAABCV6TD", "eq. 60 mg, disp. tab.", TABLET)
        ),
        new Drug("DORAABLA", "ABACAVIR/LAMIVUDINE", "ABC/3TC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
            new Format("DORAABLA1TD", "60 mg/30 mg, disp. tab.", TABLET),
            new Format("DORAABLA2T3", "600 mg/300 mg, tab.", TABLET),
            new Format("DORAABLA3TD", "120 mg/60 mg, disp. tab.", TABLET)
        )
    );

    Category INJECTABLE = new Category("DINJ", "injectable", DosingType.QUANTITY, IV, SC, IM).withDrugs(
        new Drug("DINJCEFL", "CEFTRIAXONE sodium [fr:CEFTRIAXONE sodique]").withCaptions(MsfSupplyCatalog.ANTIBACTERIAL).withFormats(
            new Format("DINJCEFL1V", "eg. 1 g base, powder, vial + lidocaine IM", MG),
            new Format("DINJCEFL2V", "eg. 250 mg base, powder, vial + lidocaine IM", MG)
        )
    );

    Category INFUSIBLE = new Category("DINF", "infusible", DosingType.QUANTITY_OVER_DURATION).withDrugs(
        new Drug("DINFRINL", "RINGER lactate").withCaptions(MsfSupplyCatalog.FLUID_REPLACER).withFormats(
            new Format("DINFRINL1FBF1", "1 L, flex. bag, PVC free", ML),
            new Format("DINFRINL1FBF5", "500 mL, flex. bag, PVC free", ML),
            new Format("DINFRINL1FBP1", "1 L, flex. bag, PVC", ML),
            new Format("DINFRINL1FBP5", "500 mL, flex. bag, PVC", ML),
            new Format("DINFRINL1SRF1", "1 L, semi-rigid bot., PVC free", ML),
            new Format("DINFRINL1SRF5", "500 mL, semi-rigid bot., PVC free", ML)
        )
    );

    Category EXTERNAL = new Category("DEXT", "external", DosingType.QUANTITY);

    Category VACCINE = new Category("DVAC", "vaccines/immunoglobulins", DosingType.QUANTITY);

    CatalogIndex INDEX = new CatalogIndex(ORAL, INJECTABLE, INFUSIBLE, EXTERNAL, VACCINE)
        .withRoutes(PO, IV, SC, IM)
        .withUnits(TABLET, CAPSULE, MG, ML, DROP, PUFF, AMPOULE, SACHET);
}
