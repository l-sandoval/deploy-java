package reliqreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.export.*;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;

public class ReportGenerator {
    private LambdaLogger logger;
    private ReportGeneratorConfig config;

    public ReportGenerator(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig) {
        this.logger = logger;
        this.config = reportGeneratorConfig;
    }

    private long startTime = System.currentTimeMillis();    
    private Map<String, Object> parameters = new HashMap<String, Object>();    

    /**
     * Generate a report according to the type
     * @param type      : "PDF", "XLS" or "XLSX"  (one or more are allowed), if null, the type(s) are read from the XML file
     * @param reportName: name of the report e.g. "ComplianceBillingReport"
     * @param xmlFile   : XML file with the report parameters
     * @param jasperPath: path for the templates
     * @param buildPath : destination path for the output reports
     * @param apiEndpoint  : current environment api endpoint
     */
    public void generateReport(
            String type,
            String reportName,
            String xmlFile,
            String jasperPath,
            String buildPath,
            String apiEndpoint
            ) throws JRException {

        HelperFunctions helper = new HelperFunctions(this.logger);

        boolean shouldStageReport = helper.shouldStageReport(reportName);

        String jasperSource = jasperPath + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + reportName + ".jrxml";

        logger.log("GenerateReport: " + reportName + "t=" + (System.currentTimeMillis() - startTime));

        parameters = new HashMap<String, Object>();
        ArrayList<String> sheetNameList = new ArrayList<String>();
        ArrayList<String> fileNameList = new ArrayList<String>();
        
        retrieveFileFromS3(xmlFile, StringLiterals.XML, StringLiterals.FILES_BUCKET);

        File dataSource = new File(StringLiterals.TMP_XML);
        if (dataSource.canRead()) {
            logger.log("Report... : Fill from : " + xmlFile);
            Document document = JRXmlUtils.parse(JRLoader.getLocationInputStream(dataSource.getPath()));

            parameters.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
            parameters.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
            parameters.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
            parameters.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
            parameters.put(JRParameter.REPORT_LOCALE, Locale.US);

            Node topNode = document.getChildNodes().item(0);
            if (helper.extractXml(topNode, null, parameters)) {
                logger.log("Report... : Map size=" + parameters.size());
            }

            // get the sheet names & source files from the parameters
            Set<String> keys = parameters.keySet();
            Map<String, Object> otherParams = new HashMap<String, Object>();
            
            for (String key : keys) {
                if (key.toLowerCase().contains(StringLiterals.SUBREPORT)) {
                    logger.log("Report... : key=" + key);
                    // get the key and filename
                    String tab = key.split(StringLiterals.FILENAME_FIELD_SEPARATOR)[2];
                    logger.log("Report... : key=" + tab);
                    sheetNameList.add(tab);
                    String subFileName = parameters.get(key).toString();
                    fileNameList.add(subFileName);
                }
                
                if (key.contains(StringLiterals.PARAMETER_FILES)) {
                    logger.log("Report... : key=" + key);
                    String parameterFileName = parameters.get(key).toString();
                    retrieveFileFromS3(parameterFileName, StringLiterals.CSV, StringLiterals.FILES_BUCKET);
                    Map<String, String> p = getParametersFromCsvFile(StringLiterals.TMP_CSV);
                    otherParams.putAll(p);
                }
            }
            
            logger.log("Other Params: " + otherParams);
            parameters.putAll(otherParams);            
            parameters.put(StringLiterals.IUGOLOGO, StringLiterals.TMP_IMAGE);
            parameters.put(StringLiterals.PAGE_COUNT, Integer.toString(fileNameList.size()));        

            retrieveFileFromS3(jasperSource, StringLiterals.TEMPLATE, StringLiterals.LAMBDA_BUCKET);

            JasperReport jasperDesign = JasperCompileManager.compileReport(StringLiterals.TMP_TEMPLATE);
            JasperPrint jpMaster = JasperFillManager.fillReport(jasperDesign, parameters, (JRDataSource) null);
            logger.log("Report... : Fill from : " + jasperSource);
            logger.log("Filling time : " + (System.currentTimeMillis() - startTime));

            // convert sheetNames list to array, add the home item
            String[] sheetNames = new String[sheetNameList.size() + 1];
            sheetNames[0] = StringLiterals.HOME_NAME;

            createSheetsFromCSVData(sheetNameList, sheetNames, jasperPath, fileNameList, jpMaster);

            // find the file type required
            if (type == null && parameters.containsKey(StringLiterals.FILE_TYPE)) {
                type = parameters.get(StringLiterals.FILE_TYPE).toString().toLowerCase();   
            }

            byte[] fileByteArray = generateReportFile(type, jpMaster, sheetNames);
            
            String fileName = buildPath + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES
                    + xmlFile.substring(xmlFile.lastIndexOf("/") + 1, xmlFile.lastIndexOf(".")) + "." + type;

            uploadFileToS3( fileName, fileByteArray);

            logger.log("Export " + type + " :" + buildPath + ", creation time : " + (System.currentTimeMillis() - startTime));

            if(shouldStageReport){
                ReportsLiterals.REPORT_CATEGORY reportCategory = helper.getReportCategory(reportName);
                String entityId = helper.extractEntityId(fileNameList.get(0));
                stageRecord(fileName, apiEndpoint, reportCategory, entityId);
            }
        }
    }

    /**
     * Generate the report file according to the file type
     * @throws JRException 
     */
    private byte[] generateReportFile (String type, JasperPrint jpMaster, String[] sheetNames) throws JRException{
        File destFile = null;
        // PDF
        if (type != null && type.contains(StringLiterals.TYPE_PDF)) {
            destFile = new File(StringLiterals.TMP_OUT_FILE_PDF);
            JRPdfExporter exporter = new JRPdfExporter();

            // export the final doc
            exporter.setExporterInput(new SimpleExporterInput(jpMaster));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

            // set the configuration for PDF
            SimplePdfReportConfiguration configuration = new SimplePdfReportConfiguration();
            configuration.setSizePageToContent(false);
            configuration.setForceLineBreakPolicy(true);
            exporter.setConfiguration(configuration);

            exporter.exportReport();
        }

        // XLSX or XLS
        if (type != null && type.contains(StringLiterals.TYPE_XLSX)) {
            destFile = new File(StringLiterals.TMP_OUT_FILE_XLS);
            JRXlsxExporter exporter = new JRXlsxExporter();

            // export the final doc
            exporter.setExporterInput(new SimpleExporterInput(jpMaster));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

            // set the configuration for XLSX
            SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
            configuration.setOnePagePerSheet(true);
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(false);
            configuration.setSheetNames(sheetNames);
            exporter.setConfiguration(configuration);

            exporter.exportReport();    
        } // XLS
        else if (type != null && type.contains(StringLiterals.TYPE_XLS)) {
            destFile = new File(StringLiterals.TMP_OUT_FILE_XLS);
            JRXlsExporter exporter = new JRXlsExporter();

            // export the final doc
            exporter.setExporterInput(new SimpleExporterInput(jpMaster));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

            // set the configuration for XLS
            SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
            configuration.setOnePagePerSheet(true);
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(false);
            configuration.setSheetNames(sheetNames);
            exporter.setConfiguration(configuration);

            exporter.exportReport();    
        }       

        byte[] fileByteArray = null;
        try {
            fileByteArray = FileUtils.readFileToByteArray(destFile);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }       

        return fileByteArray;
    }

    /**
     * Retrieve file from S3 bucket
     */
    private void retrieveFileFromS3 (String key_name, String file_type, String bucketType) {
        AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);
        try {
            s3Consumer.retrieveFileFromS3(key_name, file_type, bucketType);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }
    
    /**
     * Upload file to S3 bucket
     */
    private void uploadFileToS3 (String key_name, byte[] bytes){
        AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);
        try {
            s3Consumer.uploadFileToS3(key_name, bytes);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }

    public void stageRecord(String filePath, String apiEndpoint, ReportsLiterals.REPORT_CATEGORY reportCategory, String entityId) {
        AmazonDynamoDBConsumer dynamoDBConsumer = new AmazonDynamoDBConsumer(this.logger, this.config);
        try {
            dynamoDBConsumer.stageRecord(filePath, apiEndpoint, reportCategory, entityId);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }

    /**
     * Create each sheet from the associated CSV data file
     * @throws JRException 
     */
    private void createSheetsFromCSVData (ArrayList<String> sheetNameList, String[] sheetNames, String jasperPath, ArrayList<String> fileNameList, JasperPrint jpMaster) throws JRException{
        for (int i = 0; i < sheetNameList.size(); i++) {
            String sheetName = sheetNameList.get(i);

            // copy to the array
            sheetNames[i + 1] = sheetName;

            // set the page number in the report
            parameters.put(StringLiterals.PAGE_NUMBER, Integer.toString(i + 1));            

            retrieveFileFromS3(
                    jasperPath + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + sheetNameList.get(i) + ".jrxml",
                    StringLiterals.TEMPLATE,
                    StringLiterals.LAMBDA_BUCKET);

            retrieveFileFromS3(
                    fileNameList.get(i),
                    StringLiterals.CSV,
                    StringLiterals.FILES_BUCKET);

            logger.log("Retrieve from S3 template= " + sheetNameList.get(i) + ".jrxml" + " csv= " + fileNameList.get(i) + "\r\n");

            File sourceFile = new File(StringLiterals.TMP_TEMPLATE);
            File dataFile = new File(StringLiterals.TMP_CSV);               

            if (sourceFile.canRead() && dataFile.canRead() ) {
                logger.log("Fill...t=" + (System.currentTimeMillis() - startTime));
                JRCsvDataSource source = new JRCsvDataSource(JRLoader.getInputStream(dataFile));
                source.setRecordDelimiter("\r\n");
                source.setUseFirstRowAsHeader(true);
                logger.log("Datasource loaded...");

                JasperReport jasperDesign = JasperCompileManager.compileReport(sourceFile.getPath());
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, parameters, source);

                // add all the pages into the master doc
                List<JRPrintPage> pp = jasperPrint.getPages();
                jpMaster.setName(sheetName);
                for (int j = 0; j < pp.size(); j++) {
                    logger.log("fill... : Add page : " + sheetName + "|" + j + 1);
                    jpMaster.addPage(pp.get(j));
                }
            } else {
                logger.log("Fill...Error - cannot load files\r\n");
            }
        }
    }
    
    private Map<String, String> getParametersFromCsvFile(String filePath) throws JRException {
        Map<String, String> csvParameters = new HashMap<String, String>();
        File dataFile = new File(filePath);
        logger.log("Extract csv parameters : file=" + filePath);
        
        if (dataFile.canRead()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(JRLoader.getInputStream(dataFile)));

            try {
                //Read header line
                if (reader.ready()) 
                    reader.readLine();
                
                // Read parameters from csv files
                while (reader.ready()) {
                    String line = reader.readLine();
                    
                    if (!line.isEmpty()) {
                        String[] fields = line.split(StringLiterals.CSV_FIELD_SEPARATOR);

                        if(fields.length > 0) {
                            String key = fields[0].trim();
                            String value = fields.length > 1 ? fields[1].trim() : "";
                            csvParameters.put(key, value);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(ex.getMessage());
            }
        }
        logger.log("CSV PARAMETERS: " + csvParameters);
        return csvParameters;
    }
}