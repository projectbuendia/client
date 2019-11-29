package org.projectbuendia.client.models;

import org.projectbuendia.client.models.Catalog.Category;
import org.projectbuendia.client.models.Catalog.Drug;
import org.projectbuendia.client.models.Catalog.Format;
import org.projectbuendia.client.models.Catalog.Route;

import java.util.HashMap;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

public class CatalogIndex {
    Category[] categories = {};
    Map<String, Drug> drugs = new HashMap<>();
    Map<String, Format> formats = new HashMap<>();
    Route[] routes = {};
    Unit[] units = {};

    public CatalogIndex(Category... categories) {
        this.categories = categories;
        for (Category category : categories) {
            for (Drug drug : category.drugs) {
                drugs.put(drug.code, drug);
                for (Format format : drug.formats) {
                    formats.put(format.code, format);
                }
            }
        }
    }

    public CatalogIndex withRoutes(Route... routes) {
        this.routes = routes;
        return this;
    }

    public CatalogIndex withUnits(Unit... units) {
        this.units = units;
        return this;
    }

    public Category[] getCategories() {
        return categories;
    }

    public Drug getDrug(String code) {
        if (code.length() > 8) code = code.substring(0, 8);
        if (drugs.containsKey(code)) {
            return drugs.get(code);
        }
        return Drug.UNSPECIFIED;
    }

    public Format getFormat(String code) {
        code = code.replaceAll("-*$", "");
        if (formats.containsKey(code)) {
            return formats.get(code);
        }
        return Format.UNSPECIFIED;
    }

    public Route getRoute(String code) {
        for (Route route : routes) {
            if (eq(code, route.code)) return route;
        }
        return Route.UNSPECIFIED;
    }

    public Unit getUnit(String code) {
        for (Unit unit : units) {
            if (eq(code, unit.code)) return unit;
        }
        return Unit.UNSPECIFIED;
    }
}
