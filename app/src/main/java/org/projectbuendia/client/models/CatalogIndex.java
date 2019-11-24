package org.projectbuendia.client.models;

import android.view.View;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Catalog.Category;
import org.projectbuendia.client.models.Catalog.Drug;
import org.projectbuendia.client.models.Catalog.Format;
import org.projectbuendia.client.models.Catalog.Route;
import org.projectbuendia.client.models.Catalog.Unit;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completer;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completion;
import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

public class CatalogIndex implements Completer {
    Category[] categories;
    Map<String, Drug> drugs = new HashMap<>();
    List<DrugCompletion> completions = new ArrayList<>();
    Map<String, Format> formats = new HashMap<>();
    Route[] routes;
    Unit[] units;

    public CatalogIndex(Category... categories) {
        this.categories = categories;
        for (Category category : categories) {
            for (Drug drug : category.drugs) {
                drugs.put(drug.code, drug);
                completions.add(new DrugCompletion(drug));
                for (Format format : drug.formats) {
                    formats.put(format.code, format);
                }
            }
        }
    }

    public void addRoutes(Route... routes) {
        this.routes = routes;
    }

    public void addUnits(Unit... units) {
        this.units = units;
    }

    public Category getCategory(String code) {
        for (Category category : categories) {
            if (eq(code, category.code)) return category;
        }
        return Category.UNSPECIFIED;
    }

    public Drug getDrug(String code) {
        if (drugs.containsKey(code)) {
            return drugs.get(code);
        }
        return Drug.UNSPECIFIED;
    }

    public Format getFormat(String code) {
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

    @Override public Collection<DrugCompletion> suggestCompletions(CharSequence constraint) {
        String[] searchKeys = normalize(constraint).trim().split(" ");
        for (int i = 0; i < searchKeys.length; i++) {
            searchKeys[i] = " " + searchKeys[i];
        }

        List<DrugCompletion> results = new ArrayList<>();
        for (DrugCompletion completion : completions) {
            // Look for words matching the words in the input as prefixes.
            int score = 0;
            for (String searchKey : searchKeys) {
                score += completion.filterTarget.contains(searchKey) ? 1 : 0;
            }
            if (score == searchKeys.length) {
                results.add(completion);
                continue;
            }

            if (searchKeys.length == 1) {
                // Look for words matching the letters in the input as initials.
                score = 0;
                char[] initials = searchKeys[0].trim().toCharArray();
                for (char ch : initials) {
                    score += completion.filterTarget.contains(" " + ch) ? 1 : 0;
                }
                if (score == initials.length) {
                    results.add(completion);
                }
            }
        }
        return results;
    }

    private static String normalize(CharSequence name) {
        return name.toString().toLowerCase().replaceAll("[^a-z0-9]+", " ");
    }

    public static class DrugCompletion implements Completion {
        Loc name;
        Loc[] captions;
        String filterTarget;

        public DrugCompletion(Drug drug) {
            name = drug.name;
            captions = drug.captions;
            String target = "";
            for (String localizedName : drug.name.getAll()) {
                target += " " + localizedName.toLowerCase();
            }
            for (Loc alias : drug.aliases) {
                for (String localizedAlias : alias.getAll()) {
                    target += " " + localizedAlias;
                }
            }
            String collapsed = target.replaceAll("[^a-z0-9]+", "");
            filterTarget = normalize(" " + target + " " + collapsed + " ");
        }

        public String getValue() {
            return App.localize(name);
        }

        public void showInView(View itemView) {
            Utils.setText(itemView, R.id.label, App.localize(name));
            String result = "";
            for (Loc caption : captions) {
                if (!result.isEmpty()) result += ", ";
                result += App.localize(caption);
            }
            Utils.setText(itemView, R.id.caption, result);
        }
    }
}
