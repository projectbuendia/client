package org.projectbuendia.client.ui;

import android.support.annotation.NonNull;

import org.projectbuendia.client.ui.AutocompleteAdapter.Completer;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MedCompleter implements Completer {
    public static List<Med> MEDS = deduplicate(
        // ==== MSF Essential Drugs, 2016 edition, by Sophie Pilon

        // Oral drugs
        new Med("Abacavir", "ABC"),
        new Med("Acetylsalicylic Acid", "Aspirin", "ASA"),
        new Med("Aciclovir", "Acyclovir"),
        new Med("Activated Charcoal"),
        new Med("Albendazole"),
        new Med("Aluminium Hydroxide"),
        new Med("Amitriptyline"),
        new Med("Amlodipine"),
        // ! new Med("Amodiaquine", "AQ"),
        new Med("Amoxicillin"),
        // ! new Med("Artesunate", "AS"),
        new Med("Artesunate/Amodiaquine", "AS/AQ"),
        new Med("Artesunate/Sulfadoxine/Pyrimethamine", "AS/SP"),
        new Med("Ascorbic Acid", "Vitamin C"),
        new Med("Atazanavir", "ATV"),
        new Med("Azithromycin"),
        new Med("Beclometasone aerosol"),
        new Med("Biperiden"),
        new Med("Bisacodyl"),
        new Med("Bisoprolol"),
        new Med("Butylscopolamine"),
        new Med("Cabergoline"),
        new Med("Calcium Folinate", "Folinic Acid"),
        new Med("Carbamazepine"),
        new Med("Cefalexin"),
        new Med("Cefixime"),
        new Med("Chloramphenicol"),
        new Med("Chloroquine Phosphate"),
        new Med("Chloroquine Sulfate"),
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
        new Med("Ferrous Salts"),
        new Med("Ferrous Salts/Folic Acid"),
        new Med("Fluconazole"),
        new Med("Flucytosine"),
        new Med("Fluoxetine"),
        new Med("Folic Acid", "Vitamin B9"),
        new Med("Fosfomycin Trometamol"),
        new Med("Furosemide"),
        new Med("Glibenclamide"),
        new Med("Gliclazide"),
        new Med("Glyceryl Trinitrate", "Nitroglycerin", "Trinitrin"),
        new Med("Griseofulvin"),
        new Med("Haloperidol"),
        new Med("Hydrochlorothiazide"),
        new Med("Hydroxyzine"),
        new Med("Hyoscine Butylbromide", "Butylscopolamine"),
        new Med("Ibuprofen"),
        new Med("Iodized oil"),
        new Med("Ipratropium Bromide nebuliser solution"),
        new Med("Isoniazid", "H"),
        new Med("Isosorbide Dinitrate"),
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
        // ! new Med("Noramidopyrine"),
        new Med("Nystatin"),
        new Med("Olanzapine"),
        new Med("Omeprazole"),
        new Med("Oral Rehydration Salts", "ORS"),
        new Med("Paracetamol", "Acetaminophen"),
        new Med("Paroxetine"),
        new Med("Phenobarbital"),
        new Med("Phenoxymethylpenicillin", "Penicillin V"),
        new Med("Phenytoin"),
        new Med("Potassium Chloride Immediate-release"),
        new Med("Potassium Chloride Sustained-release"),
        new Med("Praziquantel"),
        new Med("Prednisolone"),
        new Med("Prednisone"),
        new Med("Promethazine"),
        new Med("Pyrantel"),
        new Med("Pyrazinamide", "Z"),
        new Med("Pyridoxine", "Vitamin B6"),
        new Med("Pyrimethamine"),
        new Med("Quinine"),
        new Med("Resomal", "Rehydration Solution for Malnutrition"),
        new Med("Retinol", "Vitamin A"),
        new Med("Rifampicin", "R"),
        new Med("Risperidone"),
        new Med("Ritonavir", "RTV"),
        // ! new Med("Salbutamol", "Albuterol"),
        new Med("Salbutamol aerosol", "Albuterol"),
        new Med("Salbutamol nebuliser solution", "Albuterol"),
        new Med("Sertraline"),
        new Med("Spironolactone"),
        new Med("Sulfadiazine"),
        new Med("Sulfadoxine/Pyrimethamine", "SP"),
        new Med("Sulfamethoxazole/Trimethoprim", "SMX/TMP"),
        new Med("Tenofovir disoproxil fumarate", "TDF"),
        new Med("Thiamine", "Vitamin B1"),
        new Med("Tinidazole"),
        new Med("Tramadol"),
        new Med("Tranexamic Acid"),
        new Med("Triclabendazole"),
        new Med("Trihexyphenidyl"),
        new Med("Ulipristal"),
        new Med("Valproic Acid", "Sodium Valproate"),
        new Med("Vitamin B6"),
        new Med("Zidovudine", "AZT", "ZDV"),
        new Med("Zidovudine/Lamivudine", "AZT/3TC"),
        new Med("Zidovudine/Lamivudine/Nevirapine", "AZT/3TC/NVP"),
        new Med("Zinc Sulfate"),

        // Injectable drugs
        new Med("Co-amoxiclav", "Amoxicillin/Clavulanic Acid"),
        new Med("Amphotericin B conventional"),
        new Med("Amphotericin B liposomal"),
        new Med("Ampicillin"),
        // ! new Med("Artemether"),
        new Med("Artesunate"),
        new Med("Atropine"),
        new Med("Benzathine Benzylpenicillin"),
        new Med("Benzylpenicillin", "Penicillin G"),
        new Med("Calcium Gluconate"),
        new Med("Cefotaxime"),
        new Med("Ceftriaxone"),
        new Med("Chloramphenicol"),
        // ! new Med("Long-Acting Oily Chloramphenicol"),
        new Med("Chlorpromazine"),
        new Med("Clindamycin"),
        new Med("Cloxacillin"),
        new Med("Dexamethasone"),
        new Med("Diazepam emulsion"),
        new Med("Diazepam solution"),
        new Med("Diclofenac"),
        new Med("Digoxin"),
        // ! new Med("Dipyrone"),
        new Med("Eflornithine"),
        new Med("Epinephrine", "EPN", "Adrenaline"),
        new Med("Etonogestrel subdermal implant"),
        new Med("Fluconazole"),
        new Med("Furosemide"),
        new Med("Gentamicin"),
        new Med("Glucose 50%", "Dextrose 50%"),
        new Med("Haloperidol"),
        new Med("Haloperidol Decanoate"),
        new Med("Heparin"),
        new Med("Hydralazine"),
        new Med("Hydrocortisone"),
        new Med("Hyoscine Butylbromide", "Butylscopolamine"),
        new Med("Short-acting Insulin"),
        new Med("Biphasic Insulin"),
        new Med("Intermediate-acting Insulin"),
        new Med("Long-acting Insulin"),
        new Med("Ketamine"),
        new Med("Labetalol"),
        new Med("Levonorgestrel subdermal implant"),
        new Med("Lidocaine", "Lignocaine"),
        new Med("Magnesium Sulfate", "MgSO4"),
        new Med("Medroxyprogesterone"),
        new Med("Melarsoprol"),
        // ! new Med("Metamizole"),
        new Med("Methylergometrine"),
        new Med("Metoclopramide"),
        new Med("Metronidazole"),
        new Med("Morphine"),
        new Med("Naloxone"),
        // ! new Med("Noramidopyrine"),
        new Med("Omeprazole"),
        new Med("Ondansetron"),
        new Med("Oxytocin"),
        new Med("Paracetamol", "Acetaminophen"),
        new Med("Penicillin G"),
        new Med("Pentamidine"),
        new Med("Phenobarbital"),
        new Med("Phytomenadione", "Vitamin K1"),
        new Med("Potassium Chloride 10%", "KCl 10%"),
        new Med("Promethazine"),
        new Med("Protamine"),
        new Med("Quinine"),
        new Med("Salbutamol", "Albuterol"),
        new Med("Sodium Bicarbonate 8.4%"),
        new Med("Spectinomycin"),
        new Med("Streptomycin", "S"),
        new Med("Suramin"),
        new Med("Thiamine", "Vitamin B1"),
        new Med("Tramadol"),
        new Med("Tranexamic Acid"),

        // Infusion fluids
        new Med("Glucose 5%", "Dextrose 5%"),
        new Med("Glucose 10%", "Dextrose 10%"),
        new Med("Modified Fluid Gelatin"),
        new Med("Polygeline"),
        new Med("Ringer Lactate"),
        new Med("Sodium Chloride 0.9%", "NaCl"),

        // Vaccines, immunoglobulins, and antisera
        new Med("Oral Cholera Vaccine O1+O139"),
        new Med("Diphtheria/Tetanus/Pertussis Vaccine", "DTP"),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B Vaccine"),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B/Hib Vaccine"),
        new Med("Hepatitis B Vaccine"),
        new Med("Japanese Encephalitis Vaccine"),
        new Med("Measles Vaccine"),
        new Med("Meningococcal A Conjugate Vaccine"),
        new Med("Meningococcal A+C Vaccine"),
        new Med("Meningococcal A+C+W135 Vaccine"),
        new Med("Human Papillomavirus Vaccine", "HPV"),
        new Med("Pneumococcal Conjugate Vaccine", "PCV"),
        new Med("Inactivated Poliomyelitis Vaccine", "IPV"),
        new Med("Oral Poliomyelitis Vaccine", "OPV"),
        new Med("Human Rabies Immunoglobulin", "HRIG"),
        new Med("Rabies Vaccine"),
        new Med("Oral Rotavirus Vaccine"),
        new Med("Human Tetanus Immunoglobulin", "HTIG"),
        new Med("Tetanus Vaccine", "TT"),
        new Med("Tetanus-Diphtheria Vaccine", "Td"),
        // ! new Med("Tetanus Antitoxin, Equine"),
        new Med("Tuberculosis Vaccine", "BCG Vaccine"),
        new Med("Typhoid Conjugate Vaccine", "TCV"),
        new Med("Yellow Fever Vaccine"),

        // Drugs for external use, antiseptics, and disinfectants
        new Med("Aciclovir eye ointment", "Acyclovir"),
        new Med("Alcohol-based hand rub"),
        new Med("Artesunate rectal"),
        new Med("Benzoic Acid/Salicylic Acid ointment", "Whitfield's ointment"),
        new Med("Benzyl Benzoate lotion"),
        new Med("Calamine lotion"),
        new Med("Chlorhexidine 5% solution"),
        new Med("Chlorhexidine 7.1% dermal gel"),
        new Med("Chlorhexidine 0.2% mouthwash"),
        new Med("Ciprofloxacin ear drops"),
        new Med("Clotrimazole vaginal tablet"),
        new Med("Dinoprostone vaginal gel"),
        new Med("Ethyl alcohol", "Ethanol"),
        new Med("Fluorescein", "Eye drops"),
        new Med("Hydrocortisone cream"),
        new Med("Malathion lotion"),
        // ! new Med("Methylrosanilinium Chloride", "Gentian Violet", "GV", "Crystal violet"),
        new Med("Miconazole cream"),
        new Med("Mupirocin ointment"),
        new Med("NaDCC"),
        new Med("Nystatin vaginal tablet"),
        new Med("Oxybuprocaine eye drops"),
        new Med("Permethrin 1% lotion"),
        new Med("Permethrin 5% lotion"),
        new Med("Pilocarpine eye drops"),
        new Med("Podophyllotoxin 0.5% solution"),
        new Med("Podophyllum resin solution"),
        new Med("Povidone Iodine aqueous solution", "Polyvidone iodine", "PVI"),
        new Med("Povidone Iodine scrub solution", "Polyvidone iodine", "PVI"),
        new Med("Silver Sulfadiazine cream"),
        new Med("Sodium Dichloroisocyanurate", "NaDCC"),
        new Med("Calcium Hypochlorite", "HTH"),
        new Med("Sodium Hypochlorite solution", "Bleach"),
        new Med("Chlorinated Lime powder"),
        new Med("Tetracycline eye ointment"),
        new Med("Zinc Oxide ointment"),

        // ==== Additional Ebola treatments and vaccines

        // Experimental Ebola treatments
        new Med("Amodiaquine"),
        new Med("Favipiravir"),
        new Med("GS-5734"),
        new Med("ZMapp"),

        // Experimental Ebola vaccines
        new Med("cAd3-EBOZ"),
        new Med("VSV-EBOV"),
        new Med("Ad5-EBOV"),
        new Med("Ad26-ZEBOV/MVA-BN"),
        new Med("rVSV-ZEBOV")
    );

    public static List<Med> deduplicate(Med... meds) {
        List<Med> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Med med : meds) {
            if (!names.contains(normalize(med.name))) {
                names.add(normalize(med.name));
                result.add(med);
            }
        }
        return result;
    }

    @Override public Collection<? extends Completion> suggestCompletions(CharSequence constraint) {
        String searchKey = normalize(" " + constraint);
        List<Med> results = new ArrayList<>();
        for (Med med : MEDS) {
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
