package org.projectbuendia.client.models;

import java.util.Arrays;
import java.util.List;

public class MsfCatalog implements Catalog {
    Unit TABLET = new Unit("tablet [fr:comprimé]", "tablets [fr:comprimés]");
    Unit CAPSULE = new Unit("capsule", "capsules");
    Unit MG = new Unit("mg", "mg");
    Unit ML = new Unit("mL", "mL");
    Unit DROP = new Unit("drop", "drops");
    Unit PUFF = new Unit("puff", "puffs");
    Unit AMPOULE = new Unit("ampoule", "ampoules");
    Unit SACHET = new Unit("sachet", "sachets");

    Category[] categories = {
        new Category("oral", "DORA", "PO").withDrugs(
            new Drug("ABACAVIR sulfate", "ABC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
                new Format("DORAABCV3T", "eq. 300 mg base, tab.", TABLET),
                new Format("DORAABCV6TD", "eq. 60 mg, disp. tab.", TABLET)
            ),
            new Drug("ABACAVIR/LAMIVUDINE", "ABC/3TC").withCaptions(MsfSupplyCatalog.ANTIRETROVIRAL).withFormats(
                new Format("DORAABLA1TD", "60 mg/30 mg, disp. tab.", TABLET),
                new Format("DORAABLA2T3", "600 mg/300 mg, tab.", TABLET),
                new Format("DORAABLA3TD", "120 mg/60 mg, disp. tab.", TABLET)
            )
        )
    };

    public List<Category> getCategories() {
        return Arrays.asList(categories);
    }
}
