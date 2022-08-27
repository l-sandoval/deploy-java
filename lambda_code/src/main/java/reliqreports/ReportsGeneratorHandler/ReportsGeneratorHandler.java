package reliqreports.ReportsGeneratorHandler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.json.simple.JSONObject;
import reliqreports.ReportGenerator;
import reliqreports.ReportGeneratorConfig;
import reliqreports.ReportsLiterals;
import reliqreports.StringLiterals;
import java.util.HashMap;

public class ReportsGeneratorHandler {
    private LambdaLogger logger;
    private String[] reportsToBeGenerated;
    private HashMap<String, HashMap<String, String[]>> xmlFiles;
    private HashMap<String, String> reportTypes;
    private String[] reportsList;
    private String generationDate;
    private String reportPeriodDate;
    private String entityId;
    private String organizationId;
    private String[] environments;
    private JSONObject environmentsApiEndpoints;
    private Boolean shouldStageReport;

    public ReportsGeneratorHandler(LambdaLogger logger, 
                                   String[] reportsToBeGenerated, HashMap<String, HashMap<String, String[]>> xmlFiles,
                                   String[] environments, JSONObject environmentsApiEndpoints, String generationDate,
                                   String reportPeriodDate, String entityId, String organizationId,
                                   Boolean shouldStageReport) {
        this.logger = logger;
        this.reportsToBeGenerated = reportsToBeGenerated;
        this.xmlFiles = xmlFiles;
        this.environments = environments;
        this.generationDate = generationDate;
        this.reportPeriodDate = reportPeriodDate;
        this.entityId = entityId;
        this.organizationId = organizationId;
        this.shouldStageReport = shouldStageReport;
        setReportTypes();

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
        return ReportGeneratorConfig.getValue("s3path.Output") + "/" +  this.reportPeriodDate + "/" + this.generationDate + "/" + environment;
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
        try {
            ReportGenerator reportGenerator = new ReportGenerator(this.logger);
            String reportType = this.reportTypes.get(reportName);
    
            String templatesPath = reportType.equals(StringLiterals.TYPE_XLS) ?
                    ReportGeneratorConfig.getValue("s3path.Templates.Excel") :
                        ReportGeneratorConfig.getValue("s3path.Templates.PDF");     
    
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
                        this.entityId,
                        this.organizationId,
                        this.generationDate,
                        apiEndpoint,
                        this.shouldStageReport);
            }
        } catch (Exception e) {
            logger.log("Exception thrown runing report generation");
            logger.log(e.getLocalizedMessage());
            logger.log(e.getMessage());
        }
    }

    private void setReportTypes() {
        this.reportTypes = new HashMap<String, String>();
        this.reportTypes.put(ReportsLiterals.CUSTOMER_BILLING_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.EMERGENCY_RECOVERY_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.COMPLIANCE_BILLING_REPORT, StringLiterals.TYPE_XLS); 
        this.reportTypes.put(ReportsLiterals.INDIVIDUAL_RPM_READINGS_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.INDIVIDUAL_PATIENT_REPORT, StringLiterals.TYPE_PDF);
    }
}
