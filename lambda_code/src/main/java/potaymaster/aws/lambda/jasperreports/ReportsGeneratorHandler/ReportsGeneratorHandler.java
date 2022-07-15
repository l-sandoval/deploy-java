package potaymaster.aws.lambda.jasperreports.ReportsGeneratorHandler;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import potaymaster.aws.lambda.jasperreports.ReportGenerator;
import potaymaster.aws.lambda.jasperreports.ReportGeneratorConfig;
import potaymaster.aws.lambda.jasperreports.ReportsLiterals;
import potaymaster.aws.lambda.jasperreports.StringLiterals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ReportsGeneratorHandler {
    LambdaLogger logger;
    ReportGeneratorConfig config;
    public String[] reportsToBeGenerated;
    public HashMap<String, HashMap<String, String[]>> xmlFiles;
    public HashMap<String, String> reportTypes;
    public String[] reportsList;
    public Date generationDate;
    public String reportPeriodDate;
    public String[] environments;

    public ReportsGeneratorHandler(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig,
                                   String[] reportsToBeGenerated, HashMap<String, HashMap<String, String[]>> xmlFiles,
                                   String[] environments) {
        this.logger = logger;
        this.config = reportGeneratorConfig;
        this.reportsToBeGenerated = reportsToBeGenerated;
        this.xmlFiles = xmlFiles;
        this.environments = environments;
        this.generationDate = new Date();
        this.setReportTypes();

        this.setReportPeriodDate();

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

            for(String environment : this.environments){
                if(validateIfXmlFilesListIsProvided(report, environment)){
                    String[] xmlFiles = this.xmlFiles.get(environment).get(report);
                    generateReport(xmlFiles, report, generateOutputFolder(environment, report));
                }
            }
        }
    }

    public String generateOutputFolder(String environment, String report){
        SimpleDateFormat fullDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fullDate = fullDateFormatter.format(this.generationDate);
        return this.config.get("s3path.Output") + "/" +  this.reportPeriodDate + "/" + fullDate + "/" + environment;
    }

    public void setReportPeriodDate(){
        SimpleDateFormat reportPeriodFormatter = new SimpleDateFormat("yyyy-MM");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.generationDate);
        calendar.add(Calendar.MONTH, -1);
        String reportPeriodDateFormatted = reportPeriodFormatter.format(calendar.getTime());
        this.reportPeriodDate = reportPeriodDateFormatted;
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

    public void generateReport(String[] xmlFiles, String reportName, String outputFolder) throws Exception {
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
                    this.generationDate);
        }
    }

    private void setReportTypes() {
        this.reportTypes = new HashMap<String, String>();
        this.reportTypes.put(ReportsLiterals.CUSTOMER_BILLING_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.EMERGENCY_RECOVERY_REPORT, StringLiterals.TYPE_XLS);
        this.reportTypes.put(ReportsLiterals.COMPLIANCE_BILLING_REPORT, StringLiterals.TYPE_XLS);
    }
}