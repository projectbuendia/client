package org.projectbuendia.client.ui;

import android.support.annotation.NonNull;
import android.view.View;

import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MedCompleter {

    // ==== Style conventions for captions ====
    //
    // A caption describes a primary category of therapeutic action, in lowercase.
    //   - A good test for this part is that it should have a plural form.
    //   - Use a noun, not a verb phrase (e.g. "abortifacient", not "causes abortion")
    //   - Use a noun, not an adjective (e.g. "anticonvulsant", not "anticonvulsive")
    //   - Describe the drug, not the action (e.g. "laxative", not "laxation")
    //   - Describe the drug, not the disease (e.g. "antimalarial", not "malaria")
    //   - For multiple purposes, use a comma (e.g. "antiseptic, disinfectant")
    //   - If primarily for a specific disease, optionally append "for" and the
    //     disease name in parentheses (e.g. "insecticide (for scabies)")

    public static final Loc ABORTIFACIENT = new Loc("abortifacient [fr:abortif]");
    public static final Loc ADSORBENT = new Loc("adsorbent [fr:adsorbant]");
    public static final Loc ANAESTHETIC = new Loc("anaesthetic [fr:anesthésique]");
    public static final Loc ANALGESIC = new Loc("analgesic [fr:analgésique]");
    public static final Loc ANTACID = new Loc("antacid [fr:antiacide]");
    public static final Loc ANTHELMINTHIC = new Loc("anthelminthic [fr:anthelminthique]");
    public static final Loc ANTIANAEMIC = new Loc("antianaemic [fr:anti-anémique]");
    public static final Loc ANTIANAPHYLACTIC = new Loc("antianaphylactic [fr:anti-anaphylactique]");
    public static final Loc ANTIANGINAL = new Loc("antianginal [fr:anti-angineux]");
    public static final Loc ANTIBACTERIAL = new Loc("antibacterial [fr:antibactérien]");
    public static final Loc ANTICHOLINERGIC = new Loc("anticholinergic [fr:anticholinergique]");
    public static final Loc ANTICOAGULANT = new Loc("anticoagulant [fr:anticoagulant]");
    public static final Loc ANTICONVULSANT = new Loc("anticonvulsant [fr:anticonvulsivant]");
    public static final Loc ANTIDEPRESSANT = new Loc("antidepressant [fr:antidépresseur]");
    public static final Loc ANTIDIABETIC = new Loc("antidiabetic [fr:antidiabétique]");
    public static final Loc ANTIDIARRHOEAL = new Loc("antidiarrhoeal [fr:antidiarrhéique]");
    public static final Loc ANTIEMETIC = new Loc("antiemetic [fr:antiémétique]");
    public static final Loc ANTIEPILEPTIC = new Loc("antiepileptic [fr:antiépileptique]");
    public static final Loc ANTIFIBRINOLYTIC = new Loc("antifibrinolytic [fr:antifibrinolytique]");
    public static final Loc ANTIFUNGAL = new Loc("antifungal [fr:antifongique]");
    public static final Loc ANTIHISTAMINE = new Loc("antihistamine [fr:antihistaminique]");
    public static final Loc ANTIHYPERTENSIVE = new Loc("antihypertensive [fr:antihypertenseur]");
    public static final Loc ANTILEPROTIC = new Loc("antileprotic [fr:antileprotique]");
    public static final Loc ANTIMALARIAL = new Loc("antimalarial [fr:antipaludique]");
    public static final Loc ANTIOXYTOCIC = new Loc("antioxytocic [fr:antioxytocique]");
    public static final Loc ANTIPARKINSONIAN = new Loc("antiparkinsonian [fr:antiparkinsonien]");
    public static final Loc ANTIPROTOZOAL = new Loc("antiprotozoal [fr:antiprotozoaire]");
    public static final Loc ANTIPROTOZOAL_FOR_SLEEPING_SICKNESS = new Loc("antiprotozoal (for sleeping sickness) [fr:antiprotozoaire (pour la maladie du sommeil)]");
    public static final Loc ANTIPRURITIC = new Loc("antipruritic [fr:antiprurigineux]");
    public static final Loc ANTIPSYCHOTIC = new Loc("antipsychotic [fr:antipsychotique]");
    public static final Loc ANTIPYRETIC = new Loc("antipyretic [fr:antipyrétique]");
    public static final Loc ANTIRETROVIRAL = new Loc("antiretroviral [fr:antirétroviral]");
    public static final Loc ANTISEPTIC = new Loc("antiseptic [fr:antiseptique]");
    public static final Loc ANTISPASMODIC = new Loc("antispasmodic [fr:antispasmodique]");
    public static final Loc ANTITUBERCULAR = new Loc("antitubercular [fr:antituberculeux]");
    public static final Loc ANTIVIRAL = new Loc("antiviral [fr:antiviral]");
    public static final Loc ANTIVIRAL_FOR_HPV = new Loc("antiviral (for HPV) [fr:antiviral (pour le VPH)]");
    public static final Loc BETA_BLOCKER = new Loc("beta blocker [fr:bêta-bloquant]");
    public static final Loc BRONCHODILATOR = new Loc("bronchodilator [fr:bronchodilatateur]");
    public static final Loc CARDIOTONIC = new Loc("cardiotonic [fr:cardiotonique]");
    public static final Loc CONTRACEPTIVE = new Loc("contraceptive [fr:contraceptif]");
    public static final Loc CORTICOSTEROID = new Loc("corticosteroid [fr:corticostéroïde]");
    public static final Loc DIAGNOSTIC_STAINING_AGENT = new Loc("diagnostic staining agent [fr:agent de coloration diagnostique]");
    public static final Loc DISINFECTANT = new Loc("disinfectant [fr:désinfectant]");
    public static final Loc DIURETIC = new Loc("diuretic [fr:diurétique]");
    public static final Loc EXPERIMENTAL_EBOLA_TREATMENT = new Loc("experimental Ebola treatment [fr:traitement expérimental contre Ebola]");
    public static final Loc EXPERIMENTAL_EBOLA_VACCINE = new Loc("experimental Ebola vaccine [fr:vaccin expérimental contre Ebola]");
    public static final Loc FLUID_REPLACER = new Loc("fluid replacer [fr:substitut fluide]");
    public static final Loc HEPARIN_ANTIDOTE = new Loc("heparin antidote [fr:antidote à l'héparine]");
    public static final Loc INSECTICIDE = new Loc("insecticide [fr:insecticide]");
    public static final Loc INSECTICIDE_FOR_LICE = new Loc("insecticide (for lice) [fr:insecticide (pour les poux)]");
    public static final Loc INSECTICIDE_FOR_SCABIES = new Loc("insecticide (for scabies) [fr:insecticide (pour la gale)]");
    public static final Loc LACTATION_INHIBITOR = new Loc("lactation inhibitor [fr:inhibiteur de la lactation]");
    public static final Loc LAXATIVE = new Loc("laxative [fr:laxatif]");
    public static final Loc MIOTIC = new Loc("miotic [fr:miotique]");
    public static final Loc OPIOID_ANALGESIC = new Loc("opioid analgesic [fr:analgésique opioïde]");
    public static final Loc OPIOID_ANTAGONIST = new Loc("opioid antagonist [fr:antagoniste des opioïdes]");
    public static final Loc OXYTOCIC = new Loc("oxytocic [fr:ocytocique]");
    public static final Loc PLASMA_SUBSTITUTE = new Loc("plasma substitute [fr:substitut de plasma]");
    public static final Loc SEDATIVE = new Loc("sedative [fr:sedatif]");
    public static final Loc SKIN_PROTECTOR = new Loc("skin protector [fr:protecteur de la peau]");
    public static final Loc SUPPLEMENT = new Loc("supplement [fr:supplément]");
    public static final Loc VACCINE = new Loc("vaccine [fr:vaccin]");


    // === Style conventions for medication names ====
    //
    // The medication name consists of a title-cased part followed optionally
    // by a comma and a lower-cased part.
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

    public static List<Med> MEDS = deduplicate(

        // === MSF Essential Drugs, 2016 edition, by Sophie Pilon

        new Med("Abacavir", "ABC").caption(ANTIRETROVIRAL),
        // Oral drugs
        new Med("Abacavir", "ABC").caption(ANTIRETROVIRAL),
        new Med("Acetylsalicylic Acid", "Aspirin", "ASA").caption(ANALGESIC, ANTIPYRETIC),
        new Med("Aciclovir", "Acyclovir").caption(ANTIVIRAL),
        new Med("Activated Charcoal").caption(ADSORBENT),
        new Med("Albendazole").caption(ANTHELMINTHIC),
        new Med("Aluminium Hydroxide").caption(ANTACID),
        new Med("Amitriptyline").caption(ANTIDEPRESSANT),
        new Med("Amlodipine").caption(ANTIHYPERTENSIVE),
        // ! new Med("Amodiaquine").caption(AQ),
        new Med("Amoxicillin").caption(ANTIBACTERIAL),
        // ! new Med("Artesunate").caption(AS),
        new Med("Artesunate/Amodiaquine", "AS/AQ").caption(ANTIMALARIAL),
        new Med("Artesunate/Sulfadoxine/Pyrimethamine", "AS/SP").caption(ANTIMALARIAL),
        new Med("Ascorbic Acid", "Vitamin C").caption(SUPPLEMENT),
        new Med("Atazanavir", "ATV").caption(ANTIRETROVIRAL),
        new Med("Azithromycin").caption(ANTIBACTERIAL),
        new Med("Beclometasone, aerosol").caption(CORTICOSTEROID),
        new Med("Biperiden").caption(ANTICHOLINERGIC),
        new Med("Bisacodyl").caption(LAXATIVE),
        new Med("Bisoprolol").caption(BETA_BLOCKER),
        new Med("Cabergoline").caption(LACTATION_INHIBITOR),
        new Med("Calcium Folinate", "Folinic Acid").caption(),
        new Med("Carbamazepine").caption(ANTIEPILEPTIC),
        new Med("Cefalexin").caption(ANTIBACTERIAL),
        new Med("Cefixime").caption(ANTIBACTERIAL),
        new Med("Chloramphenicol").caption(ANTIBACTERIAL),
        new Med("Chloroquine Phosphate").caption(ANTIMALARIAL),
        new Med("Chloroquine Sulfate").caption(ANTIMALARIAL),
        new Med("Chlorphenamine", "Chlorpheniramine").caption(ANTIHISTAMINE),
        new Med("Chlorpromazine").caption(ANTIPSYCHOTIC),
        new Med("Cimetidine").caption(ANTACID),
        new Med("Ciprofloxacin").caption(ANTIBACTERIAL),
        new Med("Clindamycin").caption(ANTIBACTERIAL),
        new Med("Clomipramine").caption(ANTIDEPRESSANT),
        new Med("Cloxacillin").caption(ANTIBACTERIAL),
        new Med("Co-amoxiclav", "Amoxicillin/Clavulanic Acid").caption(ANTIBACTERIAL),
        new Med("Co-artemether", "Artemether/Lumefantrine").caption(ANTIMALARIAL),
        new Med("Codeine").caption(OPIOID_ANALGESIC),
        new Med("Colecalciferol", "Vitamin D3").caption(SUPPLEMENT),
        new Med("Co-trimoxazole", "Sulfamethoxazole/Trimethoprim", "SMX/TMP").caption(ANTIBACTERIAL),
        new Med("Dapsone").caption(ANTIBACTERIAL, ANTILEPROTIC),
        new Med("Darunavir", "DRV").caption(ANTIRETROVIRAL),
        new Med("Desogestrel").caption(CONTRACEPTIVE),
        new Med("Diazepam").caption(SEDATIVE, ANTICONVULSANT),
        new Med("Diethylcarbamazine").caption(ANTHELMINTHIC),
        new Med("Digoxin").caption(CARDIOTONIC),
        new Med("Dihydroartemisinin/Piperaquine", "DHA/PPQ").caption(ANTIMALARIAL),
        // ! new Med("Dipyrone"),
        new Med("Dolutegravir", "DTG").caption(ANTIRETROVIRAL),
        new Med("Doxycycline").caption(ANTIBACTERIAL),
        new Med("Efavirenz", "EFV", "EFZ").caption(ANTIRETROVIRAL),
        new Med("Enalapril").caption(ANTIHYPERTENSIVE),
        new Med("Ergocalciferol", "Vitamin D2").caption(SUPPLEMENT),
        new Med("Erythromycin").caption(ANTIBACTERIAL),
        new Med("Ethambutol",  "E").caption(ANTITUBERCULAR, ANTIBACTERIAL),
        new Med("Ethinylestradiol/Levonorgestrel").caption(CONTRACEPTIVE),
        new Med("Ferrous Salts").caption(ANTIANAEMIC),
        new Med("Ferrous Salts/Folic Acid"),
        new Med("Fluconazole").caption(ANTIFUNGAL),
        new Med("Flucytosine").caption(ANTIFUNGAL),
        new Med("Fluoxetine").caption(ANTIDEPRESSANT),
        new Med("Folic Acid", "Vitamin B9").caption(ANTIANAEMIC),
        new Med("Fosfomycin Trometamol").caption(ANTIBACTERIAL),
        new Med("Furosemide").caption(DIURETIC),
        new Med("Glibenclamide").caption(ANTIDIABETIC),
        new Med("Gliclazide").caption(ANTIDIABETIC),
        new Med("Glyceryl Trinitrate", "Nitroglycerin", "Trinitrin").caption(ANTIANGINAL),
        new Med("Griseofulvin").caption(ANTIFUNGAL),
        new Med("Haloperidol").caption(ANTIPSYCHOTIC),
        new Med("Hydrochlorothiazide").caption(DIURETIC),
        new Med("Hydroxyzine").caption(ANTIHISTAMINE),
        new Med("Hyoscine Butylbromide", "Butylscopolamine").caption(ANTISPASMODIC),
        new Med("Ibuprofen").caption(ANALGESIC, ANTIPYRETIC),
        new Med("Iodized Oil").caption(SUPPLEMENT),
        new Med("Ipratropium Bromide, nebuliser solution").caption(BRONCHODILATOR),
        new Med("Isoniazid", "H").caption(ANTITUBERCULAR),
        new Med("Isosorbide Dinitrate").caption(ANTIANGINAL),
        new Med("Itraconazole").caption(ANTIFUNGAL),
        new Med("Ivermectin").caption(ANTHELMINTHIC),
        new Med("Labetalol").caption(BETA_BLOCKER),
        new Med("Lactulose").caption(LAXATIVE),
        new Med("Lamivudine", "3TC").caption(ANTIRETROVIRAL),
        new Med("Levodopa/Carbidopa", "Co-careldopa").caption(ANTIPARKINSONIAN),
        new Med("Levonorgestrel").caption(CONTRACEPTIVE),
        new Med("Loperamide").caption(ANTIDIARRHOEAL),
        new Med("Lopinavir/Ritonavir", "LPV/R").caption(ANTIRETROVIRAL),
        new Med("Loratadine").caption(ANTIHISTAMINE),
        new Med("Mebendazole").caption(ANTHELMINTHIC),
        new Med("Mefloquine", "MQ").caption(ANTIMALARIAL),
        // ! new Med("Metamizole"),
        new Med("Metformin").caption(ANTIDIABETIC),
        new Med("Methyldopa").caption(ANTIHYPERTENSIVE),
        new Med("Metoclopramide").caption(ANTIEMETIC),
        new Med("Metronidazole").caption(ANTIBACTERIAL, ANTIPROTOZOAL),
        new Med("Miconazole").caption(ANTIFUNGAL),
        new Med("Mifepristone", "RU-486").caption(ABORTIFACIENT),
        new Med("Misoprostol").caption(OXYTOCIC),
        new Med("Morphine, immediate-release", "MIR").caption(OPIOID_ANALGESIC),
        new Med("Morphine, sustained-release", "MSR").caption(OPIOID_ANALGESIC),
        new Med("Multivitamins", "Vitamin B complex").caption(SUPPLEMENT),
        new Med("Nevirapine", "NVP").caption(ANTIRETROVIRAL),
        new Med("Niclosamide").caption(ANTHELMINTHIC),
        new Med("Nicotinamide", "Vitamin PP", "Vitamin B3").caption(SUPPLEMENT),
        new Med("Nifedipine").caption(ANTIHYPERTENSIVE, ANTIOXYTOCIC),
        new Med("Nitrofurantoin").caption(ANTIBACTERIAL),
        // ! new Med("Noramidopyrine"),
        new Med("Nystatin").caption(ANTIFUNGAL),
        new Med("Olanzapine").caption(ANTIPSYCHOTIC),
        new Med("Omeprazole").caption(ANTACID),
        new Med("Oral Rehydration Salts", "ORS").caption(FLUID_REPLACER),
        new Med("Paracetamol", "Acetaminophen").caption(ANALGESIC, ANTIPYRETIC),
        new Med("Paroxetine").caption(ANTIDEPRESSANT),
        new Med("Phenobarbital").caption(SEDATIVE, ANTICONVULSANT),
        new Med("Phenoxymethylpenicillin", "Penicillin V").caption(ANTIBACTERIAL),
        new Med("Phenytoin").caption(ANTICONVULSANT),
        new Med("Potassium Chloride, immediate-release").caption(SUPPLEMENT),
        new Med("Potassium Chloride, sustained-release").caption(SUPPLEMENT),
        new Med("Praziquantel").caption(ANTHELMINTHIC),
        new Med("Prednisolone").caption(CORTICOSTEROID),
        new Med("Prednisone").caption(CORTICOSTEROID),
        new Med("Promethazine").caption(ANTIHISTAMINE),
        new Med("Pyrantel").caption(ANTHELMINTHIC),
        new Med("Pyrazinamide", "Z").caption(ANTITUBERCULAR),
        new Med("Pyridoxine", "Vitamin B6").caption(SUPPLEMENT),
        new Med("Pyrimethamine").caption(ANTIPROTOZOAL),
        new Med("Quinine").caption(ANTIMALARIAL),
        new Med("Resomal", "Rehydration Solution for Malnutrition").caption(),
        new Med("Retinol", "Vitamin A").caption(SUPPLEMENT),
        new Med("Rifampicin", "R").caption(ANTITUBERCULAR),
        new Med("Risperidone").caption(ANTIPSYCHOTIC),
        new Med("Ritonavir", "RTV").caption(ANTIRETROVIRAL),
        // ! new Med("Salbutamol").caption(ALBUTEROL),
        new Med("Salbutamol, aerosol", "Albuterol").caption(BRONCHODILATOR),
        new Med("Salbutamol, nebuliser solution", "Albuterol").caption(BRONCHODILATOR),
        new Med("Sertraline").caption(ANTIDEPRESSANT),
        new Med("Spironolactone").caption(DIURETIC),
        new Med("Sulfadiazine").caption(ANTIBACTERIAL),
        new Med("Sulfadoxine/Pyrimethamine", "SP").caption(ANTIMALARIAL),
        new Med("Tenofovir Disoproxil Fumarate", "TDF").caption(ANTIRETROVIRAL),
        new Med("Thiamine", "Vitamin B1").caption(SUPPLEMENT),
        new Med("Tinidazole").caption(ANTIPROTOZOAL, ANTIBACTERIAL),
        new Med("Tramadol").caption(OPIOID_ANALGESIC),
        new Med("Tranexamic Acid").caption(ANTIFIBRINOLYTIC),
        new Med("Triclabendazole").caption(ANTHELMINTHIC),
        new Med("Trihexyphenidyl").caption(ANTIPARKINSONIAN),
        new Med("Ulipristal").caption(CONTRACEPTIVE),
        new Med("Valproic Acid", "Sodium Valproate").caption(ANTIEPILEPTIC),
        new Med("Vitamin B6").caption(SUPPLEMENT),
        new Med("Zidovudine", "AZT", "ZDV").caption(ANTIRETROVIRAL),
        new Med("Zidovudine/Lamivudine", "AZT/3TC").caption(ANTIRETROVIRAL),
        new Med("Zidovudine/Lamivudine/Nevirapine", "AZT/3TC/NVP").caption(ANTIRETROVIRAL),
        new Med("Zinc Sulfate").caption(SUPPLEMENT),

        // Injectable drugs
        new Med("Amphotericin B, conventional").caption(ANTIFUNGAL),
        new Med("Amphotericin B, liposomal").caption(ANTIFUNGAL),
        new Med("Ampicillin").caption(ANTIBACTERIAL),
        // ! new Med("Artemether"),
        new Med("Artesunate").caption(ANTIMALARIAL),
        new Med("Atropine").caption(ANTISPASMODIC),
        new Med("Benzathine Benzylpenicillin").caption(ANTIBACTERIAL),
        new Med("Benzylpenicillin", "Penicillin G").caption(ANTIBACTERIAL),
        new Med("Calcium Gluconate").caption(SUPPLEMENT),
        new Med("Cefotaxime").caption(ANTIBACTERIAL),
        new Med("Ceftriaxone").caption(ANTIBACTERIAL),
        new Med("Chloramphenicol").caption(ANTIBACTERIAL),
        // ! new Med("Long-Acting Oily Chloramphenicol"),
        new Med("Chlorpromazine").caption(ANTIPSYCHOTIC),
        new Med("Clindamycin").caption(ANTIBACTERIAL),
        new Med("Cloxacillin").caption(ANTIBACTERIAL),
        new Med("Co-amoxiclav", "Amoxicillin/Clavulanic Acid").caption(ANTIBACTERIAL),
        new Med("Dexamethasone").caption(CORTICOSTEROID),
        new Med("Diazepam, emulsion").caption(SEDATIVE, ANTICONVULSANT),
        new Med("Diazepam, solution").caption(SEDATIVE, ANTICONVULSANT),
        new Med("Diclofenac").caption(ANALGESIC, ANTIPYRETIC),
        new Med("Digoxin").caption(CARDIOTONIC),
        // ! new Med("Dipyrone"),
        new Med("Eflornithine").caption(ANTIPROTOZOAL_FOR_SLEEPING_SICKNESS),
        new Med("Epinephrine", "EPN", "Adrenaline").caption(ANTIANAPHYLACTIC),
        new Med("Etonogestrel, subdermal implant").caption(CONTRACEPTIVE),
        new Med("Fluconazole").caption(ANTIFUNGAL),
        new Med("Furosemide").caption(DIURETIC),
        new Med("Gentamicin").caption(ANTIBACTERIAL),
        new Med("Glucose, 50%", "Dextrose, 50%").caption(),
        new Med("Haloperidol").caption(ANTIPSYCHOTIC),
        new Med("Haloperidol Decanoate").caption(ANTIPSYCHOTIC),
        new Med("Heparin").caption(ANTICOAGULANT),
        new Med("Hydralazine").caption(ANTIHYPERTENSIVE),
        new Med("Hydrocortisone").caption(CORTICOSTEROID),
        new Med("Hyoscine Butylbromide", "Butylscopolamine").caption(ANTISPASMODIC),
        new Med("Insulin, biphasic"),
        new Med("Insulin, intermediate-acting"),
        new Med("Insulin, long-acting"),
        new Med("Insulin, short-acting"),
        new Med("Ketamine").caption(ANAESTHETIC),
        new Med("Labetalol").caption(BETA_BLOCKER),
        new Med("Levonorgestrel, subdermal implant").caption(CONTRACEPTIVE),
        new Med("Lidocaine", "Lignocaine").caption(ANAESTHETIC),
        new Med("Magnesium Sulfate", "MgSO4").caption(ANTICONVULSANT),
        new Med("Medroxyprogesterone").caption(CONTRACEPTIVE),
        new Med("Melarsoprol").caption(ANTIPROTOZOAL_FOR_SLEEPING_SICKNESS),
        // ! new Med("Metamizole"),
        new Med("Methylergometrine").caption(OXYTOCIC),
        new Med("Metoclopramide").caption(ANTIEMETIC),
        new Med("Metronidazole").caption(ANTIPROTOZOAL, ANTIBACTERIAL),
        new Med("Morphine").caption(OPIOID_ANALGESIC),
        new Med("Naloxone").caption(OPIOID_ANTAGONIST),
        // ! new Med("Noramidopyrine"),
        new Med("Omeprazole").caption(ANTACID),
        new Med("Ondansetron").caption(ANTIEMETIC),
        new Med("Oxytocin").caption(OXYTOCIC),
        new Med("Paracetamol", "Acetaminophen").caption(ANALGESIC, ANTIPYRETIC),
        new Med("Penicillin G").caption(ANTIBACTERIAL),
        new Med("Pentamidine").caption(ANTIPROTOZOAL),
        new Med("Phenobarbital").caption(ANTICONVULSANT),
        new Med("Phytomenadione", "Vitamin K1").caption(SUPPLEMENT),
        new Med("Potassium Chloride, 10%", "KCl, 10%").caption(),
        new Med("Promethazine").caption(ANTIHISTAMINE, ANTIEMETIC),
        new Med("Protamine").caption(HEPARIN_ANTIDOTE),
        new Med("Quinine").caption(ANTIMALARIAL),
        new Med("Salbutamol", "Albuterol").caption(BRONCHODILATOR),
        new Med("Sodium Bicarbonate, 8.4%"),
        new Med("Spectinomycin").caption(ANTIBACTERIAL),
        new Med("Streptomycin", "S").caption(ANTIBACTERIAL),
        new Med("Suramin").caption(ANTIPROTOZOAL_FOR_SLEEPING_SICKNESS),
        new Med("Thiamine", "Vitamin B1").caption(SUPPLEMENT),
        new Med("Tramadol").caption(OPIOID_ANALGESIC),
        new Med("Tranexamic Acid").caption(ANTIFIBRINOLYTIC),

        // Infusion fluids
        new Med("Glucose, 5%", "Dextrose, 5%").caption(),
        new Med("Glucose, 10%", "Dextrose, 10%").caption(),
        new Med("Modified Fluid Gelatin").caption(PLASMA_SUBSTITUTE),
        new Med("Polygeline").caption(PLASMA_SUBSTITUTE),
        new Med("Ringer Lactate").caption(FLUID_REPLACER),
        new Med("RLG 5% mix", "Ringer Lactate/Glucose, 5%").caption(FLUID_REPLACER),
        new Med("RLG 10% mix", "Ringer Lactate/Glucose, 10%").caption(FLUID_REPLACER),
        new Med("Sodium Chloride, 0.9%", "NaCl").caption(FLUID_REPLACER),

        // Vaccines, immunoglobulins, and antisera
        new Med("Oral Cholera Vaccine O1+O139").caption(VACCINE),
        new Med("Diphtheria/Tetanus/Pertussis Vaccine", "DTP").caption(VACCINE),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B Vaccine").caption(VACCINE),
        new Med("Diphtheria/Tetanus/Pertussis/Hepatitis B/Hib Vaccine").caption(VACCINE),
        new Med("Hepatitis B Vaccine").caption(VACCINE),
        new Med("Japanese Encephalitis Vaccine").caption(VACCINE),
        new Med("Measles Vaccine").caption(VACCINE),
        new Med("Meningococcal A Conjugate Vaccine").caption(VACCINE),
        new Med("Meningococcal A+C Vaccine").caption(VACCINE),
        new Med("Meningococcal A+C+W135 Vaccine").caption(VACCINE),
        new Med("Human Papillomavirus Vaccine", "HPV").caption(VACCINE),
        new Med("Pneumococcal Conjugate Vaccine", "PCV").caption(VACCINE),
        new Med("Inactivated Poliomyelitis Vaccine", "IPV").caption(VACCINE),
        new Med("Oral Poliomyelitis Vaccine", "OPV").caption(VACCINE),
        new Med("Human Rabies Immunoglobulin", "HRIG").caption(),
        new Med("Rabies Vaccine").caption(VACCINE),
        new Med("Oral Rotavirus Vaccine").caption(VACCINE),
        new Med("Human Tetanus Immunoglobulin", "HTIG").caption(),
        new Med("Tetanus Vaccine", "TT").caption(VACCINE),
        new Med("Tetanus-Diphtheria Vaccine", "Td").caption(VACCINE),
        // ! new Med("Tetanus Antitoxin, Equine"),
        new Med("Tuberculosis Vaccine", "BCG Vaccine").caption(VACCINE),
        new Med("Typhoid Conjugate Vaccine", "TCV").caption(VACCINE),
        new Med("Yellow Fever Vaccine").caption(VACCINE),

        // Drugs for external use, antiseptics, and disinfectants
        new Med("Aciclovir, eye ointment", "Acyclovir").caption(ANTIVIRAL),
        new Med("Alcohol-Based Hand Rub").caption(ANTISEPTIC),
        new Med("Artesunate, rectal").caption(ANTIMALARIAL),
        new Med("Benzoic Acid/Salicylic Acid ointment", "Whitfield's ointment").caption(ANTIFUNGAL),
        new Med("Benzyl Benzoate, lotion").caption(INSECTICIDE_FOR_SCABIES),
        new Med("Calamine, lotion").caption(ANTIPRURITIC),
        new Med("Chlorhexidine, 5% solution").caption(ANTISEPTIC),
        new Med("Chlorhexidine, 7.1% dermal gel").caption(ANTISEPTIC),
        new Med("Chlorhexidine, 0.2% mouthwash").caption(ANTISEPTIC),
        new Med("Ciprofloxacin, ear drops").caption(ANTIBACTERIAL),
        new Med("Clotrimazole, vaginal tablet").caption(ANTIFUNGAL),
        new Med("Dinoprostone, vaginal gel").caption(OXYTOCIC),
        new Med("Ethyl Alcohol", "Ethanol").caption(ANTISEPTIC, DISINFECTANT),
        new Med("Fluorescein, eye drops").caption(DIAGNOSTIC_STAINING_AGENT),
        new Med("Hydrocortisone, cream").caption(CORTICOSTEROID),
        new Med("Malathion, lotion").caption(INSECTICIDE_FOR_LICE),
        // ! new Med("Methylrosanilinium Chloride", "GV", "Crystal violet").caption(GENTIAN VIOLET),
        new Med("Miconazole, cream").caption(ANTIFUNGAL),
        new Med("Mupirocin, ointment").caption(ANTIBACTERIAL),
        new Med("Nystatin, vaginal tablet").caption(ANTIFUNGAL),
        new Med("Oxybuprocaine, eye drops").caption(ANAESTHETIC),
        new Med("Permethrin, 1% lotion").caption(INSECTICIDE_FOR_LICE),
        new Med("Permethrin, 5% lotion").caption(INSECTICIDE_FOR_SCABIES),
        new Med("Pilocarpine, eye drops").caption(MIOTIC),
        new Med("Podophyllotoxin, 0.5% solution").caption(ANTIVIRAL_FOR_HPV),
        new Med("Podophyllum Resin, solution").caption(ANTIVIRAL_FOR_HPV),
        new Med("Povidone Iodine, aqueous solution", "Polyvidone iodine", "PVI").caption(ANTISEPTIC, DISINFECTANT),
        new Med("Povidone Iodine, scrub solution", "Polyvidone iodine", "PVI").caption(ANTISEPTIC, DISINFECTANT),
        new Med("Silver Sulfadiazine, cream").caption(ANTIBACTERIAL),
        new Med("Sodium Dichloroisocyanurate", "NaDCC").caption(DISINFECTANT),
        new Med("Calcium Hypochlorite", "HTH").caption(DISINFECTANT),
        new Med("Sodium Hypochlorite, solution", "Bleach").caption(DISINFECTANT),
        new Med("Chlorinated Lime, powder").caption(DISINFECTANT),
        new Med("Tetracycline, eye ointment").caption(ANTIBACTERIAL),
        new Med("Zinc Oxide, ointment").caption(SKIN_PROTECTOR),

        // ==== Additional Ebola treatments and vaccines

        // Experimental Ebola treatments
        new Med("Amodiaquine").caption(EXPERIMENTAL_EBOLA_TREATMENT),
        new Med("Favipiravir").caption(EXPERIMENTAL_EBOLA_TREATMENT),
        new Med("Remdesivir").caption(EXPERIMENTAL_EBOLA_TREATMENT),
        new Med("ZMapp").caption(EXPERIMENTAL_EBOLA_TREATMENT),

        // Experimental Ebola vaccines
        new Med("cAd3-EBOZ").caption(EXPERIMENTAL_EBOLA_VACCINE),
        new Med("VSV-EBOV").caption(EXPERIMENTAL_EBOLA_VACCINE),
        new Med("Ad5-EBOV").caption(EXPERIMENTAL_EBOLA_VACCINE),
        new Med("Ad26-ZEBOV/MVA-BN").caption(EXPERIMENTAL_EBOLA_VACCINE),
        new Med("rVSV-ZEBOV").caption(EXPERIMENTAL_EBOLA_VACCINE)
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
        Collections.sort(result, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    public List<Med> suggestCompletions(CharSequence constraint) {
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

    public static class Med {
        String name;
        Loc[] captions;
        String[] aliases;
        String label;
        String filterTarget;

        public Med(String name, String... aliases) {
            this.name = name;
            this.captions = new Loc[0];
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

        public Med caption(Loc... captions) {
            this.captions = captions;
            return this;
        }

        public void showInView(View itemView) {
            Locale locale = Locale.getDefault();
            Utils.setText(itemView, R.id.label, label);
            String caption = "";
            for (Loc loc : captions) {
                if (!caption.isEmpty()) caption += ", ";
                caption += loc.get(locale);
            }
            Utils.setText(itemView, R.id.caption, caption);
        }

        public @NonNull String getValue() {
            return name;
        }
    }
}
