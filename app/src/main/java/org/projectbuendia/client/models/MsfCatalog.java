package org.projectbuendia.client.models;

public interface MsfCatalog extends Catalog {
    Route PO = new Route("PO", "oral", "PO");
    Route IV = new Route("IV", "intravenous", "IV");
    Route SC = new Route("SC", "subcutaneous", "SC");
    Route IM = new Route("IM", "intramuscular", "IM");

    Unit TABLET = new Unit("tablet [fr:comprimé]", "tablets [fr:comprimés]");
    Unit CAPSULE = new Unit("capsule", "capsules");
    Unit MG = new Unit("mg", "mg");
    Unit ML = new Unit("mL", "mL");
    Unit DROP = new Unit("drop", "drops");
    Unit PUFF = new Unit("puff", "puffs");
    Unit AMPOULE = new Unit("ampoule", "ampoules");
    Unit SACHET = new Unit("sachet", "sachets");

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

    Category INFUSIBLE = new Category("DINF", "infusible", DosingType.QUANTITY_OVER_TIME).withDrugs(
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
}
