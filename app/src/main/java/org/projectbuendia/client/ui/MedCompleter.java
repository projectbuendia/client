package org.projectbuendia.client.ui;

import android.support.annotation.NonNull;

import org.projectbuendia.client.ui.AutocompleteAdapter.Completer;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MedCompleter implements Completer {
    public static Med[] PO_MEDS = {
        new Med("Abacavir", "ABC"),
        new Med("Acetylsalicylic acid", "Aspirin", "ASA"),
        new Med("Aciclovir", "Acyclovir"),
        new Med("Activated charcoal"),
        new Med("Albendazole"),
        new Med("Albuterol aerosol"),
        new Med("Albuterol nebuliser solution"),
        new Med("Aluminium hydroxide"),
        new Med("Amitriptyline"),
        new Med("Amlodipine"),
        // ! new Med("Amodiaquine", "AQ"),
        new Med("Amoxicillin"),
        // ! new Med("Artesunate", "AS"),
        new Med("Artesunate/Amodiaquine", "AS/AQ"),
        new Med("Artesunate/Sulfadoxine/Pyrimethamine", "AS/SP"),
        new Med("Ascorbic acid", "Vitamin C"),
        new Med("Atazanavir", "ATV"),
        new Med("Azithromycin"),
        new Med("Beclometasone aerosol"),
        new Med("Biperiden"),
        new Med("Bisacodyl"),
        new Med("Bisoprolol"),
        new Med("Butylscopolamine"),
        new Med("Cabergoline"),
        new Med("Calcium folinate", "Folinic acid"),
        new Med("Carbamazepine"),
        new Med("Cefalexin"),
        new Med("Cefixime"),
        new Med("Chloramphenicol"),
        new Med("Chloroquine sulfate or phosphate"),
        new Med("Chlorphenamine", "Chlorpheniramine"),
        new Med("Chlorpromazine"),
        new Med("Cimetidine"),
        new Med("Ciprofloxacin"),
        new Med("Clindamycin"),
        new Med("Cloxacillin"),
        new Med("Co-amoxiclav", "Amoxicillin/Clavulanic acid"),
        new Med("Co-artemether", "Artemether/Lumefantrine"),
        new Med("Codeine"),
        new Med("Colecalciferol", "Vitamin D3"),
        new Med("Co-trimoxazole", "Sulfamethoxazole/Trimethoprim", "SMX/TMP"),
        new Med("Dapsone"),
        new Med("Darunavir", "DRV"),
        new Med("Desogestrel"),
        new Med("Diazepam"),
        new Med("Diethylcarbamazine"),
        new Med("Digoxin"),
        new Med("Dihydroartemisinin/Piperaquine", "DHA/PPQ"),
        // ! new Med("Dipyrone"),
        new Med("Dolutegravir", "DTG"),
        new Med("Doxycycline"),
        new Med("Efavirenz", "EFV", "EFZ"),
        new Med("Enalapril"),
        new Med("Ergocalciferol", "Vitamin D2"),
        new Med("Erythromycin"),
        new Med("Ethambutol", "E"),
        new Med("Ethinylestradiol/Levonorgestrel"),
        new Med("Ferrous salts"),
        new Med("Ferrous salts/Folic acid"),
        new Med("Fluconazole"),
        new Med("Flucytosine"),
        new Med("Fluoxetine"),
        new Med("Folic acid", "Vitamin B9"),
        new Med("Fosfomycin trometamol"),
        new Med("Furosemide"),
        new Med("Glibenclamide"),
        new Med("Gliclazide"),
        new Med("Glyceryl trinitrate", "Nitroglycerin", "Trinitrin"),
        new Med("Griseofulvin"),
        new Med("Haloperidol"),
        new Med("Hydrochlorothiazide"),
        new Med("Hydroxyzine"),
        new Med("Hyoscine butylbromide", "Butylscopolamine"),
        new Med("Ibuprofen"),
        new Med("Iodized oil"),
        new Med("Ipratropium bromide nebuliser solution"),
        new Med("Isoniazid", "H"),
        new Med("Isosorbide dinitrate"),
        new Med("Itraconazole"),
        new Med("Ivermectin"),
        new Med("Labetalol"),
        new Med("Lactulose"),
        new Med("Lamivudine", "3TC"),
        new Med("Levodopa/Carbidopa"),
        new Med("Levonorgestrel"),
        new Med("Loperamide"),
        new Med("Lopinavir/Ritonavir", "LPV/R"),
        new Med("Loratadine"),
        new Med("Mebendazole"),
        new Med("Mefloquine", "MQ"),
        // ! new Med("Metamizole"),
        new Med("Metformin"),
        new Med("Methyldopa"),
        new Med("Metoclopramide"),
        new Med("Metronidazole"),
        new Med("Miconazole"),
        new Med("Mifepristone"),
        new Med("Misoprostol"),
        new Med("Morphine immediate-release", "MIR"),
        new Med("Morphine sustained-release", "MSR"),
        new Med("Multivitamins", "Vitamin B complex"),
        new Med("Nevirapine", "NVP"),
        new Med("Niclosamide"),
        new Med("Nicotinamide", "Vitamin PP", "Vitamin B3"),
        new Med("Nifedipine"),
        new Med("Nitrofurantoin"),
        new Med("Nitroglycerin"),
        // ! new Med("Noramidopyrine"),
        new Med("Nystatin"),
        new Med("Olanzapine"),
        new Med("Omeprazole"),
        new Med("Oral rehydration salts", "ORS"),
        new Med("Paracetamol", "Acetaminophen"),
        new Med("Paroxetine"),
        new Med("Phenobarbital"),
        new Med("Phenoxymethylpenicillin", "Penicillin V"),
        new Med("Phenytoin"),
        new Med("Potassium chloride immediate-release"),
        new Med("Potassium chloride sustained-release"),
        new Med("Praziquantel"),
        new Med("Prednisolone and prednisone"),
        new Med("Promethazine"),
        new Med("Pyrantel"),
        new Med("Pyrazinamide", "Z"),
        new Med("Pyridoxine", "Vitamin B6"),
        new Med("Pyrimethamine"),
        new Med("Quinine"),
        new Med("Resomal", "Rehydration solution for malnutrition"),
        new Med("Retinol", "Vitamin A"),
        new Med("Rifampicin", "R"),
        new Med("Risperidone"),
        new Med("Ritonavir", "RTV"),
        new Med("Salbutamol", "Albuterol aerosol"),
        new Med("Salbutamol", "Albuterol nebuliser solution"),
        new Med("Sertraline"),
        new Med("Sodium valproate"),
        new Med("Spironolactone"),
        new Med("Sulfadiazine"),
        new Med("Sulfadoxine/Pyrimethamine", "SP"),
        new Med("Sulfamethoxazole/Trimethoprim", "SMX/TMP"),
        new Med("Tenofovir disoproxil fumarate", "TDF"),
        new Med("Thiamine", "Vitamin B1"),
        new Med("Tinidazole"),
        new Med("Tramadol"),
        new Med("Tranexamic acid"),
        new Med("Triclabendazole"),
        new Med("Trihexyphenidyl"),
        new Med("Ulipristal"),
        new Med("Valproic acid", "Sodium valproate"),
        new Med("Vitamin B6"),
        new Med("Zidovudine", "AZT", "ZDV"),
        new Med("Zinc sulfate"),
    };

    @Override public Collection<? extends Completion> suggestCompletions(CharSequence constraint) {
        String searchKey = normalize(" " + constraint);
        List<Med> results = new ArrayList<>();
        for (Med med : PO_MEDS) {
            if (med.filterTarget.contains(searchKey)) {
                results.add(med);
            }
        }
        return results;
    }

    private static String normalize(CharSequence name) {
        return name.toString().toLowerCase().replaceAll("[^a-z0-9]+", " ");
    }

    public static class Med implements Completion {
        String name;
        String[] aliases;
        String label;
        String filterTarget;

        public Med(String name, String... aliases) {
            this.name = name;
            this.aliases = aliases;
            label = name;
            filterTarget = " " + name.toLowerCase();
            for (String alias : aliases) {
                label += " (" + alias + ")";
                filterTarget += " " + alias;
            }
            String collapsed = filterTarget.replaceAll("[^a-z0-9]+", "");
            filterTarget = normalize(" " + filterTarget + " " + collapsed + " ");
        }

        public @NonNull String getLabel() {
            return label;
        }

        public @NonNull String getValue() {
            return name;
        }
    }
}
