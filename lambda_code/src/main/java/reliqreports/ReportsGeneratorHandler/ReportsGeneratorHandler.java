package reliqreports.ReportsGeneratorHandler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONObject;
import reliqreports.ReportGenerator;
import reliqreports.ReportGeneratorConfig;
import reliqreports.ReportsLiterals;
import reliqreports.StringLiterals;

import java.util.HashMap;

public class ReportsGeneratorHandler {
    LambdaLogger logger;
    ReportGeneratorConfig config;
    public String[] reportsToBeGenerated;
    public HashMap<String, HashMap<String, String[]>> xmlFiles;
    public HashMap<String, String> reportTypes;
    public String[] reportsList;
    public String generationDate;
    public String reportPeriodDate;
    public String entityId;
    public String[] environments;
    public JSONObject environmentsApiEndpoints;

    public ReportsGeneratorHandler(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig,
                                   String[] reportsToBeGenerated, HashMap<String, HashMap<String, String[]>> xmlFiles,
                                   String[] environments, JSONObject environmentsApiEndpoints,
                                   String generationDate, String reportPeriodDate, String entityId) {
        this.logger = logger;
        this.config = reportGeneratorConfig;
        this.reportsToBeGenerated = reportsToBeGenerated;
        this.xmlFiles = xmlFiles;
        this.environments = environments;
        this.generationDate = generationDate;
        this.reportPeriodDate = reportPeriodDate;
        this.entityId = entityId;
        this.setReportTypes();

        this.environmentsApiEndpoints = environmentsApiEndpoints;

        this.reportsList = new String[]{
                ReportsLiterals.CUSTOMER_BILLING_REPORT,
                ReportsLiterals.EMERGENCY_RECOVERY_REPORT,
                ReportsLiterals.COMPLIANCE_BILLING_REPORT,
                ReportsLiterals.INDIVIDUAL_RPM_READINGS_REPORT
        };
    }

    public void generateReports() throws Exception {
        String[] reports = this.reportsToBeGenerated;

        if(this.reportsToBeGenerated.length == 0)
            reports = this.reportsList;

        for (String report : reports) {
            validateIfReportIsSupported(report);

            for(String environment : this.environments){
                if(validateIfXmlFilesListIsProvided(report, environment)){
                    this.logger.log("Generating report: " + report + " for environment: " + environment);
                    String apiEndpoint = (String) this.environmentsApiEndpoints.get(environment);
                    String[] xmlFiles = this.xmlFiles.get(environment).get(report);
                    generateReport(xmlFiles, report, generateOutputFolder(environment), apiEndpoint);
                }
            }
        }
    }

    public String generateOutputFolder(String environment){
    	String pathSufixEntityId = (this.entityId != "") ? "/" + this.entityId : ""; 
        return this.config.get("s3path.Output") + "/" +  this.reportPeriodDate + "/" + this.generationDate + "/" + environment + pathSufixEntityId;
    }

    public void validateIfReportIsSupported(String report) {
        if(!this.reportTypes.containsKey(report)) {
            throw new IllegalArgumentException("Report type " + report + " is not supported");
        }
    }

    public boolean validateIfXmlFilesListIsProvided(String report, String environment) {
        boolean isReportFilesListProvided = this.xmlFiles.get(environment).containsKey(report);
        if(!isReportFilesListProvided) {
            this.logger.log("XML files list for report " + report + " was not provided");
            return false;
        }
        return true;
    }

    public void generateReport(String[] xmlFiles, String reportName, String outputFolder, String apiEndpoint) throws Exception {
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
                    xmlFile,
                    templatesPath,
                    outputFolder,
                    apiEndpoint);
        }
    }

    private void setReportTypes() {
        this.reportTypes = new HashMap<String, String>();
        this.reportTypes.put(ReportsLiterals.CUSTOMER_BILLING_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.EMERGENCY_RECOVERY_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.COMPLIANCE_BILLING_REPORT, StringLiterals.TYPE_XLS); 
        this.reportTypes.put(ReportsLiterals.INDIVIDUAL_RPM_READINGS_REPORT, StringLiterals.TYPE_XLS);
    }
}
