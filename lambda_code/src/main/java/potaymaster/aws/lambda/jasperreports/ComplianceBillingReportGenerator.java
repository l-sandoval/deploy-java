package potaymaster.aws.lambda.jasperreports;

import net.sf.jasperreports.engine.JRException;

public class ComplianceBillingReportGenerator {
	
	//begin TODO: Move to config file IPM-7990
	static final String COMPLIANCE_BILLING_CSV_PATH = "compliance-billing/csv/";
	static final String COMPLIANCE_BILLING_PDF_TEMPLATES_PATH = "compliance-billing/pdf-templates/";
	//end TODO: Move to config file IPM-7990

	public ComplianceBillingReportGenerator() {
	}

	public SourceFiles[] retrieveSourceFilesToGenerateReport() throws JRException {
		//TODO: Retrieve correctly the corresponding csv and templates files list
		SourceFiles[] csv_list = new SourceFiles[]{
			new SourceFiles (COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_12345678_billing_provider.csv", COMPLIANCE_BILLING_PDF_TEMPLATES_PATH + "BillingProvider.jrxml"),
			new SourceFiles (COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_12345678_devices.csv", COMPLIANCE_BILLING_PDF_TEMPLATES_PATH + "Devices.jrxml"),
			new SourceFiles (COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_blood_glucose_adherence.csv", COMPLIANCE_BILLING_PDF_TEMPLATES_PATH + "BloodGlucoseAdherence.jrxml"),
			new SourceFiles (COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_12345678_blood_pressure_adherence.csv", COMPLIANCE_BILLING_PDF_TEMPLATES_PATH + "BloodPressureAdherence.jrxml"),
			//new SourceFiles (COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_12345678_patient_summary.csv", COMPLIANCE_BILLING_PDF_TEMPLATES_PATH + "PatientSummary.jrxml"),
			/*COMPLIANCE_BILLING_CSV_PATH + "IUGOReport_2022-01-31_12345678_billing_provider.csv"*/
		};
		
		return csv_list;
	}
	
  /*  private final String REPORT_NAME = "ComplianceBillingReport";
    private final String JASPER_PATH_XLS = "build" + File.separator + "reports" + File.separator + "ComplianceBilling" + File.separator + "Excel" + File.separator + "templates";
    private final String JASPER_PATH_PDF = "build" + File.separator + "reports" + File.separator + "ComplianceBilling" + File.separator + "PDF" + File.separator + "templates";
    private final String BUILD_PATH = "build" + File.separator + "reports";
    private final String DATA_PATH = "data";
    private final String XML_SUFFIX = "monthly_compliance_billing.xml";

    private ReportGenerator reportGenerator = new ReportGenerator();

    public void generateReport(String type) throws JRException {

        // search for the xml file in the data directory
        File folder = new File(DATA_PATH);
        for (File file : folder.listFiles()) {
            // find the correct xml file
            if (file.getName().toLowerCase().contains(XML_SUFFIX)) {
                String reportXmlFile = file.getAbsolutePath();
                System.err.println("generateReport xml found: " + file.getName());

                reportGenerator.generateReport(type,
                        REPORT_NAME,
                        reportXmlFile,
                        type == IUGOReportsApp.TYPE_PDF ? JASPER_PATH_PDF : JASPER_PATH_XLS,
                        DATA_PATH,
                        BUILD_PATH);
                break;
            }
        }
    }*/
}
