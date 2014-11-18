package org.msf.records.net;

/**
 * Created by nfortescue on 11/17/14.
 */
public class Constants {
    /**
     * IP address of localhost for the computer the emulator is running on.
     *
     * See http://developer.android.com/tools/devices/emulator.html#emulatornetworking
     */
    public static final String LOCALHOST_EMULATOR = "10.0.2.2";;

    /**
     * The base path for the OpenMRS REST API from the Project Buendia module.
     */
    public static final String API_PATH = "/openmrs/ws/rest/v1/projectbuendia";

    /**
     * The base URL for the OpenMRS REST API from the Project Buendia module, when running against
     * a local server.
     */
    public static final String API_URL = "http://" + LOCALHOST_EMULATOR + ":8080" + API_PATH;

    /**
     * Recommended user for local admin in development.
     */
    public static final String LOCAL_ADMIN_USERNAME = "admin";

    /** Recommended password for local admin in development. */
    public static final String LOCAL_ADMIN_PASSWORD = "Admin123";

    /** Hard-coded UUID for the 'Add Patient' xform. */
    public static final String ADD_PATIENT_UUID = "c47d4d3d-f9a3-4f97-9623-d7acee81d401";

    /**
     * Our default GCE instance for development.
     */
    public static final String GCE_INSTANCE = "http://104.155.15.141:8080";

    /**
     * The base URL for the OpenMRS REST API from the Project Buendia module, when running against
     * the GCE instance.
     */
    public static final String GCE_URL = GCE_INSTANCE + API_PATH;

    public static String makeNewPatientFormInstance(String identifierEgKH1234, String firstName,
                                                    String lastName) {
        return "<?xml version='1.0' ?>" +
                "<form id=\"5\" name=\"Demo New Patient\" uuid=\"c47d4d3d-f9a3-4f97-9623-d7acee81d401\" " +
                "version=\"0.2\" xmlns:openmrs=\"formentry.infopath_server_url cannot be empty/moduleServlet/formentry/forms/schema/5-1\" " +
                "xmlns:xd=\"http://schemas.microsoft.com/office/infopath/2003\"><header><enterer /><date_entered /><session /><uid />" +
                "</header><patient><msf_patient_id openmrs_attribute=\"identifier\" openmrs_table=\"patient_identifier\">" +
                identifierEgKH1234 +
                "</msf_patient_id>" +
                "<patient.family_name openmrs_attribute=\"family_name\" openmrs_table=\"patient_name\">" +
                lastName +
                "</patient.family_name>" +
                "<patient.given_name openmrs_attribute=\"given_name\" openmrs_table=\"patient_name\">" +
                firstName +
                "</patient.given_name>" +
                "<patient.middle_name openmrs_attribute=\"middle_name\" openmrs_table=\"patient_name\" />" +
                "<patient.birthdate openmrs_attribute=\"birthdate\" openmrs_table=\"patient\">1973-11-07</patient.birthdate>" +
                "<patient.sex openmrs_attribute=\"gender\" openmrs_table=\"patient\">M</patient.sex></patient>" +
                "<encounter><encounter.encounter_datetime openmrs_attribute=\"encounter_datetime\" openmrs_table=\"encounter\">2014-11-18</encounter.encounter_datetime>" +
                "<encounter.location_id openmrs_attribute=\"location_id\" openmrs_table=\"encounter\">1</encounter.location_id>" +
                "<encounter.provider_id openmrs_attribute=\"provider_id\" openmrs_table=\"encounter\" provider_id_type=\"PROVIDER.ID\">1</encounter.provider_id></encounter>" +
                "<location><location.assigned_zone openmrs_attribute=\"assigned_zone\" openmrs_table=\"person_attribute\">${patient.attributeMap.assigned_zone}</location.assigned_zone>" +
                "<location.assigned_tent openmrs_attribute=\"assigned_tent\" openmrs_table=\"person_attribute\">I ${patient.attributeMap.assigned_tent}</location.assigned_tent>" +
                "<location.assigned_bed openmrs_attribute=\"assigned_bed\" openmrs_table=\"person_attribute\">${patient.attributeMap.assigned_bed}</location.assigned_bed></location>" +
                "<obs openmrs_concept=\"1238^MEDICAL RECORD OBSERVATIONS^99DCT\" openmrs_datatype=\"ZZ\">" +
                "<mobility multiple=\"0\" openmrs_concept=\"162693^Mobility^99DCT\" openmrs_datatype=\"CWE\">" +
                "<date n0:nil=\"true\" xmlns:n0=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n1:nil=\"true\" xmlns:n1=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n2:nil=\"true\" xmlns:n2=\"http://www.w3.org/2001/XMLSchema-instance\">1067^UNKNOWN^99DCT</value></mobility>" +
                "<responsiveness multiple=\"0\" openmrs_concept=\"162698^Responsiveness^99DCT\" openmrs_datatype=\"CWE\"><date n3:nil=\"true\" xmlns:n3=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n4:nil=\"true\" xmlns:n4=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n5:nil=\"true\" xmlns:n5=\"http://www.w3.org/2001/XMLSchema-instance\">120345^Confusion^99DCT</value>" +
                "</responsiveness><pregnancy_status multiple=\"0\" openmrs_concept=\"5272^PREGNANCY STATUS^99DCT\" openmrs_datatype=\"CWE\">" +
                "<date n6:nil=\"true\" xmlns:n6=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n7:nil=\"true\" xmlns:n7=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n8:nil=\"true\" xmlns:n8=\"http://www.w3.org/2001/XMLSchema-instance\">1066^NO^99DCT</value></pregnancy_status>" +
                "<fever multiple=\"0\" openmrs_concept=\"140238^Fever^99DCT\" openmrs_datatype=\"CWE\"><date n9:nil=\"true\" xmlns:n9=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n10:nil=\"true\" xmlns:n10=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n11:nil=\"true\" xmlns:n11=\"http://www.w3.org/2001/XMLSchema-instance\" /></fever>" +
                "<temperature_c openmrs_concept=\"5088^Temperature (C)^99DCT\" openmrs_datatype=\"NM\"><date n12:nil=\"true\" xmlns:n12=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n13:nil=\"true\" xmlns:n13=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n14:nil=\"true\" xmlns:n14=\"http://www.w3.org/2001/XMLSchema-instance\">40.0</value></temperature_c>" +
                "<pulse openmrs_concept=\"5087^Pulse^99DCT\" openmrs_datatype=\"NM\"><date n15:nil=\"true\" xmlns:n15=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n16:nil=\"true\" xmlns:n16=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n17:nil=\"true\" xmlns:n17=\"http://www.w3.org/2001/XMLSchema-instance\">60.0</value></pulse>" +
                "<respiratory_rate openmrs_concept=\"5242^Respiratory rate^99DCT\" openmrs_datatype=\"NM\"><date n18:nil=\"true\" xmlns:n18=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n19:nil=\"true\" xmlns:n19=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n20:nil=\"true\" xmlns:n20=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "</respiratory_rate><diastolic_blood_pressure openmrs_concept=\"5086^DIASTOLIC BLOOD PRESSURE^99DCT\" openmrs_datatype=\"NM\">" +
                "<date n21:nil=\"true\" xmlns:n21=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n22:nil=\"true\" xmlns:n22=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n23:nil=\"true\" xmlns:n23=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "</diastolic_blood_pressure><systolic_blood_pressure openmrs_concept=\"5085^SYSTOLIC BLOOD PRESSURE^99DCT\" openmrs_datatype=\"NM\"><date n24:nil=\"true\" xmlns:n24=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n25:nil=\"true\" xmlns:n25=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n26:nil=\"true\" xmlns:n26=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "</systolic_blood_pressure><blood_oxygen_saturation openmrs_concept=\"5092^Blood oxygen saturation^99DCT\" openmrs_datatype=\"NM\"><date n27:nil=\"true\" xmlns:n27=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n28:nil=\"true\" xmlns:n28=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n29:nil=\"true\" xmlns:n29=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "</blood_oxygen_saturation><level_of_consciousness multiple=\"0\" openmrs_concept=\"162643^Level of consciousness^99DCT\" openmrs_datatype=\"CWE\"><date n30:nil=\"true\" xmlns:n30=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n31:nil=\"true\" xmlns:n31=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n32:nil=\"true\" xmlns:n32=\"http://www.w3.org/2001/XMLSchema-instance\">162644^Responds to pain^99DCT</value></level_of_consciousness>" +
                "<glasgow_coma_score_gcs openmrs_concept=\"160347^Glasgow coma scale^99DCT\" openmrs_datatype=\"NM\"><date n33:nil=\"true\" xmlns:n33=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n34:nil=\"true\" xmlns:n34=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n35:nil=\"true\" xmlns:n35=\"http://www.w3.org/2001/XMLSchema-instance\" /></glasgow_coma_score_gcs>" +
                "<diarrhea multiple=\"0\" openmrs_concept=\"142412^Diarrhea^99DCT\" openmrs_datatype=\"CWE\"><date n36:nil=\"true\" xmlns:n36=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n37:nil=\"true\" xmlns:n37=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n38:nil=\"true\" xmlns:n38=\"http://www.w3.org/2001/XMLSchema-instance\" /></diarrhea>" +
                "<vomiting multiple=\"0\" openmrs_concept=\"122983^VOMITING^99DCT\" openmrs_datatype=\"CWE\"><date n39:nil=\"true\" xmlns:n39=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n40:nil=\"true\" xmlns:n40=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n41:nil=\"true\" xmlns:n41=\"http://www.w3.org/2001/XMLSchema-instance\" /></vomiting><nausea openmrs_concept=\"5978^NAUSEA^99DCT\" openmrs_datatype=\"ZZ\"><date n42:nil=\"true\" xmlns:n42=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n43:nil=\"true\" xmlns:n43=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n44:nil=\"true\" xmlns:n44=\"http://www.w3.org/2001/XMLSchema-instance\" /></nausea>" +
                "<bleeding_gums openmrs_concept=\"147230^Bleeding Gums^99DCT\" openmrs_datatype=\"ZZ\"><date n45:nil=\"true\" xmlns:n45=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n46:nil=\"true\" xmlns:n46=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n47:nil=\"true\" xmlns:n47=\"http://www.w3.org/2001/XMLSchema-instance\" /></bleeding_gums>" +
                "<hemoptysis openmrs_concept=\"138905^Hemoptysis^99DCT\" openmrs_datatype=\"ZZ\"><date n48:nil=\"true\" xmlns:n48=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n49:nil=\"true\" xmlns:n49=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n50:nil=\"true\" xmlns:n50=\"http://www.w3.org/2001/XMLSchema-instance\" /></hemoptysis>" +
                "<haemorrhage_nasal openmrs_concept=\"133499^HAEMORRHAGE NASAL^99DCT\" openmrs_datatype=\"ZZ\"><date n51:nil=\"true\" xmlns:n51=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n52:nil=\"true\" xmlns:n52=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n53:nil=\"true\" xmlns:n53=\"http://www.w3.org/2001/XMLSchema-instance\" /></haemorrhage_nasal>" +
                "<hematuria openmrs_concept=\"840^Hematuria^99DCT\" openmrs_datatype=\"ZZ\"><date n54:nil=\"true\" xmlns:n54=\"http://www.w3.org/2001/XMLSchema-instance\" /><time n55:nil=\"true\" xmlns:n55=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<value n56:nil=\"true\" xmlns:n56=\"http://www.w3.org/2001/XMLSchema-instance\" /></hematuria><hematochezia multiple=\"0\" openmrs_concept=\"117671^HEMATOCHEZIA^99DCT\" openmrs_datatype=\"CWE\"><date n57:nil=\"true\" xmlns:n57=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n58:nil=\"true\" xmlns:n58=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n59:nil=\"true\" xmlns:n59=\"http://www.w3.org/2001/XMLSchema-instance\" /></hematochezia>" +
                "<abnormal_vaginal_bleeding multiple=\"0\" openmrs_concept=\"150802^ABNORMAL VAGINAL BLEEDING^99DCT\" openmrs_datatype=\"CWE\"><date n60:nil=\"true\" xmlns:n60=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n61:nil=\"true\" xmlns:n61=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n62:nil=\"true\" xmlns:n62=\"http://www.w3.org/2001/XMLSchema-instance\" /></abnormal_vaginal_bleeding>" +
                "<urine_output_qualitative multiple=\"0\" openmrs_concept=\"162647^Urine output, qualitative^99DCT\" openmrs_datatype=\"CWE\"><date n63:nil=\"true\" xmlns:n63=\"http://www.w3.org/2001/XMLSchema-instance\" />" +
                "<time n64:nil=\"true\" xmlns:n64=\"http://www.w3.org/2001/XMLSchema-instance\" /><value n65:nil=\"true\" xmlns:n65=\"http://www.w3.org/2001/XMLSchema-instance\" /></urine_output_qualitative></obs></form>";
    }

}