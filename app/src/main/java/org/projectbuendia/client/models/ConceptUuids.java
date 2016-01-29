// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.models;

import org.projectbuendia.client.resolvables.ResStatus;
import java.util.HashMap;

/**
 * Defines hardcoded concept ids expected to exist on the OpenMRS server. Over time, values in this
 * file should be phased out and replaced with modular configuration, either on the server or the
 * client.
 */
public class ConceptUuids {
    public static final String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    public static final String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // Persistent fields
    public static HashMap<String,String> PERSISTENT_FIELDS = new HashMap<String,String>();

    static {
        // This is terrible for i18n and generalisation - every time we change these fields or switch languages we have to edit them here
        // TODO: Refactor this to get the labels from the concept with that UUID
        // TODO: Determine from the profile which fields should be persistent (on a per form basis), instead of here

        // These are case insensitive

        // Diagnoses
        PERSISTENT_FIELDS.put("5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "pregnancy");
        PERSISTENT_FIELDS.put("f50c9c63-3ff9-4c26-9d18-12bfc58a3d07", "iv access present");
        PERSISTENT_FIELDS.put("1777000619AAAAAAAAAAAAAAAAAAAAAAAAAA", "acute watery diarrhoea (awd)");
        PERSISTENT_FIELDS.put("1777000620AAAAAAAAAAAAAAAAAAAAAAAAAA", "suspected bloody diarrhoea");
        PERSISTENT_FIELDS.put("777138868AAAAAAAAAAAAAAAAAAAAAAAAAAA", "bloody diarrhoea");
        PERSISTENT_FIELDS.put("777000124AAAAAAAAAAAAAAAAAAAAAAAAAAA", "suspected amoebiasis");
        PERSISTENT_FIELDS.put("777139457AAAAAAAAAAAAAAAAAAAAAAAAAAA", "suspected giardiasis");
        PERSISTENT_FIELDS.put("777145443AAAAAAAAAAAAAAAAAAAAAAAAAAA", "chronic diarrhoea (>14 days)");
        PERSISTENT_FIELDS.put("777122604AAAAAAAAAAAAAAAAAAAAAAAAAAA", "cholera");
        PERSISTENT_FIELDS.put("777148353AAAAAAAAAAAAAAAAAAAAAAAAAAA", "ascaris");
        PERSISTENT_FIELDS.put("777119356AAAAAAAAAAAAAAAAAAAAAAAAAAA", "hookworm");
        PERSISTENT_FIELDS.put("777000199AAAAAAAAAAAAAAAAAAAAAAAAAAA", "helminths (other)");
        PERSISTENT_FIELDS.put("777113224AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Rhinitis");
        PERSISTENT_FIELDS.put("777149609AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Acute otitis media");
        PERSISTENT_FIELDS.put("1777000622AAAAAAAAAAAAAAAAAAAAAAAAAA", "Chronic suppurative otitis media");
        PERSISTENT_FIELDS.put("777114431AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Otitis externa");
        PERSISTENT_FIELDS.put("777149579AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Acute Pharyngitis");
        PERSISTENT_FIELDS.put("777149716AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Acute laryngitis");
        PERSISTENT_FIELDS.put("1777000700AAAAAAAAAAAAAAAAAAAAAAAAAA", "Simple Lower RTI");
        PERSISTENT_FIELDS.put("1777000701AAAAAAAAAAAAAAAAAAAAAAAAAA", "Severe Lower RTI");
        PERSISTENT_FIELDS.put("777114100AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Pneumonia");
        PERSISTENT_FIELDS.put("777113809AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Atypical pneumonia");
        PERSISTENT_FIELDS.put("777121392AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Aspiration pneumonia");
        PERSISTENT_FIELDS.put("777121009AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Bronchiolitis");
        PERSISTENT_FIELDS.put("777114190AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Whooping cough (pertussis)");
        PERSISTENT_FIELDS.put("121375AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Asthma");
        PERSISTENT_FIELDS.put("777159950AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Simple Malaria");
        PERSISTENT_FIELDS.put("777160155AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Severe Malaria");
        PERSISTENT_FIELDS.put("1777000645AAAAAAAAAAAAAAAAAAAAAAAAAA", "Mild Anaemia");
        PERSISTENT_FIELDS.put("1777000646AAAAAAAAAAAAAAAAAAAAAAAAAA", "Moderate Anaemia");
        PERSISTENT_FIELDS.put("1777000647AAAAAAAAAAAAAAAAAAAAAAAAAA", "Severe Anaemia");
        PERSISTENT_FIELDS.put("777005334AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Oral thrush");
        PERSISTENT_FIELDS.put("777139438AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Gingivitis");
        PERSISTENT_FIELDS.put("777120755AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Angular stomatitis");
        PERSISTENT_FIELDS.put("777139407AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Glossitis");
        PERSISTENT_FIELDS.put("777139437AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Gingivostomatitis");
        PERSISTENT_FIELDS.put("1777000626AAAAAAAAAAAAAAAAAAAAAAAAAA", "NOMA");
        PERSISTENT_FIELDS.put("777115247AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Night blindness");
        PERSISTENT_FIELDS.put("777143597AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Conjunctival dryness");
        PERSISTENT_FIELDS.put("777147267AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Bitot's spots");
        PERSISTENT_FIELDS.put("1777000627AAAAAAAAAAAAAAAAAAAAAAAAAA", "Corneal dryness");
        PERSISTENT_FIELDS.put("777136292AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Keratomalacia");
        PERSISTENT_FIELDS.put("777119905AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Conjunctivitis");
        PERSISTENT_FIELDS.put("777112287AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Trachoma");
        PERSISTENT_FIELDS.put("1777000702AAAAAAAAAAAAAAAAAAAAAAAAAA", "Kwashiorkor related skin lesions");
        PERSISTENT_FIELDS.put("777137693AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Impetigo");
        PERSISTENT_FIELDS.put("777150555AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Abcess");
        PERSISTENT_FIELDS.put("777000134AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Cellulitis");
        PERSISTENT_FIELDS.put("777124654AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Tinea (scalp/capitis)");
        PERSISTENT_FIELDS.put("777124650AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Tinea (body/corporis)");
        PERSISTENT_FIELDS.put("777000140AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Scabies");
        PERSISTENT_FIELDS.put("777118731AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Eczema");
        PERSISTENT_FIELDS.put("777148705AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Anogenital candidiasis");
        PERSISTENT_FIELDS.put("777126707AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Septic shock");
        PERSISTENT_FIELDS.put("1777000628AAAAAAAAAAAAAAAAAAAAAAAAAA", "Hypovolemic shock, Dehydration");
        PERSISTENT_FIELDS.put("1777000629AAAAAAAAAAAAAAAAAAAAAAAAAA", "Hypovolemic shock, Bleeding");
        PERSISTENT_FIELDS.put("777146166AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Cardiogenic shock");
        PERSISTENT_FIELDS.put("1777000630AAAAAAAAAAAAAAAAAAAAAAAAAA", "Anaphylactic shock");
        PERSISTENT_FIELDS.put("1777000631AAAAAAAAAAAAAAAAAAAAAAAAAA", "Suspected acute meningitis");
        PERSISTENT_FIELDS.put("1777000632AAAAAAAAAAAAAAAAAAAAAAAAAA", "Confirmed acute meningitis");
        PERSISTENT_FIELDS.put("777152209AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Measles (uncomplicated)");
        PERSISTENT_FIELDS.put("777115886AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Measles(complicated)");
        PERSISTENT_FIELDS.put("777111633AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Urinary tract infection (UTI)");
        PERSISTENT_FIELDS.put("777127990AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Fever of unknown origin");
        PERSISTENT_FIELDS.put("777000892AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Chickenpox");
        PERSISTENT_FIELDS.put("124957AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Tetanus");
        PERSISTENT_FIELDS.put("777127394AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Rickets");
        PERSISTENT_FIELDS.put("1777000633AAAAAAAAAAAAAAAAAAAAAAAAAA", "Sickle cell disease");
        PERSISTENT_FIELDS.put("777114675AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Osteomielitis");
        PERSISTENT_FIELDS.put("777114702AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Osteoarthritis");
        PERSISTENT_FIELDS.put("777123084AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Visceral Leishmaniasis (Kala-azar)");
        PERSISTENT_FIELDS.put("777117152AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Schistosomiasis");
        PERSISTENT_FIELDS.put("1777000635AAAAAAAAAAAAAAAAAAAAAAAAAA", "Pulmonary TB");
        PERSISTENT_FIELDS.put("777115753AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Miliary TB");
        PERSISTENT_FIELDS.put("777156204AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Extrapulmonary TB");
        PERSISTENT_FIELDS.put("777111873AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Lymph node TB");
        PERSISTENT_FIELDS.put("777161355AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Abdominal TB");
        PERSISTENT_FIELDS.put("777111904AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Spinal TB (Pott’s Disease)");
        PERSISTENT_FIELDS.put("1777000636AAAAAAAAAAAAAAAAAAAAAAAAAA", "Osteo-articular TB");
        PERSISTENT_FIELDS.put("777111946AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Pleural effusion TB");
        PERSISTENT_FIELDS.put("777124033AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Pericardial TB");
        PERSISTENT_FIELDS.put("777111967AAAAAAAAAAAAAAAAAAAAAAAAAAA", "TB Meningitis");
        PERSISTENT_FIELDS.put("777117825AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Genitourinary TB");
        PERSISTENT_FIELDS.put("777124028AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Skin TB");
        PERSISTENT_FIELDS.put("777138571AAAAAAAAAAAAAAAAAAAAAAAAAAA", "HIV positive");
        // Complications
        PERSISTENT_FIELDS.put("777138061AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Hypoglycaemia");
        PERSISTENT_FIELDS.put("777117326AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Hypothermia");
        PERSISTENT_FIELDS.put("113054AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Convulsions");
        PERSISTENT_FIELDS.put("777118876AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Altered state of consciousness");
        PERSISTENT_FIELDS.put("122983AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Vomiting");
        PERSISTENT_FIELDS.put("777147241AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Bleeding");
        PERSISTENT_FIELDS.put("777138099AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Fluid overload");
        PERSISTENT_FIELDS.put("150915AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "Abdominal distention");
        PERSISTENT_FIELDS.put("1777000637AAAAAAAAAAAAAAAAAAAAAAAAAA", "Persistent oedema");
        PERSISTENT_FIELDS.put("777130473AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Recurrent oedema");
        PERSISTENT_FIELDS.put("1777000638AAAAAAAAAAAAAAAAAAAAAAAAAA", "Persistent fever");
        PERSISTENT_FIELDS.put("1777000639AAAAAAAAAAAAAAAAAAAAAAAAAA", "Refeeding syndrome");
        PERSISTENT_FIELDS.put("1777000640AAAAAAAAAAAAAAAAAAAAAAAAAA", "Difficulties with breastfeeding");
        PERSISTENT_FIELDS.put("777135595AAAAAAAAAAAAAAAAAAAAAAAAAAA", "Persistent lack of appetite");
        PERSISTENT_FIELDS.put("1777000641AAAAAAAAAAAAAAAAAAAAAAAAAA", "inappropriate weight response (if stagnant or losing weight in phase 2)");
        PERSISTENT_FIELDS.put("1777000642AAAAAAAAAAAAAAAAAAAAAAAAAA", "child behavioural problems");
    }

    public static final String PHASE_UUID = "UUID";
    public static final String BREASTFEEDING_UUID = "UUID";
    public static final String NG_FEEDING_UUID = "UUID";
    public static final String TYPE_OF_LIQUID_FOOD_UUID = "UUID";
    public static final String LIQUID_FOOD_SERVINGS_UUID = "UUID";
    public static final String LIQUID_FOOD_ML_PER_SERVING_UUID = "UUID";
    public static final String TYPE_OF_SOLID_FOOD_UUID = "UUID";
    public static final String RUTF_SERVINGS_UUID = "UUID";
    public static final String RUTF_SACHETS_UUID = "UUID";

    public static final String PULSE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String RESPIRATION_UUID = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_NP_UUID = "162826AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_L_UUID = "162827AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FIRST_SYMPTOM_DATE_UUID = "1730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String ADMISSION_DATE_UUID = "162622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String GENERAL_CONDITION_WELL_UUID = "1855AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_UNWELL_UUID =
        "137793AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_CRITICAL_UUID =
        "2827e7ac-10c1-4d3f-9fa4-0239771d8548";
    public static final String GENERAL_CONDITION_PALLIATIVE_UUID =
        "7cea1f8f-88cb-4f9c-a9d6-dc28d6eaa520";
    public static final String GENERAL_CONDITION_CONVALESCENT_UUID =
        "119844AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_DISCHARGED_NON_CASE =
        "e4a20c4a-6f13-11e4-b315-040ccecfdba4";
    public static final String GENERAL_CONDITION_CURED =
        "159791AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_SUSPECTED_DEAD =
        "91dc5fcc-fa9e-4ccd-8cd0-0d203923493f";
    public static final String GENERAL_CONDITION_CONFIRMED_DEAD =
        "160432AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String[] GENERAL_CONDITION_UUIDS = new String[] {
        GENERAL_CONDITION_WELL_UUID,
        GENERAL_CONDITION_UNWELL_UUID,
        GENERAL_CONDITION_CRITICAL_UUID,
        GENERAL_CONDITION_PALLIATIVE_UUID,
        GENERAL_CONDITION_CONVALESCENT_UUID,
        GENERAL_CONDITION_SUSPECTED_DEAD,
        GENERAL_CONDITION_CONFIRMED_DEAD,
        GENERAL_CONDITION_CURED,
        GENERAL_CONDITION_DISCHARGED_NON_CASE
    };

    /** UUID for the (question) concept for the temperature. */
    public static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the weight. */
    public static final String WEIGHT_UUID = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Diarrhea. */
    public static final String DIARRHEA_UUID = "1aa247f3-2d83-4efc-94bc-123b1a71b19f";

    /** UUID for the (question) concept for (any) bleeding. */
    public static final String BLEEDING_UUID = "147241AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // TODO: This may break localization.
    /** Group name for Bleeding Sites section. */
    public static final String BLEEDING_SITES_NAME = "Bleeding site";

    /** UUID for the (question) concept for Vomiting. */
    public static final String VOMITING_UUID = "405ad95d-f6e1-4023-a459-28cffdb055c5";

    /** UUID for the (question) concept for pain. */
    public static final String PAIN_UUID = "f75da5de-404c-42d0-b484-b69a4896e093";

    /** UUID for the (question) concept for best conscious state (AVPU). */
    public static final String RESPONSIVENESS_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for (severe) weakness. */
    public static final String WEAKNESS_UUID = "5226AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for appetite. */
    public static final String APPETITE_UUID = "777000003AAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for oedema. */
    public static final String OEDEMA_UUID = "460AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the notes field. */
    public static final String NOTES_UUID = "162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept None. */
    public static final String NONE_UUID = "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Mild. */
    public static final String MILD_UUID = "1148AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Moderate. */
    public static final String MODERATE_UUID = "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Severe. */
    public static final String SEVERE_UUID = "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Mobility. */
    public static final String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";

    /** UUID for the (answer) concept of Yes. */
    public static final String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of No. */
    public static final String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of the answer is unknown. */
    public static final String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for indicating a patient can eat solid food. */
    public static final String SOLID_FOOD_UUID = "159597AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for Normal. */
    public static final String NORMAL_UUID = "1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** Returns the {@link ResStatus} for the specified condition UUID. */
    public static ResStatus getResStatus(String conditionUuid) {
        if (conditionUuid == null) {
            return ResStatus.UNKNOWN;
        }

        switch (conditionUuid) {
            case ConceptUuids.GENERAL_CONDITION_WELL_UUID:
                return ResStatus.WELL;
            case ConceptUuids.GENERAL_CONDITION_UNWELL_UUID:
                return ResStatus.UNWELL;
            case ConceptUuids.GENERAL_CONDITION_CRITICAL_UUID:
                return ResStatus.CRITICAL;
            case ConceptUuids.GENERAL_CONDITION_PALLIATIVE_UUID:
                return ResStatus.PALLIATIVE;
            case ConceptUuids.GENERAL_CONDITION_CONVALESCENT_UUID:
                return ResStatus.CONVALESCENT;
            case ConceptUuids.GENERAL_CONDITION_DISCHARGED_NON_CASE:
                return ResStatus.DISCHARGED_NON_CASE;
            case ConceptUuids.GENERAL_CONDITION_CURED:
                return ResStatus.DISCHARGED_CURED;
            case ConceptUuids.GENERAL_CONDITION_SUSPECTED_DEAD:
                return ResStatus.SUSPECTED_DEAD;
            case ConceptUuids.GENERAL_CONDITION_CONFIRMED_DEAD:
                return ResStatus.CONFIRMED_DEAD;
            default:
                return ResStatus.UNKNOWN;
        }
    }

    private ConceptUuids() {
    }
}
