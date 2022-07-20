package potaymaster.aws.lambda.jasperreports;

public class ReportsLiterals {
    public static final String COMPLIANCE_BILLING_REPORT = "ComplianceBillingReport";
    public static final String CUSTOMER_BILLING_REPORT = "CustomerBillingReport";
    public static final String EMERGENCY_RECOVERY_REPORT = "EmergencyRecoveryReport";
    public static final String INDIVIDUAL_PATIENT_REPORT = "IndividualPatientReport";

    public static enum REPORT_CATEGORY {
        ORGANIZATION,
        PATIENT
    }
}
