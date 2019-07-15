package org.projectbuendia.client.ui;

import android.support.annotation.NonNull;
import android.view.View;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completer;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completion;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MedCompleter implements Completer {

    // === Style conventions for this list ===
    //
    // First argument: Main name of the medication, consisting of a title-cased
    // part, followed optionally by a comma and a lower-cased part.
    //   - A good test for the title-cased part is that it should fill the blank
    //     in the sentence: "The active ingredient in this drug is ______."
    //   - The title-cased part is the complete chemical name and should be a
    //     meaningful noun phrase on its own (e.g. "Ascorbic Acid", not
    //     "Ascorbic acid"; "Ferrous Salts", not "Ferrous salts")
    //   - When medications are combined, join them with a slash (e.g.
    //     "Artesunate/Amodiaquine", not "Artesunate + Amodiaquine")
    //   - Hyphenate the prefix "Co-" in names of combination medicines
    //     (e.g. "Co-amoxiclav", not "Coamoxiclav")
    //   - Do not capitalize immediately after a hyphen (e.g. "Co-artemether",
    //     not "Co-Artemether")
    //   - The lower-cased part describes the concentration, forumlation, or
    //     application (e.g. "Diazepam, solution", not "Diazepam Solution";
    //     "Morphine, immediate-release", not "Morphine Immediate-Release")
    //   - The concentration comes first, immediately after the comma (e.g.
    //     "Glucose, 50%" not "Glucose 50%"; "Permethrin, 1% lotion", not
    //     "Permethrin Lotion 1%" or "Permethrin 1%, lotion")
    //
    // Second argument: Primary category of therapeutic action, in lowercase.
    //   - A good test for this part is that it should have a plural form.
    //   - Use a noun, not a verb phrase (e.g. "abortifacient", not "causes abortion")
    //   - Use a noun, not an adjective (e.g. "anticonvulsant", not "anticonvulsive")
    //   - Describe the drug, not the action (e.g. "laxative", not "laxation")
    //   - Describe the drug, not the disease (e.g. "antimalarial", not "malaria")
    //   - For multiple purposes, use a comma (e.g. "antiseptic, disinfectant")
    //   - If primarily for a specific disease, optionally append "for" and the
    //     disease name in parentheses (e.g. "insecticide (for scabies)")
    //
    // Third and subsequent arguments: Aliases (to be shown in parentheses).
    //   - These parts should follow the same format as the first argument.
    //
    public static List<Med> MEDS = deduplicate(

        // === MSF Essential Drugs, 2016 edition, by Sophie Pilon

        // Oral drugs
        new Med("Abacavir", "antiretroviral", "ABC"),
        new Med("Acetylsalicylic Acid", "analgesic, antipyretic", "Aspirin", "ASA"),
        new Med("Aciclovir", "antiviral", "Acyclovir"),
        new Med("Activated Charcoal", "adsorbent"),
        new Med("Albendazole", "anthelminthic"),
        new Med("Aluminium Hydroxide", "antacid"),
        new Med("Amitriptyline", "antidepressant"),
        new Med("Amlodipine", "antihypertensive"),
        // ! new Med("Amodiaquine", "AQ"),
        new Med("Amoxicillin", "antibacterial"),
        // ! new Med("Artesunate", "AS"),
        new Med("Artesunate/Amodiaquine", "antimalarial", "AS/AQ"),
        new Med("Artesunate/Sulfadoxine/Pyrimethamine", "antimalarial", "AS/SP"),
        new Med("Ascorbic Acid", "vitamin", "Vitamin C"),
        new Med("Atazanavir", "antiretroviral", "ATV"),
        new Med("Azithromycin", "antibacterial"),
        new Med("Beclometasone, aerosol", "corticosteroid"),
        new Med("Biperiden", "anticholinergic"),
        new Med("Bisacodyl", "laxative"),
        new Med("Bisoprolol", "beta blocker"),
        new Med("Cabergoline", "lactation inhibitor"),
        new Med("Calcium Folinate", "", "Folinic Acid"),
        new Med("Carbamazepine", "antiepileptic"),
        new Med("Cefalexin", "antibacterial"),
        new Med("Cefixime", "antibacterial"),
        new Med("Chloramphenicol", "antibacterial"),
        new Med("Chloroquine Phosphate", "antimalarial"),
        new Med("Chloroquine Sulfate", "antimalarial"),
        new Med("Chlorphenamine", "antihistamine", "Chlorpheniramine"),
        new Med("Chlorpromazine", "antipsychotic"),
        new Med("Cimetidine", "acid reducer"),
        new Med("Ciprofloxacin", "antibacterial"),
        new Med("Clindamycin", "antibacterial"),
        new Med("Clomipramine", "antidepressant"),
        new Med("Cloxacillin", "antibacterial"),
        new Med("Co-amoxiclav", "antibacterial", "Amoxicillin/Clavulanic Acid"),
        new Med("Co-artemether", "antimalarial", "Artemether/Lumefantrine"),
        new Med("Codeine", "opioid analgesic"),
        new Med("Colecalciferol", "supplement", "Vitamin D3"),
        new Med("Co-trimoxazole", "antibacterial", "Sulfamethoxazole/Trimethoprim", "SMX/TMP"),
        new Med("Dapsone", "antibacterial, antileprotic"),
        new Med("Darunavir", "antiretroviral", "DRV"),
        new Med("Desogestrel", "contraceptive"),
        new Med("Diazepam", "sedative, anticonvulsant"),
        new Med("Diethylcarbamazine", "anthelminthic"),
        new Med("Digoxin", "cardiotonic"),
        new Med("Dihydroartemisinin/Piperaquine", "antimalarial", "DHA/PPQ"),
        // ! new Med("Dipyrone"),
        new Med("Dolutegravir", "antiretroviral", "DTG"),
        new Med("Doxycycline", "antibacterial"),
        new Med("Efavirenz", "antiretroviral", "EFV", "EFZ"),
        new Med("Enalapril", "antihypertensive"),
        new Med("Ergocalciferol", "supplement", "Vitamin D2"),
        new Med("Erythromycin", "antibacterial"),
        new Med("Ethambutol", "antituberculous antibacterial",  "E"),
        new Med("Ethinylestradiol/Levonorgestrel", "contraceptive"),
        new Med("Ferrous Salts", "antianaemic"),
        new Med("Ferrous Salts/Folic Acid"),
        new Med("Fluconazole", "antifungal"),
        new Med("Flucytosine", "antifungal"),
        new Med("Fluoxetine", "antidepressant"),
        new Med("Folic Acid", "antianaemic", "Vitamin B9"),
        new Med("Fosfomycin Trometamol", "antibacterial"),
        new Med("Furosemide", "diuretic"),
        new Med("Glibenclamide", "antidiabetic"),
        new Med("Gliclazide", "antidiabetic"),
        new Med("Glyceryl Trinitrate", "antianginal", "Nitroglycerin", "Trinitrin"),
        new Med("Griseofulvin", "antifungal"),
        new Med("Haloperidol", "antipsychotic"),
        new Med("Hydrochlorothiazide", "diuretic"),
        new Med("Hydroxyzine", "antihistamine"),
        new Med("Hyoscine Butylbromide", "antispasmodic", "Butylscopolamine"),
        new Med("Ibuprofen", "analgesic, antipyretic"),
        new Med("Iodized Oil", "supplement"),
        new Med("Ipratropium Bromide, nebuliser solution", "bronchodilator"),
        new Med("Isoniazid", "antitubercular", "H"),
        new Med("Isosorbide Dinitrate", "antianginal"),
        new Med("Itraconazole", "antifungal"),
        new Med("Ivermectin", "anthelminthic"),
        new Med("Labetalol", "beta blocker"),
        new Med("Lactulose", "laxative"),
        new Med("Lamivudine", "anitretroviral", "3TC"),
        new Med("Levodopa/Carbidopa", "antiparkinsonian", "Co-careldopa"),
        new Med("Levonorgestrel", "contraceptive"),
        new Med("Loperamide", "antidiarrhoeal"),
        new Med("Lopinavir/Ritonavir", "antiretroviral", "LPV/R"),
        new Med("Loratadine", "antihistamine"),
        new Med("Mebendazole", "anthelminthic"),
        new Med("Mefloquine", "antimalarial", "MQ"),
        // ! new Med("Metamizole"),
        new Med("Metformin", "antidiabetic"),
        new Med("Methyldopa", "antihypertensive"),
        new Med("Metoclopramide", "antiemetic"),
        new Med("Metronidazole", "antibacterial, antiprotozoal"),
        new Med("Miconazole", "antifungal"),
        new Med("Mifepristone", "abortifacient", "RU-486"),
        new Med("Misoprostol", "oxytocic"),
        new Med("Morphine, immediate-release", "opioid analgesic", "MIR"),
        new Med("Morphine, sustained-release", "opioid analgesic", "MSR"),
        new Med("Multivitamins", "supplement", "Vitamin B complex"),
        new Med("Nevirapine", "antiretroviral", "NVP"),
        new Med("Niclosamide", "anthelminthic"),
        new Med("Nicotinamide", "supplement", "Vitamin PP", "Vitamin B3"),
        new Med("Nifedipine", "antihypertensive, antioxytocic"),
        new Med("Nitrofurantoin", "antibacterial"),
        // ! new Med("Noramidopyrine"),
        new Med("Nystatin", "antifungal"),
        new Med("Olanzapine", "antipsychotic"),
        new Med("Omeprazole", "acid reducer"),
        new Med("Oral Rehydration Salts", "fluid replacer", "ORS"),
        new Med("Paracetamol", "analgesic, antipyretic", "Acetaminophen"),
        new Med("Paroxetine", "antidepressant"),
        new Med("Phenobarbital", "sedative, anticonvulsant"),
        new Med("Phenoxymethylpenicillin", "antibacterial", "Penicillin V"),
        new Med("Phenytoin", "anticonvulsant"),
        new Med("Potassium Chloride, immediate-release", "supplement"),
        new Med("Potassium Chloride, sustained-release", "supplement"),
        new Med("Praziquantel", "anthelminthic"),
        new Med("Prednisolone", "corticosteroid"),
        new Med("Prednisone", "corticosteroid"),
        new Med("Promethazine", "antihistamine"),
        new Med("Pyrantel", "anthelminthic"),
        new Med("Pyrazinamide", "antitubercular", "Z"),
        new Med("Pyridoxine", "supplement", "Vitamin B6"),
        new Med("Pyrimethamine", "antiprotozoal"),
        new Med("Quinine", "antimalarial"),
        new Med("Resomal", "", "Rehydration Solution for Malnutrition"),
        new Med("Retinol", "supplement", "Vitamin A"),
        new Med("Rifampicin", "antitubercular", "R"),
        new Med("Risperidone", "antipsychotic"),
        new Med("Ritonavir", "antiretroviral", "RTV"),
        // ! new Med("Salbutamol", "Albuterol"),
        new Med("Salbutamol, aerosol", "bronchodilator", "Albuterol"),
        new Med("Salbutamol, nebuliser solution", "bronchodilator", "Albuterol"),
        new Med("Sertraline", "antidepressant"),
        new Med("Spironolactone", "diuretic"),
        new Med("Sulfadiazine", "antibacterial"),
        new Med("Sulfadoxine/Pyrimethamine", "antimalarial", "SP"),
        new Med("Tenofovir Disoproxil Fumarate", "antiretroviral", "TDF"),
        new Med("Thiamine", "supplement", "Vitamin B1"),
        new Med("Tinidazole", "antiprotozoal, antibacterial"),
        new Med("Tramadol", "opioid analgesic"),
        new Med("Tranexamic Acid", "antifibrinolytic"),
        new Med("Triclabendazole", "anthelminthic"),
        new Med("Trihexyphenidyl", "antiparkinsonian"),
        new Med("Ulipristal", "contraceptive"),
        new Med("Valproic Acid", "antiepileptic", "Sodium Valproate"),
        new Med("Vitamin B6", "supplement"),
        new Med("Zidovudine", "antiretroviral", "AZT", "ZDV"),
        new Med("Zidovudine/Lamivudine", "antiretroviral", "AZT/3TC"),
        new Med("Zidovudine/Lamivudine/Nevirapine", "antiretroviral", "AZT/3TC/NVP"),
        new Med("Zinc Sulfate", "supplement"),

        // Injectable drugs
        new Med("Amphotericin B, conventional", "antifungal"),
        new Med("Amphotericin B, liposomal", "antifungal"),
        new Med("Ampicillin", "antibacterial"),
        // ! new Med("Artemether"),
        new Med("Artesunate", "antimalarial"),
        new Med("Atropine", "antispasmodic"),
        new Med("Benzathine Benzylpenicillin", "antibacterial"),
        new Med("Benzylpenicillin", "antibacterial", "Penicillin G"),
        new Med("Calcium Gluconate", "supplement"),
        new Med("Cefotaxime", "antibacterial"),
        new Med("Ceftriaxone", "antibacterial"),
        new Med("Chloramphenicol", "antibacterial"),
        // ! new Med("Long-Acting Oily Chloramphenicol"),
        new Med("Chlorpromazine", "antipsychotic"),
        new Med("Clindamycin", "antibacterial"),
        new Med("Cloxacillin", "antibacterial"),
        new Med("Co-amoxiclav", "antibacterial", "Amoxicillin/Clavulanic Acid"),
        new Med("Dexamethasone", "corticosteroid"),
        new Med("Diazepam, emulsion", "sedative, anticonvulsant"),
        new Med("Diazepam, solution", "sedative, anticonvulsant"),
        new Med("Diclofenac", "analgesic, antipyretic"),
        new Med("Digoxin", "cardiotonic"),
        // ! new Med("Dipyrone"),
        new Med("Eflornithine", "antiprotozoal (for sleeping sickness)"),
        new Med("Epinephrine", "anti-anaphylactic", "EPN", "Adrenaline"),
        new Med("Etonogestrel, subdermal implant", "contraceptive"),
        new Med("Fluconazole", "antifungal"),
        new Med("Furosemide", "diuretic"),
        new Med("Gentamicin", "antibacterial"),
        new Med("Glucose, 50%", "", "Dextrose, 50%"),
        new Med("Haloperidol", "antipsychotic"),
        new Med("Haloperidol Decanoate", "antipsychotic"),
        new Med("Heparin", "anticoagulant"),
        new Med("Hydralazine", "antihypertensive"),
        new Med("Hydrocortisone", "corticosteroid"),
        new Med("Hyoscine Butylbromide", "antispasmodic", "Butylscopolamine"),
        new Med("Insulin, biphasic"),
        new Med("Insulin, intermediate-acting"),
        new Med("Insulin, long-acting"),
        new Med("Insulin, short-acting"),
        new Med("Ketamine", "anaesthetic"),
        new Med("Labetalol", "beta blocker"),
        new Med("Levonorgestrel, subdermal implant", "contraceptive"),
        new Med("Lidocaine", "anaesthetic", "Lignocaine"),
        new Med("Magnesium Sulfate", "anticonvulsant", "MgSO4"),
        new Med("Medroxyprogesterone", "contraceptive"),
        new Med("Melarsoprol", "antiprotozoal (for sleeping sickness)"),
        // ! new Med("Metamizole"),
        new Med("Methylergometrine", "oxytocic"),
        new Med("Metoclopramide", "antiemetic"),
        new Med("Metronidazole", "antiprotozoal, antibacterial"),
        new Med("Morphine", "opioid analgesic"),
        new Med("Naloxone", "opioid antagonist"),
        // ! new Med("Noramidopyrine"),
        new Med("Omeprazole", "acid reducer"),
        new Med("Ondansetron", "antiemetic"),
        new Med("Oxytocin", "oxytocic"),
        new Med("Paracetamol", "analgesic, antipyretic", "Acetaminophen"),
        new Med("Penicillin G", "antibacterial"),
        new Med("Pentamidine", "antiprotozoal"),
        new Med("Phenobarbital", "anticonvulsant"),
        new Med("Phytomenadione", "supplement", "Vitamin K1"),
        new Med("Potassium Chloride, 10%", "", "KCl, 10%"),
        new Med("Promethazine", "antihistamine, antiemetic"),
        new Med("Protamine", "heparin antidote"),
        new Med("Quinine", "antimalarial"),
        new Med("Salbutamol", "bronchodilator", "Albuterol"),
        new Med("Sodium Bicarbonate, 8.4%"),
        new Med("Spectinomycin", "antibacterial"),
        new Med("Streptomycin", "antibacterial", "S"),
        new Med("Suramin", "antiprotozoal (for sleeping sickness)"),
        new Med("Thiamine", "supplement", "Vitamin B1"),
        new Med("Tramadol", "opioid analgesic"),
        new Med("Tranexamic Acid", "antifibrinolytic"),

        // Infusion fluids
        new Med("Glucose, 5%", "", "Dextrose, 5%"),
        new Med("Glucose, 10%", "", "Dextrose, 10%"),
        new Med("Modified Fluid Gelatin", "plasma substitute"),
        new Med("Polygeline", "plasma substitute"),
        new Med("Ringer Lactate", "fluid replacer"),
        new Med("Sodium Chloride, 0.9%", "fluid replacer", "NaCl"),

        // Vaccines, immunoglobulins, and antisera
        new Med("Oral Cholera Vaccine O1+O139"),
        new Med("Diphtheria/Tetanus/Pertussis Vaccine", "vaccine", "DTP"),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B Vaccine", "vaccine"),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B/Hib Vaccine", "vaccine"),
        new Med("Hepatitis B Vaccine", "vaccine"),
        new Med("Japanese Encephalitis Vaccine", "vaccine"),
        new Med("Measles Vaccine", "vaccine"),
        new Med("Meningococcal A Conjugate Vaccine", "vaccine"),
        new Med("Meningococcal A+C Vaccine", "vaccine"),
        new Med("Meningococcal A+C+W135 Vaccine", "vaccine"),
        new Med("Human Papillomavirus Vaccine", "vaccine", "HPV"),
        new Med("Pneumococcal Conjugate Vaccine", "vaccine", "PCV"),
        new Med("Inactivated Poliomyelitis Vaccine", "vaccine", "IPV"),
        new Med("Oral Poliomyelitis Vaccine", "vaccine", "OPV"),
        new Med("Human Rabies Immunoglobulin", "", "HRIG"),
        new Med("Rabies Vaccine", "vaccine"),
        new Med("Oral Rotavirus Vaccine", "vaccine"),
        new Med("Human Tetanus Immunoglobulin", "", "HTIG"),
        new Med("Tetanus Vaccine", "TT", "vaccine"),
        new Med("Tetanus-Diphtheria Vaccine", "vaccine", "Td"),
        // ! new Med("Tetanus Antitoxin, Equine"),
        new Med("Tuberculosis Vaccine", "vaccine", "BCG Vaccine"),
        new Med("Typhoid Conjugate Vaccine", "vaccine", "TCV"),
        new Med("Yellow Fever Vaccine", "vaccine"),

        // Drugs for external use, antiseptics, and disinfectants
        new Med("Aciclovir, eye ointment", "antiviral", "Acyclovir"),
        new Med("Alcohol-Based Hand Rub", "antiseptic"),
        new Med("Artesunate, rectal", "antimalarial"),
        new Med("Benzoic Acid/Salicylic Acid ointment", "antifungal", "Whitfield's ointment"),
        new Med("Benzyl Benzoate, lotion", "insecticide (for scabies)"),
        new Med("Calamine, lotion", "antipruritic"),
        new Med("Chlorhexidine, 5% solution", "antiseptic"),
        new Med("Chlorhexidine, 7.1% dermal gel", "antiseptic"),
        new Med("Chlorhexidine, 0.2% mouthwash", "antiseptic"),
        new Med("Ciprofloxacin, ear drops", "antibacterial"),
        new Med("Clotrimazole, vaginal tablet", "antifungal"),
        new Med("Dinoprostone, vaginal gel", "oxytocic"),
        new Med("Ethyl Alcohol", "antiseptic, disinfectant", "Ethanol"),
        new Med("Fluorescein, eye drops", "diagnostic staining agent"),
        new Med("Hydrocortisone, cream", "corticosteroid"),
        new Med("Malathion, lotion", "insecticide (for lice)"),
        // ! new Med("Methylrosanilinium Chloride", "Gentian Violet", "GV", "Crystal violet"),
        new Med("Miconazole, cream", "antifungal"),
        new Med("Mupirocin, ointment", "antibacterial"),
        new Med("Nystatin, vaginal tablet", "antifungal"),
        new Med("Oxybuprocaine, eye drops", "anaesthetic"),
        new Med("Permethrin, 1% lotion", "insecticide (for lice)"),
        new Med("Permethrin, 5% lotion", "insecticide (for scabies)"),
        new Med("Pilocarpine, eye drops", "miotic"),
        new Med("Podophyllotoxin, 0.5% solution", "antiviral (for HPV)"),
        new Med("Podophyllum Resin, solution", "antiviral (for HPV)"),
        new Med("Povidone Iodine, aqueous solution", "antiseptic, disinfectant", "Polyvidone iodine", "PVI"),
        new Med("Povidone Iodine, scrub solution", "antiseptic, disinfectant", "Polyvidone iodine", "PVI"),
        new Med("Silver Sulfadiazine, cream", "antibacterial"),
        new Med("Sodium Dichloroisocyanurate", "disinfectant", "NaDCC"),
        new Med("Calcium Hypochlorite", "disinfectant", "HTH"),
        new Med("Sodium Hypochlorite, solution", "disinfectant", "Bleach"),
        new Med("Chlorinated Lime, powder", "disinfectant"),
        new Med("Tetracycline, eye ointment", "antibacterial"),
        new Med("Zinc Oxide, ointment", "skin protector"),

        // ==== Additional Ebola treatments and vaccines

        // Experimental Ebola treatments
        new Med("Amodiaquine", "experimental Ebola treatment"),
        new Med("Favipiravir", "experimental Ebola treatment"),
        new Med("GS-5734", "experimental Ebola treatment"),
        new Med("ZMapp", "experimental Ebola treatment"),

        // Experimental Ebola vaccines
        new Med("cAd3-EBOZ", "experimental Ebola vaccine"),
        new Med("VSV-EBOV", "experimental Ebola vaccine"),
        new Med("Ad5-EBOV", "experimental Ebola vaccine"),
        new Med("Ad26-ZEBOV/MVA-BN", "experimental Ebola vaccine"),
        new Med("rVSV-ZEBOV", "experimental Ebola vaccine")
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
        Collections.sort(result, new Comparator<Med>() {
            @Override public int compare(Med a, Med b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return result;
    }

    @Override public Collection<? extends Completion> suggestCompletions(CharSequence constraint) {
        String[] searchKeys = normalize(constraint).trim().split(" ");
        for (int i = 0; i < searchKeys.length; i++) {
            searchKeys[i] = " " + searchKeys[i];
        }

        List<Med> results = new ArrayList<>();
        for (Med med : MEDS) {
            // Look for words matching the words in the input as prefixes.
            int score = 0;
            for (String searchKey : searchKeys) {
                score += med.filterTarget.contains(searchKey) ? 1 : 0;
            }
            if (score == searchKeys.length) {
                results.add(med);
                continue;
            }

            if (searchKeys.length == 1) {
                // Look for words matching the letters in the input as initials.
                score = 0;
                char[] initials = searchKeys[0].trim().toCharArray();
                for (char ch : initials) {
                    score += med.filterTarget.contains(" " + ch) ? 1 : 0;
                }
                if (score == initials.length) {
                    results.add(med);
                }
            }
        }
        return results;
    }

    private static String normalize(CharSequence name) {
        return name.toString().toLowerCase().replaceAll("[^a-z0-9]+", " ");
    }

    public static class Med implements Completion {
        String name;
        String caption;
        String[] aliases;
        String label;
        String filterTarget;

        public Med(String name) {
            this(name, "");
        }

        public Med(String name, String caption, String... aliases) {
            this.name = name;
            this.caption = caption;
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

        public void showInView(View itemView) {
            Utils.setText(itemView, R.id.label, label);
            Utils.setText(itemView, R.id.caption, caption);
        }

        public @NonNull String getValue() {
            return name;
        }
    }
}
