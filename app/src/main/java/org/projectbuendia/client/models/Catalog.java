package org.projectbuendia.client.models;

import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Utils;

import java.util.List;

// Catalog -->* Category -->* Drug -->* Format --> Unit
public interface Catalog {
    List<Category> getCategories();

    class Category {
        public final Loc name;  // drug category, e.g. "oral", "injectable", "external"
        public final String code;  // category identifier, e.g. "DORA", "DINJ", "DEXT"
        public final Loc[] routes;  // routes of administration, e.g. PO, IV, SC
        public final Drug[] drugs;

        public Category(Loc name, String code, Loc[] routes, Drug[] drugs) {
            this.name = name;
            this.code = code;
            this.routes = routes;
            this.drugs = drugs;
        }

        public Category(String name, String code, String[] routes, Drug[] drugs) {
            this(new Loc(name), code, Loc.newArray(routes), drugs);
        }

        public Category(String name, String code, String... routes) {
            this(name, code, routes, new Drug[0]);
        }

        public Category withDrugs(Drug... drugs) {
            return new Category(name, code, routes, drugs);
        }
    }

    class Drug {
        public final Loc name;  // active ingredient, title case, e.g. "Acetylsalicylic Acid"
        public final String[] aliases;  // alternative names, title case, e.g. {"Aspirin", "ASA"}
        public final Loc[] captions;  // therapeutic action, lowercase noun, e.g. {"analgesic", "antipyretic"}
        public final Format[] formats;

        public Drug(Loc name, String[] aliases, Loc[] captions, Format[] formats) {
            this.name = name;
            this.aliases = aliases;
            this.captions = Utils.orDefault(captions, new Loc[0]);
            this.formats = Utils.orDefault(formats, new Format[0]);
        }

        public Drug(String name, String... aliases) {
            this(new Loc(name), aliases, null, null);
        }

        public Drug withCaptions(Loc... captions) {
            return new Drug(name, aliases, captions, formats);
        }

        public Drug withFormats(Format... formats) {
            return new Drug(name, aliases, captions, formats);
        }

    }

    class Format {
        public final String code;  // stock code, e.g. "DORAACSA3TD"
        public final Loc formulation;  // quantity, concentration, form, e.g. "300 mg, disp. tab."
        public final Unit dosageUnit;

        public Format(String code, String formulation, Unit dosageUnit) {
            this(code, new Loc(formulation), dosageUnit);
        }

        public Format(String code, Loc formulation, Unit dosageUnit) {
            this.code = code;
            this.formulation = formulation;
            this.dosageUnit = dosageUnit;
        }
    }

    class Unit {
        public final Loc singular;  // unit of prescription, singular, e.g. "tablet"
        public final Loc plural;  // unit of prescription, plural, e.g. "tablets"

        public Unit(String unit) {
            this(new Loc(unit));
        }

        public Unit(Loc unit) {
            singular = plural = unit;
        }

        public Unit(String singular, String plural) {
            this(new Loc(singular), new Loc(plural));
        }

        public Unit(Loc singular, Loc plural) {
            this.singular = singular;
            this.plural = plural;
        }
    }
}
