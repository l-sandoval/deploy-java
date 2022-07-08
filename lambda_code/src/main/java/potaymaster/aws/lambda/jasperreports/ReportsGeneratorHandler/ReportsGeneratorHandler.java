package potaymaster.aws.lambda.jasperreports.ReportsGeneratorHandler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import potaymaster.aws.lambda.jasperreports.ReportGenerator;
import potaymaster.aws.lambda.jasperreports.ReportGeneratorConfig;
import potaymaster.aws.lambda.jasperreports.ReportsLiterals;
import potaymaster.aws.lambda.jasperreports.StringLiterals;

import java.util.HashMap;

public class ReportsGeneratorHandler {
    LambdaLogger logger;
    ReportGeneratorConfig config;
    public String[] reportsToBeGenerated;
    public HashMap<String, String[]> xmlFiles;
    public HashMap<String, String> reportTypes;
    public String[] reportsList;

    public ReportsGeneratorHandler(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig,
                                   String[] reportsToBeGenerated, HashMap<String, String[]> xmlFiles) {
        this.logger = logger;
        this.config = reportGeneratorConfig;
        this.reportsToBeGenerated = reportsToBeGenerated;
        this.xmlFiles = xmlFiles;
        this.setReportTypes();

        this.reportsList = new String[]{
                ReportsLiterals.CUSTOMER_BILLING_REPORT,
                ReportsLiterals.EMERGENCY_RECOVERY_REPORT,
                ReportsLiterals.COMPLIANCE_BILLING_REPORT
        };
    }

    public void generateReports() throws Exception {

        String[] reports = this.reportsToBeGenerated;

        if(this.reportsToBeGenerated.length == 0)
            reports = this.reportsList;

        for (String report : reports) {
            validateIfReportIsSupported(report);

            if(validateIfXmlFilesListIsProvided(report))
                generateReport(this.xmlFiles.get(report), report);
        }
    }

    public void validateIfReportIsSupported(String report) {
        if(!this.reportTypes.containsKey(report)) {
            throw new IllegalArgumentException("Report type " + report + " is not supported");
        }
    }

    public boolean validateIfXmlFilesListIsProvided(String report) {
        if(!this.xmlFiles.containsKey(report)) {
            this.logger.log("XML files list for report " + report + " was not provided");
            return false;
        }
        return true;
    }

    public void generateReport(String[] xmlFiles, String reportName) throws Exception {
        ReportGenerator reportGenerator = new ReportGenerator(this.logger, this.config);

        String reportType = this.reportTypes.get(reportName);

        String templatesPath = reportType.equals(StringLiterals.TYPE_XLS) ?
                this.config.get("s3path.Templates.Excel") :
                this.config.get("s3path.Templates.PDF");


        if(xmlFiles.length == 0)
            logger.log("No XML files provided for report " + reportName);

        for (String xmlFile : xmlFiles) {
            logger.log("Found XML to retrieve parameters for " + reportName + ": " + xmlFile);
            reportGenerator.generateReport(
                    reportType,
                    reportName,
                    "xml/"+xmlFile,
                    templatesPath,
                    this.config.get("s3path.CSV"),
                    this.config.get("s3path.Output"));
        }
    }

    private void setReportTypes() {
        this.reportTypes = new HashMap();
        this.reportTypes.put(ReportsLiterals.CUSTOMER_BILLING_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.EMERGENCY_RECOVERY_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.COMPLIANCE_BILLING_REPORT, StringLiterals.TYPE_XLS);
    }
}
