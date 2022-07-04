package potaymaster.aws.lambda.jasperreports.EmergencyRecoveryReport;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import net.sf.jasperreports.engine.JRException;
import potaymaster.aws.lambda.jasperreports.AmazonS3Consumer;
import potaymaster.aws.lambda.jasperreports.ReportGenerator;
import potaymaster.aws.lambda.jasperreports.ReportGeneratorConfig;
import potaymaster.aws.lambda.jasperreports.StringLiterals;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.time.Instant;

public class EmergencyRecoveryReport {
    LambdaLogger logger;
    ReportGeneratorConfig config;
    public String reportName;
    public String typeReport;
    public String xmlSuffix;

    public EmergencyRecoveryReport(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig){
        this.logger = logger;
        this.config = reportGeneratorConfig;
        this.reportName = EmergencyRecoveryReportLiterals.EMERGENCY_RECOVERY_REPORT;
        this.typeReport = StringLiterals.TYPE_XLS;
        this.xmlSuffix = EmergencyRecoveryReportLiterals.XML_SUFFIX;
    }

    public void generateReport() throws JRException {
        ReportGenerator reportGenerator = new ReportGenerator(this.logger, this.config);
        AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);

        Instant now = Instant.now();
        String generationDate = now.toString().substring(0, 10);

        try {
            ListObjectsResponse objects = s3Consumer.listObjectsFromS3Folder(this.config.get("s3path.XML"));

            // search in the S3 folder the corresponding XML file to the current date of report generation
            for (S3Object file : objects.contents()) {
                if (file.key().contains(generationDate) && file.key().toLowerCase().contains(xmlSuffix)) {
                    String reportXmlFile = file.key();
                    logger.log("Found XML to retrieve parameters for " + reportName + ": " + reportXmlFile);
                    reportGenerator.generateReport(
                            typeReport,
                            reportName,
                            reportXmlFile,
                            this.config.get("s3path.Templates.Excel"),
                            this.config.get("s3path.CSV"),
                            this.config.get("s3path.Output"));
                }
            }
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }
}
