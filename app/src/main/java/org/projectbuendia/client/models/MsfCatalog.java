package org.projectbuendia.client.models;

public interface MsfCatalog extends Catalog {
    Route PO = new Route("PO", "oral", "PO");
    Route IV = new Route("IV", "intravenous", "IV");
    Route SC = new Route("SC", "subcutaneous", "SC");
    Route IM = new Route("IM", "intramuscular", "IM");

    Category ORAL = new Category("DORA", "oral", DosingType.QUANTITY, PO).withDrugs(
        new Drug("DORAABCV", "ABACAVIR sulfate", "ABC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
            new Format("DORAABCV3T", "eq. 300 mg base, tab.", Unit.TABLET),
            new Format("DORAABCV6TD", "eq. 60 mg, disp. tab.", Unit.TABLET)
        ),
        new Drug("DORAABLA", "ABACAVIR/LAMIVUDINE", "ABC/3TC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
            new Format("DORAABLA1TD", "60 mg/30 mg, disp. tab.", Unit.TABLET),
            new Format("DORAABLA2T3", "600 mg/300 mg, tab.", Unit.TABLET),
            new Format("DORAABLA3TD", "120 mg/60 mg, disp. tab.", Unit.TABLET)
        )
    );

    Category INJECTABLE = new Category("DINJ", "injectable", DosingType.QUANTITY, IV, SC, IM).withDrugs(
        new Drug("DINJCEFL", "CEFTRIAXONE sodium [fr:CEFTRIAXONE sodique]").withCaptions(MsfSupplyCatalog.ANTIBACTERIAL).withFormats(
            new Format("DINJCEFL1V", "eg. 1 g base, powder, vial + lidocaine IM", Unit.MG),
            new Format("DINJCEFL2V", "eg. 250 mg base, powder, vial + lidocaine IM", Unit.MG)
        )
    );

    Category INFUSIBLE = new Category("DINF", "infusible", DosingType.QUANTITY_OVER_DURATION).withDrugs(
        new Drug("DINFRINL", "RINGER lactate").withCaptions(MsfSupplyCatalog.FLUID_REPLACER).withFormats(
            new Format("DINFRINL1FBF1", "1 L, flex. bag, PVC free", Unit.ML),
            new Format("DINFRINL1FBF5", "500 mL, flex. bag, PVC free", Unit.ML),
            new Format("DINFRINL1FBP1", "1 L, flex. bag, PVC", Unit.ML),
            new Format("DINFRINL1FBP5", "500 mL, flex. bag, PVC", Unit.ML),
            new Format("DINFRINL1SRF1", "1 L, semi-rigid bot., PVC free", Unit.ML),
            new Format("DINFRINL1SRF5", "500 mL, semi-rigid bot., PVC free", Unit.ML)
        )
    );

    Category EXTERNAL = new Category("DEXT", "external", DosingType.QUANTITY);

    Category VACCINE = new Category("DVAC", "vaccines/immunoglobulins", DosingType.QUANTITY);

    CatalogIndex INDEX = new CatalogIndex(ORAL, INJECTABLE, INFUSIBLE, EXTERNAL, VACCINE)
        .withRoutes(PO, IV, SC, IM)
        .withUnits(Unit.TABLET, Unit.CAPSULE, Unit.MG, Unit.ML, Unit.DROP, Unit.PUFF, Unit.AMPOULE, Unit.SACHET);
}
