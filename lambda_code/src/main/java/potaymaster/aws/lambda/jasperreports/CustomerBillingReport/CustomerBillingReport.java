package potaymaster.aws.lambda.jasperreports.CustomerBillingReport;

import java.io.IOException;
import java.time.Instant;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import net.sf.jasperreports.engine.JRException;
import potaymaster.aws.lambda.jasperreports.AmazonS3Consumer;
import potaymaster.aws.lambda.jasperreports.ReportGenerator;
import potaymaster.aws.lambda.jasperreports.ReportGeneratorConfig;
import potaymaster.aws.lambda.jasperreports.StringLiterals;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class CustomerBillingReport {


    LambdaLogger logger;
    ReportGeneratorConfig config;
    public String reportName;
    public String typeReportXls;
    public String typeReportPdf;
    public String xmlSuffix;
    public CustomerBillingReport(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig){
        this.logger = logger;
        this.config = reportGeneratorConfig;
        this.reportName = CustomerBillingReportLiterals.BILLING_SUMMARY_REPORT;
        this.typeReportXls = StringLiterals.TYPE_XLS;
        this.typeReportPdf = StringLiterals.TYPE_PDF;
        this.xmlSuffix = CustomerBillingReportLiterals.XML_SUFFIX;
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
                logger.log("\ngot here and found the xml folder and files: "+file.key()+" - "+xmlSuffix+"\n");
                logger.log("\ngenerationDate: "+generationDate+"\n");
                if (file.key().contains(generationDate) && file.key().toLowerCase().contains(xmlSuffix)) {
                    String reportXmlFile = file.key();
                    logger.log("Found XML to retrieve parameters for " + reportName + ": " + reportXmlFile);
                    reportGenerator.generateReport(
                            typeReportXls,
                            reportName,
                            reportXmlFile,
                            this.config.get("s3path.Templates"),
                            this.config.get("s3path.CSV"),
                            this.config.get("s3path.Output"));

                    reportGenerator.generateReport(
                            typeReportPdf,
                            reportName,
                            reportXmlFile,
                            this.config.get("s3path.Templates.pdf"),
                            this.config.get("s3path.CSV"),
                            this.config.get("s3path.Output"));
                }
            }
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }
}
