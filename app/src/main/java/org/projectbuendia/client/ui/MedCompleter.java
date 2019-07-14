package org.projectbuendia.client.ui;

import android.support.annotation.NonNull;

import org.projectbuendia.client.ui.AutocompleteAdapter.Completer;
import org.projectbuendia.client.ui.AutocompleteAdapter.Completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MedCompleter implements Completer {
    public static List<Med> MEDS = deduplicate(
        // ==== MSF Essential Drugs, 2016 edition, by Sophie Pilon

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
        new Med("Atazanavir", "?", "ATV"),
        new Med("Azithromycin", "antibacterial"),
        new Med("Beclometasone aerosol", "corticosteroid"),
        new Med("Biperiden", "anticholinergic"),
        new Med("Bisacodyl", "laxative"),
        new Med("Bisoprolol", "beta-blocker"),
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
        new Med("Cimetidine", "antiulcer"),
        new Med("Ciprofloxacin", "antibacterial"),
        new Med("Clindamycin", "antibacterial"),
        new Med("Clomipramine", "antidepressant"),
        new Med("Cloxacillin", "antibacterial"),
        new Med("Co-amoxiclav", "antibacterial", "Amoxicillin/Clavulanic acid"),
        new Med("Co-artemether", "antimalarial", "Artemether/Lumefantrine"),
        new Med("Codeine", "analgesic"),
        new Med("Colecalciferol", "supplement", "Vitamin D3"),
        new Med("Co-trimoxazole", "antibacterial", "Sulfamethoxazole/Trimethoprim", "SMX/TMP"),
        new Med("Dapsone", "antibacterial, antileprotic"),
        new Med("Darunavir", "?", "DRV"),
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
        new Med("Ferrous Salts", "antianaemia"),
        new Med("Ferrous Salts/Folic Acid"),
        new Med("Fluconazole", "antifungal"),
        new Med("Flucytosine", "antifungal"),
        new Med("Fluoxetine", "antidepressant"),
        new Med("Folic Acid", "antianaemia", "Vitamin B9"),
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
        new Med("Iodized oil", "supplement"),
        new Med("Ipratropium Bromide nebuliser solution", "bronchodilator"),
        new Med("Isoniazid", "antituberculous antibacterial", "H"),
        new Med("Isosorbide Dinitrate", "antianginal"),
        new Med("Itraconazole", "antifungal"),
        new Med("Ivermectin", "anthelminthic"),
        new Med("Labetalol", "beta-blocker"),
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
        new Med("Misoprostol", "abortifacient"),
        new Med("Morphine immediate-release", "analgesic", "MIR"),
        new Med("Morphine sustained-release", "analgesic", "MSR"),
        new Med("Multivitamins", "supplement", "Vitamin B complex"),
        new Med("Nevirapine", "antiretroviral", "NVP"),
        new Med("Niclosamide", "anthelminthic"),
        new Med("Nicotinamide", "supplement", "Vitamin PP", "Vitamin B3"),
        new Med("Nifedipine", "antihypertensive"),
        new Med("Nitrofurantoin", "antibacterial"),
        // ! new Med("Noramidopyrine"),
        new Med("Nystatin", "antifungal"),
        new Med("Olanzapine", "antipsychotic"),
        new Med("Omeprazole", "antiulcer"),
        new Med("Oral Rehydration Salts", "fluid replacer", "ORS"),
        new Med("Paracetamol", "analgesic, antipyretic", "Acetaminophen"),
        new Med("Paroxetine", "antidepressant"),
        new Med("Phenobarbital", "sedative, anticonvulsant"),
        new Med("Phenoxymethylpenicillin", "antibacterial", "Penicillin V"),
        new Med("Phenytoin", "anticonvulsant"),
        new Med("Potassium Chloride Immediate-release", "supplement"),
        new Med("Potassium Chloride Sustained-release", "supplement"),
        new Med("Praziquantel", "anthelminthic"),
        new Med("Prednisolone", "corticosteroid"),
        new Med("Prednisone", "corticosteroid"),
        new Med("Promethazine", "antihistamine"),
        new Med("Pyrantel", "anthelminthic"),
        new Med("Pyrazinamide", "antituberculous antibacterial", "Z"),
        new Med("Pyridoxine", "supplement", "Vitamin B6"),
        new Med("Pyrimethamine", "antiprotozoal"),
        new Med("Quinine", "antimalarial"),
        new Med("Resomal", "", "Rehydration Solution for Malnutrition"),
        new Med("Retinol", "supplement", "Vitamin A"),
        new Med("Rifampicin", "antituberculous antibacterial", "R"),
        new Med("Risperidone", "antipsychotic"),
        new Med("Ritonavir", "antiretroviral", "RTV"),
        // ! new Med("Salbutamol", "Albuterol"),
        new Med("Salbutamol aerosol", "bronchodilator", "Albuterol"),
        new Med("Salbutamol nebuliser solution", "bronchodilator", "Albuterol"),
        new Med("Sertraline", "antidepressant"),
        new Med("Spironolactone", "diuretic"),
        new Med("Sulfadiazine", "antibacterial"),
        new Med("Sulfadoxine/Pyrimethamine", "antimalarial", "SP"),
        new Med("Tenofovir disoproxil fumarate", "antiretroviral", "TDF"),
        new Med("Thiamine", "supplement", "Vitamin B1"),
        new Med("Tinidazole", "antiprotozoal, antibacterial"),
        new Med("Tramadol", "analgesic"),
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
        new Med("Co-amoxiclav", "antibacterial", "Amoxicillin/Clavulanic Acid"),
        new Med("Amphotericin B conventional", "antifungal"),
        new Med("Amphotericin B liposomal", "antifungal"),
        new Med("Ampicillin", "antibacterial"),
        // ! new Med("Artemether"),
        new Med("Artesunate", "antimalarial"),
        new Med("Atropine", "antispasmodic"),
        new Med("Benzathine Benzylpenicillin", "antibacterial"),
        new Med("Benzylpenicillin", "antibacterial", "Penicillin G"),
        new Med("Calcium Gluconate"),
        new Med("Cefotaxime", "antibacterial"),
        new Med("Ceftriaxone", "antibacterial"),
        new Med("Chloramphenicol", "antibacterial"),
        // ! new Med("Long-Acting Oily Chloramphenicol"),
        new Med("Chlorpromazine", "antipsychotic"),
        new Med("Clindamycin", "antibacterial"),
        new Med("Cloxacillin", "antibacterial"),
        new Med("Dexamethasone", "corticosteroid"),
        new Med("Diazepam emulsion", "sedative, anticonvulsant"),
        new Med("Diazepam solution", "sedative, anticonvulsant"),
        new Med("Diclofenac", "analgesic, antipyretic"),
        new Med("Digoxin", "cardiotonic"),
        // ! new Med("Dipyrone"),
        new Med("Eflornithine", "trypanocide"),
        new Med("Epinephrine", "sympathomimetic", "EPN", "Adrenaline"),
        new Med("Etonogestrel subdermal implant"),
        new Med("Fluconazole"),
        new Med("Furosemide"),
        new Med("Gentamicin"),
        new Med("Glucose 50%", "", "Dextrose 50%"),
        new Med("Haloperidol", "antipsychotic"),
        new Med("Haloperidol Decanoate", "antipsychotic"),
        new Med("Heparin", "anticoagulant"),
        new Med("Hydralazine"),
        new Med("Hydrocortisone", "corticosteroid"),
        new Med("Hyoscine Butylbromide", "antispasmodic", "Butylscopolamine"),
        new Med("Short-acting Insulin"),
        new Med("Biphasic Insulin"),
        new Med("Intermediate-acting Insulin"),
        new Med("Long-acting Insulin"),
        new Med("Ketamine"),
        new Med("Labetalol"),
        new Med("Levonorgestrel subdermal implant"),
        new Med("Lidocaine", "anaesthetic", "Lignocaine"),
        new Med("Magnesium Sulfate", "anticonvulsant", "MgSO4"),
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
        new Med("Paracetamol", "analgesic, antipyretic", "Acetaminophen"),
        new Med("Penicillin G"),
        new Med("Pentamidine"),
        new Med("Phenobarbital"),
        new Med("Phytomenadione", "supplement", "Vitamin K1"),
        new Med("Potassium Chloride 10%", "", "KCl 10%"),
        new Med("Promethazine"),
        new Med("Protamine"),
        new Med("Quinine"),
        new Med("Salbutamol", "bronchodilator", "Albuterol"),
        new Med("Sodium Bicarbonate 8.4%"),
        new Med("Spectinomycin", "antibacterial"),
        new Med("Streptomycin", "antibacterial", "S"),
        new Med("Suramin"),
        new Med("Thiamine", "supplement", "Vitamin B1"),
        new Med("Tramadol"),
        new Med("Tranexamic Acid"),

        // Infusion fluids
        new Med("Glucose 5%", "", "Dextrose 5%"),
        new Med("Glucose 10%", "", "Dextrose 10%"),
        new Med("Modified Fluid Gelatin", "plasma substitute"),
        new Med("Polygeline", "plasma substitute"),
        new Med("Ringer Lactate", "fluid replacer"),
        new Med("Sodium Chloride 0.9%", "fluid replacer", "NaCl"),

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
        new Med("Human Rabies Immunoglobulin", "antirabies", "HRIG"),
        new Med("Rabies Vaccine", "vaccine"),
        new Med("Oral Rotavirus Vaccine", "vaccine"),
        new Med("Human Tetanus Immunoglobulin", "antitetanus", "HTIG"),
        new Med("Tetanus Vaccine", "TT", "vaccine"),
        new Med("Tetanus-Diphtheria Vaccine", "vaccine", "Td"),
        // ! new Med("Tetanus Antitoxin, Equine"),
        new Med("Tuberculosis Vaccine", "vaccine", "BCG Vaccine"),
        new Med("Typhoid Conjugate Vaccine", "vaccine", "TCV"),
        new Med("Yellow Fever Vaccine", "vaccine"),

        // Drugs for external use, antiseptics, and disinfectants
        new Med("Aciclovir eye ointment", "antiviral", "Acyclovir"),
        new Med("Alcohol-based hand rub", "antiseptic"),
        new Med("Artesunate rectal", "antimalarial"),
        new Med("Benzoic Acid/Salicylic Acid ointment", "antifungal", "Whitfield's ointment"),
        new Med("Benzyl Benzoate lotion", "scabicide"),
        new Med("Calamine lotion", "antipruritic"),
        new Med("Chlorhexidine 5% solution", "antiseptic"),
        new Med("Chlorhexidine 7.1% dermal gel", "antiseptic"),
        new Med("Chlorhexidine 0.2% mouthwash", "antiseptic"),
        new Med("Ciprofloxacin ear drops", "antibacterial"),
        new Med("Clotrimazole vaginal tablet", "antifungal"),
        new Med("Dinoprostone vaginal gel", "labour inducer"),
        new Med("Ethyl alcohol", "antiseptic, disinfectant", "Ethanol"),
        new Med("Fluorescein eye drops", "diagnostic staining agent"),
        new Med("Hydrocortisone cream", "corticosteroid"),
        new Med("Malathion lotion", "insecticide for lice"),
        // ! new Med("Methylrosanilinium Chloride", "Gentian Violet", "GV", "Crystal violet"),
        new Med("Miconazole cream", "antifungal"),
        new Med("Mupirocin ointment", "antibacterial"),
        new Med("Nystatin vaginal tablet", "antifungal"),
        new Med("Oxybuprocaine eye drops", "anaesthetic"),
        new Med("Permethrin 1% lotion", "insecticide for lice"),
        new Med("Permethrin 5% lotion", "insecticide for scabies"),
        new Med("Pilocarpine eye drops", "anti-glaucoma agent"),
        new Med("Podophyllotoxin 0.5% solution", "antiviral for HPV"),
        new Med("Podophyllum resin solution", "antiviral for HPV"),
        new Med("Povidone Iodine aqueous solution", "antiseptic, disinfectant", "Polyvidone iodine", "PVI"),
        new Med("Povidone Iodine scrub solution", "antiseptic, disinfectant", "Polyvidone iodine", "PVI"),
        new Med("Silver Sulfadiazine cream", "antibacterial"),
        new Med("Sodium Dichloroisocyanurate", "disinfectant", "NaDCC"),
        new Med("Calcium Hypochlorite", "disinfectant", "HTH"),
        new Med("Sodium Hypochlorite solution", "disinfectant", "Bleach"),
        new Med("Chlorinated Lime powder", "disinfectant"),
        new Med("Tetracycline eye ointment", "antibacterial"),
        new Med("Zinc Oxide ointment", "skin protector"),

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

        public @NonNull String getLabel() {
            return label;
        }

        public @NonNull String getValue() {
            return name;
        }

        public @NonNull String getCaption() { return caption; }
    }
}
